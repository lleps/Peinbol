package io.snower.game.client

import javax.vecmath.Vector3f

class AudioSource(
    var position: Vector3f,
    var volume: Float,
    val audioId: Int,
    var ratio: Float,
    var loop: Boolean,
    var pitch: Float = 1f,
    val millis: Int = 0 // how many millis will last
) {
    // implementation-specific. To be used by AudioManager manager.
    var sourceId: Int = 0
    var startMillis = 0L
}