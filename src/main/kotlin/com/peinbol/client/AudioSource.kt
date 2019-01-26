package com.peinbol.client

import org.joml.Vector3f

class AudioSource(
    var position: Vector3f,
    val volume: Float,
    val audioId: Int,
    val radio: Float
) {
    // implementation-specific. To be used by AudioManager manager.
    var sourceId: Int = 0
}