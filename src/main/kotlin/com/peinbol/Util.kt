package com.peinbol

import java.util.*

fun generateId(): Int = randBetween(0, 0x7FFFFFFF)

fun randBetween(min: Int, max: Int) = min + Random().nextInt(max-min)
fun <T> List<T>.randomElement() = get(randBetween(0, size))