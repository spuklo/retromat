package io.github.spuklo.retromat

import java.time.LocalDateTime

enum class MessageType {
    RETRO, CARD, VOTE, ERROR, SAFETY_LEVEL, STATS, PING
}

enum class CardType {
    POSITIVE, NEGATIVE, OTHER, ACTION, IDEA, APPRECIATION
}

data class Message(val type: MessageType, val body: Map<String, Any> = mapOf())

data class RetroCard(val id: Long, val type: CardType, val text: String, val votes: Int = 0) {
    fun toMap() = mapOf<String, Any>(
        "id" to id,
        "type" to type.name,
        "text" to text,
        "votes" to votes
    )

    fun withVote(vote: Int): RetroCard =
        RetroCard(id, type, text, votes + vote)

    fun toMessage(): Message =
        Message(
            MessageType.CARD,
            toMap()
        )
}

data class Retro(val id: Int, val created: LocalDateTime, val cards: List<RetroCard>) {
    fun addNewCard(card: RetroCard): Retro =
        Retro(
            id,
            created,
            cards.toMutableList().apply {
                add(card)
                toList()
            }
        )

    fun withVotedCard(votedCard: RetroCard): Retro =
        Retro(
            id,
            created,
            cards.filter { it.id != votedCard.id }.toMutableList().apply {
                add(votedCard)
                toList()
            }
        )

    fun toMessage(): Message =
        Message(
            MessageType.RETRO, mapOf(
                "id" to id,
                "created" to created,
                "cards" to cards.map { it.toMap() }.toList()
            )
        )
}

fun newRetro() = Retro(
    random6digits(),
    LocalDateTime.now(),
    listOf()
)
