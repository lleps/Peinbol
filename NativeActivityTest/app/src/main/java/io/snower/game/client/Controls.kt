package io.snower.game.client

/** Defines player controls, up to the implementation how to interpret them (like GLFW, or android touches)*/
interface Controls {
    // TODO: must be done generically (i.e an angle/vector instead of only 4 components)
    fun checkForward(): Boolean
    fun checkBackwards(): Boolean
    fun checkRight(): Boolean
    fun checkLeft(): Boolean
    fun checkFire(): Boolean

    fun getCameraRotationX(): Float
    fun getCameraRotationY(): Float

    /** Called when controls have been read in the frame, to prepare for the next. */
    fun readDone()
}