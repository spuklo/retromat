package io.github.spuklo.retromat

import com.lowagie.text.Chunk
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.FontFactory
import com.lowagie.text.HeaderFooter
import com.lowagie.text.ListItem
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfName
import com.lowagie.text.pdf.PdfString
import com.lowagie.text.pdf.PdfWriter
import io.github.spuklo.retromat.CardType.ACTION
import io.github.spuklo.retromat.CardType.APPRECIATION
import io.github.spuklo.retromat.CardType.IDEA
import io.github.spuklo.retromat.CardType.NEGATIVE
import io.github.spuklo.retromat.CardType.OTHER
import io.github.spuklo.retromat.CardType.POSITIVE
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

private val footerFont = FontFactory.getFont("/fonts/Roboto-Regular.ttf", 8f)
private val listFont = FontFactory.getFont("/fonts/Roboto-Regular.ttf", 11f)
private val headerFont = FontFactory.getFont("/fonts/Roboto-Regular.ttf", 14f)

private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
private fun timeOf(millis: Long = Instant.now().toEpochMilli()): String = dateTimeFormat.format(Date(millis))

fun generateRetroNotes(retro: Retro): ByteArray = try {
    val cardTypesOrder = listOf(APPRECIATION, POSITIVE, NEGATIVE, IDEA, OTHER, ACTION)
    val outputStream = ByteArrayOutputStream()
    val retroNotes = Document(PageSize.A4, 40f, 40f, 40f, 40f)
    val writer = PdfWriter.getInstance(retroNotes, outputStream)
    writer.info.put(PdfName.CREATOR, PdfString("Retromat $version using ${Document.getVersion()}"));
    val headerFooter =
        HeaderFooter(Phrase("Retromat v. $version, generated ${timeOf()}, page ", footerFont), true)
    headerFooter.setAlignment(Element.ALIGN_CENTER)
    retroNotes.setFooter(headerFooter)

    retroNotes.open()

    val heading = Paragraph(
        Chunk("Retro notes - ${timeOf(retro.created.toEpochSecond(ZoneOffset.UTC))}", headerFont)
    )
    heading.alignment = Element.ALIGN_CENTER
    retroNotes.add(heading)
    retroNotes.add(Chunk.NEWLINE)
    retroNotes.add(Chunk.NEWLINE)

    cardTypesOrder.forEach { cardType ->
        val cards = retro.cards.filter { card -> card.type == cardType }.toList()
        when {
            cards.isNotEmpty() -> {
                val sectionHeading = Paragraph(Chunk(cardType.name, headerFont))
                sectionHeading.alignment = Element.ALIGN_CENTER
                val cardsList = com.lowagie.text.List()
                cardsList.setListSymbol("")
                cards.map {
                    ListItem("${it.text} (votes: ${it.votes})", listFont)
                }.forEach {
                    cardsList.add(it)
                }
                sectionHeading.add(cardsList)
                retroNotes.add(sectionHeading)
                retroNotes.add(Chunk.NEWLINE)
            }
        }
    }

    retroNotes.close()
    outputStream.toByteArray()
} catch (e: Exception) {
    println(e.message)
    ByteArray(0)
}