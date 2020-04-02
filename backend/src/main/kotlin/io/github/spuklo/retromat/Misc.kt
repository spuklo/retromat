package io.github.spuklo.retromat

import kotlin.random.Random

fun random6digits(): Int = Random(System.nanoTime()).nextInt(100000, 1000000)

fun Float.twoDecimals() : Float {
    return "%.2f".format(this).toFloat()
}