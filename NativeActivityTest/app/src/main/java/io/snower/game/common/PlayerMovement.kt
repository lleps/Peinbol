package io.snower.game.common

import javax.vecmath.Quat4f
import javax.vecmath.Vector3f

/** To be processed by the server, and predicted by the client. */
fun doPlayerMovement(box: Box, inputState: Messages.InputState, delta: Int) {
    val deltaSec = delta / 1000f
    val force = 30f
    val limit = if (inputState.walk) 2f else 7f
    box.rotation = Quat4f(0f, 0f, 0f, 1f)
    box.angularVelocity = Vector3f()
    // W,A,S,D
    if (true) {//(box.linearVelocity.length() < limit) {
        var velVector = Vector3f()
        if (inputState.forward) velVector += vectorFront(inputState.cameraY, 0f, force * deltaSec)
        if (inputState.backwards) velVector -= vectorFront(inputState.cameraY, 0f, force * deltaSec)
        if (inputState.right) velVector -= vectorFront(inputState.cameraY + 90f, 0f, force * deltaSec)
        if (inputState.left) velVector -= vectorFront(inputState.cameraY - 90f, 0f, force * deltaSec)
        if (velVector.length() > 0.001) {
            box.rigidBody!!.activate()
            val finalVel = box.linearVelocity + velVector
            if (finalVel.length() > limit) {
                finalVel.normalize()
                finalVel.scale(limit)
            }
            box.linearVelocity = finalVel.get()
        }  else {
            // apply the "friction"?

        }
    }
    val new = box.linearVelocity.get()
    new.y = 0f
    if (new.length() > 0.01) {
        new.normalize()
        new.scale(-1f)
        new.scale(20f * deltaSec)
        box.linearVelocity += new.get()
    }

    // apply the "friction"?
    /*val new = box.linearVelocity.get()
    new.normalize()
    new.scale(-1f)
    new.scale(0.01f * deltaSec)
    box.linearVelocity += new.get()*/

    // Jump
    if (inputState.jump &&  box.inGround) {
        box.linearVelocity += Vector3f(0f, force * 25f * deltaSec, 0f)
    }
}