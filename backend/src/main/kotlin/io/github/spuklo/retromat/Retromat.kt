package io.github.spuklo.retromat

import io.github.spuklo.retromat.MessageType.CARD
import io.github.spuklo.retromat.MessageType.ERROR
import io.github.spuklo.retromat.MessageType.SAFETY_LEVEL
import io.github.spuklo.retromat.MessageType.STATS
import io.github.spuklo.retromat.MessageType.VERSION
import io.github.spuklo.retromat.MessageType.VOTE
import io.javalin.Javalin
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.json.JavalinJson
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import org.eclipse.jetty.websocket.api.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import kotlin.concurrent.fixedRateTimer

object Retromat {

    private const val port = 8765
    private const val wsPingInterval = 15_000L

    val retromatLogger : Logger = LoggerFactory.getLogger("RetromatLogger")
    private val retro = AtomicReference(loadMagicFileOrCreateEmptyRetro())
    private val sessions = mutableSetOf<Session>()
    private val safetyLevels = mutableMapOf<String, Int>()

    @JvmStatic
    fun main(args: Array<String>) {
        JavalinJackson.configure(objectMapper)
        Javalin.create {
            it.addStaticFiles("/public")
        }
            .get("/retro/pdf") { ctx ->
                val retroNotesPdf = generateRetroNotes((retro.get()))
                when {
                    retroNotesPdf.isNotEmpty() -> {
                        ctx.header("Content-disposition", "attachment; filename=retro-${retro.get().id}.pdf")
                        ctx.contentType("application/pdf")
                        ctx.result(retroNotesPdf)
                    }
                    else -> {
                        ctx.status(SC_INTERNAL_SERVER_ERROR)
                        ctx.result("Failed to generate PDF with notes")
                    }
                }
            }
            .ws("/retro") {
                it.onConnect { ctx ->
                    retromatLogger.info("Session connected {}", ctx.sessionId)
                    ctx.send(Message(VERSION, mapOf("v" to version)))
                    sessions.add(ctx.session)
                    sendStats()
                    ctx.send(retro.get().toMessage())
                }
                it.onMessage { ctx ->
                    val parsedMessage = parseMessageOrErrorMessage(ctx)
                    when (parsedMessage.type) {
                        ERROR -> {
                            ctx.send(JavalinJson.toJson(parsedMessage))
                            retromatLogger.error(
                                "Session sent {} the message which we could not understand. {}",
                                ctx.sessionId,
                                ctx.message()
                            )
                        }
                        CARD -> handleCardMessage(parsedMessage)
                        VOTE -> handleVoteMessage(parsedMessage)
                        SAFETY_LEVEL -> {
                            handleSafetyLevelMessage(
                                parsedMessage,
                                ctx.sessionId
                            )
                            sendStats()
                        }
                        else -> {
                            retromatLogger.error("Unexpected message received on session {}: {}", ctx.sessionId, ctx.message())
                        }
                    }
                    saveCurrentRetro(retro.get())
                }
                it.onClose { ctx ->
                    clear(ctx)
                    sendStats()
                    retromatLogger.info("Session disconnected {}", ctx.sessionId)
                }
                it.onError { ctx ->
                    clear(ctx)
                    sendStats()
                    retromatLogger.error("Error on session {}", ctx.sessionId)
                }
            }
            .start(port)

        retromatLogger.info(banner)
        retromatLogger.info("Retromat v.$version is listening on port: {}", port)
        retromatLogger.info("Retro is saved in file: {}", currentRetroBackupFile(retro.get()))

        val wsPingTimer = fixedRateTimer("websocket-ping-timer", false, wsPingInterval, wsPingInterval) {
            sessions.forEach {
                it.remote.sendString(pingMessageJson)
            }
        }

        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            wsPingTimer.cancel()
            saveCurrentRetro(retro.get())
            retromatLogger.info("Retromat is shutting down. Last known retro data is saved in {}",
                currentRetroBackupFile(retro.get()))
        }))
    }

    private fun clear(ctx: WsContext) {
        sessions.remove(ctx.session)
        safetyLevels.remove(ctx.sessionId)
    }

    private fun parseMessageOrErrorMessage(ctx: WsMessageContext) = try {
        ctx.message(Message::class.java)
    } catch (e: Exception) {
        Message(ERROR, mapOf("message" to "Malformed JSON message: \"${ctx.message()}\""))
    }

    private fun sendStats() {
        val statsMessage = Message(STATS, mapOf(
                "sessions" to sessions.size,
                "min_safety" to if (safetyLevels.isEmpty()) 0 else safetyLevels.values.min()!!,
                "max_safety" to if (safetyLevels.isEmpty()) 0 else safetyLevels.values.max()!!,
                "avg_safety" to if (safetyLevels.isEmpty()) 0 else (safetyLevels.values.sum() / safetyLevels.size.toFloat()).twoDecimals()
            )
        )
        sessions.forEach { it.remote.sendString(JavalinJson.toJson(statsMessage)) }
    }

    private fun handleCardMessage(cardMessage: Message) {
        val body = cardMessage.body
        when {
            body.containsKey("type") && body.containsKey("text")
                    && CardType.values().contains(CardType.valueOf(body.getValue("type").toString()))
                    && body.getValue("text").toString().isNotBlank() -> {

                val type = CardType.valueOf(body.getValue("type").toString())
                val text = body.getValue("text").toString()
                val newCard = RetroCard(System.nanoTime(), type, text)
                retro.set(retro.get().addNewCard(newCard))
                sendUpdatedCard(newCard)
            }
        }
    }

    private fun handleVoteMessage(voteMessage: Message) {
        val body = voteMessage.body
        when {
            body.containsKey("id") && body.containsKey("vote") -> {
                val cardId = body.getValue("id") as Long
                val voteValue = body.getValue("vote") as Int
                val newCard = retro.get().cards.first { it.id == cardId }.withVote(voteValue)
                retro.set(retro.get().withVotedCard(newCard))
                sendUpdatedCard(newCard)
            }
        }
    }

    private fun handleSafetyLevelMessage(safetyLevelMessage: Message, sessionId: String) {
        when {
            safetyLevelMessage.body.containsKey("level")
                    && (safetyLevelMessage.body.getValue("level") as? String ?: "0").toInt() in 1..5 -> {
                safetyLevels[sessionId] = (safetyLevelMessage.body.getValue("level") as String).toInt()
            }
        }
    }

    private fun sendUpdatedCard(card: RetroCard) {
        retromatLogger.debug("Sending updated card: {}", card)
        val newCardMessage = JavalinJson.toJson(card.toMessage())
        sessions.forEach { it.remote.sendString(newCardMessage) }
    }

    private val pingMessageJson = JavalinJson.toJson(Message(MessageType.PING, mapOf()))

    private const val banner = """
             
    _____  ______ _______ _____   ____  __  __       _______ 
   |  __ \|  ____|__   __|  __ \ / __ \|  \/  |   /\|__   __|
   | |__) | |__     | |  | |__) | |  | | \  / |  /  \  | |   
   |  _  /|  __|    | |  |  _  /| |  | | |\/| | / /\ \ | |   
   | | \ \| |____   | |  | | \ \| |__| | |  | |/ ____ \| |   
   |_|  \_\______|  |_|  |_|  \_\\____/|_|  |_/_/    \_\_|   

"""
}
