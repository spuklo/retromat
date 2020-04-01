package io.github.spuklo.retromat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.javalin.Javalin
import io.javalin.plugin.json.JavalinJackson
import io.javalin.plugin.json.JavalinJson
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import io.github.spuklo.retromat.CardType.APPRECIATION
import io.github.spuklo.retromat.CardType.IDEA
import io.github.spuklo.retromat.CardType.OTHER
import io.github.spuklo.retromat.MessageType.CARD
import io.github.spuklo.retromat.MessageType.ERROR
import io.github.spuklo.retromat.MessageType.SAFETY_LEVEL
import io.github.spuklo.retromat.MessageType.STATS
import io.github.spuklo.retromat.MessageType.VOTE
import org.eclipse.jetty.websocket.api.Session
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import kotlin.concurrent.fixedRateTimer

object Retromat {

    private const val port = 8765
    private const val wsPingInterval = 15_000L

    private val log = LoggerFactory.getLogger("RetromatLogger")
    private val mapper = objectMapper()
    private val retro = AtomicReference(newRetro())
    private val sessions = mutableSetOf<Session>()
    private val safetyLevels = mutableMapOf<String, Int>()
    private val adminCode = random6digits()

    @JvmStatic
    fun main(args: Array<String>) {
        JavalinJackson.configure(mapper)
        Javalin.create {
            it.addStaticFiles("/public")
        }
            .post("/retro") { ctx ->
                val code = ctx.formParam("code", "0")!!.toInt()
                when (adminCode) {
                    code -> {
                        val newRetro = newRetro()
                        val oldRetro = retro.getAndSet(newRetro)
                        log.info(
                            "New retro created in place of the old one. Old retro data:\n{}",
                            JavalinJson.toJson(oldRetro)
                        )
                        log.info("New retro id: {}", newRetro.id)
                        safetyLevels.clear()
                        sessions.forEach {
                            it.remote.sendString(
                                JavalinJson.toJson(retro.get().toMessage())
                            )
                        }
                        sendStats()
                        ctx.json(newRetro)
                    }
                    else -> {
                        ctx.status(SC_BAD_REQUEST)
                        ctx.result("Invalid code. Better luck next time")
                    }
                }
            }
            .get("/retro") { ctx ->
                ctx.json(retro.get())
            }
            .ws("/retro") {
                it.onConnect { ctx ->
                    log.info("Session connected {}", ctx.sessionId)
                    sessions.add(ctx.session)
                    sendStats()
                    ctx.send(retro.get().toMessage())
                }
                it.onMessage { ctx ->
                    val parsedMessage =
                        parseMessageOrErrorMessage(ctx)
                    when (parsedMessage.type) {
                        ERROR -> {
                            ctx.send(JavalinJson.toJson(parsedMessage))
                            log.error(
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
                            log.error("Unexpected message received on session {}: {}", ctx.sessionId, ctx.message())
                        }
                    }
                }
                it.onClose { ctx ->
                    clear(ctx)
                    sendStats()
                    log.info("Session disconnected {}", ctx.sessionId)
                }
                it.onError { ctx ->
                    clear(ctx)
                    sendStats()
                    log.error("Error on session {}", ctx.sessionId)
                }
            }
            .start(port)
        log.info(banner)
        log.info("Retromat is listening on port: {}",
            port
        )
        log.info("Admin code: {}",
            adminCode
        )
        log.info("Created retro id: {}", retro.get().id)

        val wsPingTimer = fixedRateTimer("websocket-ping-timer", false,
            wsPingInterval,
            wsPingInterval
        ) {
            sessions.forEach { it.remote.sendString(
                pingMessageJson
            ) }
        }

        Runtime.getRuntime().addShutdownHook(Thread(Runnable {
            wsPingTimer.cancel()
            log.info("Retromat is shutting down. Last known retro data: \n{}", JavalinJson.toJson(
                retro.get()))
        }))
    }

    private fun clear(ctx: WsContext) {
        sessions.remove(ctx.session)
        safetyLevels.remove(ctx.sessionId)
    }

    private fun parseMessageOrErrorMessage(ctx: WsMessageContext) = try {
        ctx.message(Message::class.java)
    } catch (e: Exception) {
        Message(
            ERROR,
            mapOf("message" to "Malformed JSON message: \"${ctx.message()}\"")
        )
    }

    private fun sendStats() {
        val statsMessage = Message(
            STATS, mapOf(
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

                val (type: CardType, text: String) =
                    when (val sentType = CardType.valueOf(body.getValue("type").toString())) {
                        IDEA -> Pair(OTHER, "[IDEA] ${body.getValue("text")}")
                        APPRECIATION -> Pair(OTHER, "[APPRECIATION] ${body.getValue("text")}")
                        else -> Pair(sentType, body.getValue("text").toString())
                    }

                val newCard = RetroCard(System.nanoTime(), type, text)
                retro.set(retro.get().addNewCard(newCard))
                sendUpdatedCard(newCard)
            }
        }
    }

    private fun handleVoteMessage(voteMessage: Message) {
        log.info("VOTE message: {}", voteMessage)
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
        log.debug("Sending updated card: {}", card)
        val newCardMessage = JavalinJson.toJson(card.toMessage())
        sessions.forEach { it.remote.sendString(newCardMessage) }
    }

    private fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(
                SimpleModule()
                    .addSerializer(
                        LocalDateTime::class.java,
                        LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"))
                    )
            )
            .enable(SerializationFeature.INDENT_OUTPUT)
    }

    private val pingMessageJson = JavalinJson.toJson(
        Message(
            MessageType.PING,
            mapOf()
        )
    )

    private const val banner = """
             
    _____  ______ _______ _____   ____  __  __       _______ 
   |  __ \|  ____|__   __|  __ \ / __ \|  \/  |   /\|__   __|
   | |__) | |__     | |  | |__) | |  | | \  / |  /  \  | |   
   |  _  /|  __|    | |  |  _  /| |  | | |\/| | / /\ \ | |   
   | | \ \| |____   | |  | | \ \| |__| | |  | |/ ____ \| |   
   |_|  \_\______|  |_|  |_|  \_\\____/|_|  |_/_/    \_\_|   

"""
}
