package com.peinbol

import java.util.*

fun generateId(): Int = randBetween(0, 0x7FFFFFFF)

fun randBetween(min: Int, max: Int) = min + Random().nextInt(max-min)

fun timedOscillator(millis: Int): Int {
    val time = System.currentTimeMillis() % millis
    val ascendingOrDescending = (System.currentTimeMillis() / millis) % 2
    return if (ascendingOrDescending == 0L) {
        time.toInt()
    } else {
        millis - time.toInt()
    }
}