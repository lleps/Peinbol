package io.snower.game.common

import javax.vecmath.Color4f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f

class Box(
    val id: Int = 0,
    position: Vector3f = Vector3f(),
    val size: Vector3f = Vector3f(), // if isSphere, size.x is the radius
    linearVelocity: Vector3f = Vector3f(),
    angularVelocity: Vector3f = Vector3f(),
    rotation: Quat4f = Quat4f(0f, 0f, 0f, 1f),
    val mass: Float = 0f,
    var theColor: Color4f = Color4f(1f, 1f, 1f, 1f),
    var affectedByPhysics: Boolean = true, // not useful, as mass 0 equals "static object".
    var inGround: Boolean = false, // set by physics to true if linearVelocity y is close to zero
    val textureId: Int = Textures.METAL_ID,
    val textureMultiplier: Double = 1.0,
    val bounceMultiplier: Float = 0f,
    val isSphere: Boolean = false,
    val isCharacter: Boolean = false,
    var shouldTransmit: Boolean = true // certain objects may be transmitted only once, to save cpu (i.e balls)
) {

    fun applyForce(force: Vector3f) {
        val forceCopy = force.get()
        forceCopy.scale(1f / mass)
        linearVelocity.add(forceCopy)
        shouldCommitMomentumChanges = true
    }

    var position: Vector3f = position
        set(value) {
            shouldCommitTransformChanges = true
            field = value.get()
        }

    var rotation: Quat4f = rotation
        set(value) {
            shouldCommitTransformChanges = true
            field = value.get()
        }

    var linearVelocity: Vector3f = linearVelocity
        set(value) {
            shouldCommitMomentumChanges = true
            field = value.get()
        }

    var angularVelocity: Vector3f = angularVelocity
        set(value) {
            shouldCommitMomentumChanges = true
            field = value.get()
        }

    // Physics metadata
    var shouldCommitTransformChanges: Boolean = true // always commit on first simulation
    var shouldCommitMomentumChanges: Boolean = true // always commit on first simulation
    var physicsHandle: Any? = null

    // Renderer metadata
    var rendererHandle: Any? = null

}