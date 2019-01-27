
package com.peinbol

import org.joml.Matrix4f
import java.util.*
import javax.vecmath.Tuple3f
import javax.vecmath.Tuple4f
import javax.vecmath.Vector3f
import kotlin.math.sqrt

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

// Destructuring declarations for javax.math
operator fun Tuple3f.component1(): Float = x
operator fun Tuple3f.component2(): Float = y
operator fun Tuple3f.component3(): Float = z

operator fun Tuple4f.component1(): Float = x
operator fun Tuple4f.component2(): Float = y
operator fun Tuple4f.component3(): Float = z
operator fun Tuple4f.component4(): Float = w

// Clone generic
@Suppress("UNCHECKED_CAST")
fun <T : Tuple3f> T.get() = clone() as T
@Suppress("UNCHECKED_CAST")
fun <T : Tuple4f> T.get() = clone() as T

operator fun Vector3f.plus(other: Vector3f): Vector3f {
    val ret = this.get()
    ret.add(other)
    return ret
}

operator fun Vector3f.minus(other: Vector3f): Vector3f {
    val ret = this.get()
    ret.sub(other)
    return ret
}

operator fun Vector3f.times(t: Float): Vector3f {
    val ret = this.get()
    ret.scale(t)
    return ret
}

inline fun Vector3f.withOps(block: Vector3f.() -> Unit): Vector3f {
    val ret = this.get()
    ret.block()
    return ret
}

fun Vector3f.distance3D(vector: Vector3f): Float {
    val dx = vector.x - x
    val dy = vector.y - y
    val dz = vector.z - z
    return sqrt(dx * dx + dy * dy + dz * dz)
}

// Get front vector for the given angle
fun vectorFront(angleX: Float, angleY: Float, scale: Float = 1f): Vector3f {
    return Vector3f(
        -Math.sin(Math.toRadians(angleX.toDouble())).toFloat() * scale,
        Math.sin(Math.toRadians(angleY.toDouble())).toFloat() * scale,
        -Math.cos(Math.toRadians(angleX.toDouble())).toFloat() * scale
    )
}

fun radians(degrees: Float): Float = Math.toRadians(degrees.toDouble()).toFloat()