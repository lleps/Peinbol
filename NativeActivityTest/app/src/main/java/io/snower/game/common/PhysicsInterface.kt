package io.snower.game.common

/** Defines all necessary functionality for the physics side. */
interface PhysicsInterface {
    /** Register this box in the physics world. */
    fun register(box: Box)

    /** Remove this box from the physics world. */
    fun unRegister(box: Box)

    /** Get model-space mat4 for the given box, to draw it. Store on [dst]. */
    fun getBoxOpenGLMatrix(box: Box, dst: FloatArray)

    /**
     * Simulate a step in the physics world. [delta] is the milliseconds to step.
     * Update registered boxes position when they change.
     * */
    fun simulate(delta: Int, updateObjs: Boolean, updateId: Int = -1)

    /** Returns how many millis took the last simulation. */
    val lastSimulationMillis: Float

    // TODO: Add collision callbacks
}