package com.peinbol.client

import javax.vecmath.Vector3f

class AudioSource(
    var position: Vector3f,
    var volume: Float,
    val audioId: Int,
    var ratio: Float,
    var pitch: Float = 1f,
    var loop: Int = 0
) {
    // implementation-specific. To be used by AudioManager manager.
    var sourceId: Int = 0
}