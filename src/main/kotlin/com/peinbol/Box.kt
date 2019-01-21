package com.peinbol

import javax.vecmath.Color4f
import javax.vecmath.Vector3f

class Box(
    var id: Int = 0,
    var position: Vector3f = Vector3f(),
    val size: Vector3f = Vector3f(),
    var velocity: Vector3f = Vector3f(),
    val acceleration: Vector3f = Vector3f(),
    val mass: Float = 0f,
    val theColor: Color4f = Color4f(1f, 1f, 1f, 1f),
    var affectedByPhysics: Boolean = true,
    var inGround: Boolean = false,
    val textureId: Int = Textures.METAL_ID,
    var textureMultiplier: Double = 1.0,
    var bounceMultiplier: Float = 0f
) {
    fun applyForce(force: Vector3f) {
        if (mass > 0f) {
            //println("apply force: $force")
            acceleration.x += force.x / mass
            acceleration.y += force.y / mass
            acceleration.z += force.z / mass
        }
    }
}