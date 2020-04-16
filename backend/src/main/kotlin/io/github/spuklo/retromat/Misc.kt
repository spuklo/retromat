package io.github.spuklo.retromat

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import io.github.spuklo.retromat.Retromat.retromatLogger
import java.io.File
import java.time.LocalDateTime
import kotlin.random.Random

fun random6digits(): Int = Random(System.nanoTime()).nextInt(100000, 1000000)

fun Float.twoDecimals(): Float = "%.2f".format(this).toFloat()

private val versionKey = Key("version", stringType)
val version = ConfigurationProperties.fromResource("buildinfo.properties")[versionKey]

val objectMapper = objectMapper()
private fun objectMapper(): ObjectMapper {
    return ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
}

fun newRetro() = Retro(
    System.currentTimeMillis(),
    LocalDateTime.now(),
    listOf()
)

fun currentRetroBackupFile(retro: Retro) = "retro-${retro.id}.json"

private val magicFileCurrentRetro = File("current-retro.json")
fun saveCurrentRetro(retro: Retro) {
    val saveFile = currentRetroBackupFile(retro)
    try {
        File(saveFile).writeText(objectMapper.writeValueAsString(retro))
    } catch (e: Exception) {
        retromatLogger.error("Error saving current retro file $saveFile: {}", e.message)
    }
}

fun loadMagicFileOrCreateEmptyRetro(): Retro = try {
    when {
        magicFileCurrentRetro.exists() -> {
            retromatLogger.info("Found magic file, loading retro...")
            objectMapper.readValue(magicFileCurrentRetro, Retro::class.java)
        }
        else -> newRetro()
    }
} catch (e: Exception) {
    retromatLogger.error("Error reading magic file (${magicFileCurrentRetro.name}). Error was: {}", e.message)
    newRetro()
}.also { saveCurrentRetro(it) }
