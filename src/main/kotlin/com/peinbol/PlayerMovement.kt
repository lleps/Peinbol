package com.peinbol

import javax.vecmath.Quat4f
import javax.vecmath.Vector3f

/** To be processed by the server, and predicted by the client. */
fun doPlayerMovement(box: Box, inputState: Messages.InputState, delta: Long) {
    val deltaSec = delta / 1000f
    val force = 50f
    val limit = if (inputState.walk && box.inGround) 3f else 8f
    box.rotation = Quat4f(0f, 0f, 0f, 1f)
    box.angularVelocity = Vector3f()
    // W,A,S,D
    if (box.linearVelocity.length() < limit) {
        var velVector = Vector3f()
        if (inputState.forward) velVector += vectorFront(inputState.cameraY, 0f, force * deltaSec)
        if (inputState.backwards) velVector -= vectorFront(inputState.cameraY, 0f, force * deltaSec)
        if (inputState.right) velVector -= vectorFront(inputState.cameraY + 90f, 0f, force * deltaSec)
        if (inputState.left) velVector -= vectorFront(inputState.cameraY - 90f, 0f, force * deltaSec)
        if (velVector.length() > 0.001) {
            box.rigidBody!!.activate()
            box.linearVelocity += velVector
        }
    }

    // Jump
    if (inputState.jump &&  box.inGround) {
        box.linearVelocity += Vector3f(0f, force * 25f * deltaSec, 0f)
    }
}