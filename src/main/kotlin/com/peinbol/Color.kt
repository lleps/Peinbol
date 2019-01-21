package com.peinbol

class Color(val r: Double = 1.0, val g: Double = 1.0, val b: Double = 1.0, val a: Double = 1.0) {
    companion object {
        val RED = Color(1.0, 0.0, 0.0)
        val GREEN = Color(0.0, 1.0, 0.0)
        val WHITE = Color(1.0, 1.0, 1.0)
    }
}