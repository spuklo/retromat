package io.github.spuklo.retromat

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import kotlin.random.Random

fun random6digits(): Int = Random(System.nanoTime()).nextInt(100000, 1000000)

fun Float.twoDecimals() : Float {
    return "%.2f".format(this).toFloat()
}

private val versionKey = Key("version", stringType)
val version = ConfigurationProperties.fromResource("buildinfo.properties")[versionKey]