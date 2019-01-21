package com.peinbol

import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import javax.vecmath.Color4f
import javax.vecmath.Matrix4f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f

class Box(
    var id: Int = 0,
    var position: Vector3f = Vector3f(),
    val size: Vector3f = Vector3f(),
    var velocity: Vector3f = Vector3f(),
    var angularVelocity: Vector3f = Vector3f(),
    val mass: Float = 0f,
    val theColor: Color4f = Color4f(1f, 1f, 1f, 1f),
    var affectedByPhysics: Boolean = true,
    var inGround: Boolean = false,
    val textureId: Int = Textures.METAL_ID,
    var textureMultiplier: Double = 1.0,
    var bounceMultiplier: Float = 0f
) {

    // TODO: update setters for position, velocity, angularVelocity
    // TODO: rename velocity to linearVelocity
    // TODO: single method update, to pass various parameters and build the motionstate only once (for client updates)

    fun applyForce(force: Vector3f) {
        rigidBody!!.applyCentralImpulse(force)
    }

    fun readPosition(): Vector3f {
        val body = rigidBody!!
        val transform = Transform()
        val origin = body.motionState.getWorldTransform(transform).origin
        this.position = origin
        return position
    }

    fun updatePosition(position: Vector3f) {
        val body = rigidBody!!
        val transform = Transform()
        val rotation = Quat4f()
        body.motionState.getWorldTransform(transform).getRotation(rotation)
        body.motionState = DefaultMotionState(Transform(Matrix4f(rotation, position, 1f)))
        this.position = position.get()
    }

    fun readLinearVelocity(): Vector3f {
        val result = Vector3f()
        val body = rigidBody!!
        body.getLinearVelocity(result)
        this.velocity = result
        return result
    }

    fun updateLinearVelocity(velocity: Vector3f) {
        val body = rigidBody!!
        body.setLinearVelocity(velocity.get())
        this.velocity = velocity.get()
    }

    fun updateAngularVelocity(velocity: Vector3f) {
        val body = rigidBody!!
        body.setAngularVelocity(velocity.get())
        this.angularVelocity = velocity.get()
    }

    /** Reference to bullet physics body */
    var rigidBody: RigidBody? = null
}