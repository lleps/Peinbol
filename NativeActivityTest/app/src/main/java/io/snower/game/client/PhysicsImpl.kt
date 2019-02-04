package io.snower.game.client

import android.util.Log

class PhysicsImpl {
    private external fun createBoxHandle(x: Float, y: Float, z: Float): Long

    init {
        val handle = createBoxHandle(1f, 1f, 1f)
        Log.i("tag", "create handle: $handle")
    }
}