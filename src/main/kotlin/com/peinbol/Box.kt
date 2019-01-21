package com.peinbol

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import javax.vecmath.Color4f
import javax.vecmath.Matrix4f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f

class Box(
    val id: Int = 0,
    position: Vector3f = Vector3f(),
    val size: Vector3f = Vector3f(),
    linearVelocity: Vector3f = Vector3f(),
    angularVelocity: Vector3f = Vector3f(),
    rotation: Quat4f = Quat4f(0f, 0f, 0f, 1f),
    val mass: Float = 0f,
    val theColor: Color4f = Color4f(1f, 1f, 1f, 1f),
    var affectedByPhysics: Boolean = true, // not useful, as mass 0 equals "static object".
    var inGround: Boolean = false, // set by physics to true if velocity y is close to zero
    val textureId: Int = Textures.METAL_ID,
    val textureMultiplier: Double = 1.0,
    val bounceMultiplier: Float = 0f
) {
    var position: Vector3f = position
        set(value) {
            if (!syncing) {
                val body = rigidBody!!
                val transform = Transform()
                val rotation = Quat4f()
                body.motionState.getWorldTransform(transform).getRotation(rotation)
                body.motionState = DefaultMotionState(Transform(Matrix4f(rotation.get(), position.get(), 1f)))
            }
            field = value.get()
        }

    var linearVelocity: Vector3f = linearVelocity
        set(value) {
            if (!syncing) {
                val body = rigidBody!!
                body.setLinearVelocity(value.get())
            }
            field = value.get()
        }

    var angularVelocity: Vector3f = angularVelocity
        set(value) {
            if (!syncing) {
                val body = rigidBody!!
                body.setAngularVelocity(value.get())
            }
            field = value.get()
        }

    var syncing = false

    var rotation: Quat4f = rotation
        set(value) {
            if (!syncing) {
                val body = rigidBody!!
                val transform = Transform()
                val origin = body.motionState.getWorldTransform(transform).origin
                body.motionState = DefaultMotionState(Transform(Matrix4f(value.get(), origin.get(), 1f)))
            }
            field = value.get()
        }

    /** Reference to bullet physics body. Read by Window to get rotation, and set by Physics.register. */
    var rigidBody: RigidBody? = null

    fun applyForce(force: Vector3f) {
        rigidBody!!.activate()
        rigidBody!!.applyCentralImpulse(force)
    }
}