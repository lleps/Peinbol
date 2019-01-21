package com.peinbol

import javax.vecmath.Vector3f

class Physics(private val gravity: Vector3f = Vector3f(0f, -0.05f, 0f)) {
    companion object {
        private const val DELTA_TO_STOP_BOUNCING = 0.1 // round error to go 0
    }

    var boxes: List<Box> = emptyList()
    var debugBox: Box? = null

    fun simulate(delta: Double) {
        for (b in boxes) {
            if (b.affectedByPhysics) {
                if (debugBox == null && b.mass > 0.1f) {
                    //debugBox = b
                }

                // TODO: divide into: apply(friction,gravity), update(), checkCollision(). (checkCollision may ignore velocity)

                // Apply friction and gravity
                val c = if (!b.inGround) 0.01f else 0.5f
                val normal = 1
                val frictionMag = c * normal
                val friction = b.velocity.get()
                if (friction.length() > 0.000001) {
                    friction.scale(-1f)
                    friction.normalize()
                    friction.scale(frictionMag)
                    b.applyForce(friction)
                }
                b.applyForce(gravity.withOps { scale(b.mass) })

                // Update
                b.velocity.add(b.acceleration)
                b.position.add(b.velocity)
                b.acceleration.scale(0f)

                // Check collision
                var collide = false
                for (b2 in boxes) {
                    if (b2 == b) continue

                    if (checkCollision(b, b2)) {
                        collide = true
                    }
                }
                b.inGround = collide


                /*
                b.position.x += b.velocity.x
                b.position.z += b.velocity.z
                b.inGround = collide

                if (!collide) {
                    b.velocity.y += gravity
                    b.position.y += b.velocity.y
                }*/
                //val friction = if (collide) 0.9f else 1.0f
                //b.velocity.x *= friction
                //b.velocity.z *= friction
            }
        }
    }

    private fun checkCollision(b: Box, b2: Box): Boolean {
        // TODO may use alignedX/Y/Z(b,b2) to reduce boilerplate.
        // TODO dont add speed...
        var result = false
        // Ground
        if (
            b.position.y + (b.size.y/2f) > b2.position.y - (b2.size.y/2f) &&
            b.position.y - (b.size.y/2f) < b2.position.y + (b2.size.y/2f) &&
            //b.position.y + b.velocity.y - (b.size.y/2f) <= b2.position.y + (b2.size.y/2f) &&
            //b.position.y + (b.size.y/2f) >= b2.position.y + (b2.size.y/2f) &&
            // x
            b.position.x + (b.size.x/2f) > b2.position.x - (b2.size.x/2f) &&
            b.position.x - (b.size.x/2f) < b2.position.x + (b2.size.x/2f) &&
            // z
            b.position.z + (b.size.z/2f) > b2.position.z - (b2.size.z/2f) &&
            b.position.z - (b.size.z/2f) < b2.position.z + (b2.size.z/2f)
        ) {
            b.velocity.y *= -b.bounceMultiplier
            if (Math.abs(b.velocity.y) < DELTA_TO_STOP_BOUNCING) b.velocity.y = 0f
            b.position.y = b2.position.y + (b2.size.y / 2f) + (b.size.y / 2f)
            result = true
        }
        // TODO add roof collision
        if (b.velocity.x > 0) {
            if (b.position.x + (b.size.x/2f) > b2.position.x - (b2.size.x/2f) &&
                b.position.x - (b.size.x/2f) < b2.position.x + (b2.size.x/2f) &&
                // y
                b.position.y + (b.size.y/2f) > b2.position.y - (b2.size.y/2f) &&
                b.position.y - (b.size.y/2f) < b2.position.y + (b2.size.y/2f) &&
                // z
                b.position.z + (b.size.z/2f) > b2.position.z - (b2.size.z/2f) &&
                b.position.z - (b.size.z/2f) < b2.position.z + (b2.size.z/2f)
            ) {
                b.velocity.x *= -b.bounceMultiplier
                if (Math.abs(b.velocity.x) < DELTA_TO_STOP_BOUNCING) b.velocity.x = 0f
                b.position.x = b2.position.x - (b2.size.x / 2f) - (b.size.x / 2f)
                result = true
            }
        } else if (b.velocity.x < 0) {
            if (b.position.x + (b.size.x/2f) > b2.position.x - (b2.size.x/2f) &&
                b.position.x - (b.size.x/2f) < b2.position.x + (b2.size.x/2f) &&
                // y
                b.position.y + (b.size.y/2f) > b2.position.y - (b2.size.y/2f) &&
                b.position.y - (b.size.y/2f) < b2.position.y + (b2.size.y/2f) &&
                // z
                b.position.z + (b.size.z/2f) > b2.position.z - (b2.size.z/2f) &&
                b.position.z - (b.size.z/2f) < b2.position.z + (b2.size.z/2f)
            ) {
                b.velocity.x *= -b.bounceMultiplier
                if (Math.abs(b.velocity.x) < DELTA_TO_STOP_BOUNCING) b.velocity.x = 0f
                b.position.x = b2.position.x + (b2.size.x / 2f) + (b.size.x / 2f)
                result = true
            }
        }
        // collide with z
        if (b.velocity.z > 0) {
            if (b.position.z + (b.size.z/2f) > b2.position.z - (b2.size.z/2f) &&
                b.position.z - (b.size.z/2f) < b2.position.z + (b2.size.z/2f) &&
                // y
                b.position.y + (b.size.y/2f) > b2.position.y - (b2.size.y/2f) &&
                b.position.y - (b.size.y/2f) < b2.position.y + (b2.size.y/2f) &&
                // x
                b.position.x + (b.size.x/2f) > b2.position.x - (b2.size.x/2f) &&
                b.position.x - (b.size.x/2f) < b2.position.x + (b2.size.x/2f)
            ) {
                b.velocity.z *= -b.bounceMultiplier
                if (Math.abs(b.velocity.z) < DELTA_TO_STOP_BOUNCING) b.velocity.z = 0f
                b.position.z = b2.position.z - (b2.size.z / 2f) - (b.size.z / 2f)
                result = true
            }
        } else if (b.velocity.z < 0) {
            if (b.position.z + (b.size.z/2f) > b2.position.z - (b2.size.z/2f) &&
                b.position.z - (b.size.z/2f) < b2.position.z + (b2.size.z/2f) &&
                // y
                b.position.y + (b.size.y/2f) > b2.position.y - (b2.size.y/2f) &&
                b.position.y - (b.size.y/2f) < b2.position.y + (b2.size.y/2f) &&
                // x
                b.position.x + (b.size.x/2f) > b2.position.x - (b2.size.x/2f) &&
                b.position.x - (b.size.x/2f) < b2.position.x + (b2.size.x/2f)
            ) {
                b.velocity.z *= -b.bounceMultiplier
                if (Math.abs(b.velocity.z) < DELTA_TO_STOP_BOUNCING) b.velocity.z = 0f
                b.position.z = b2.position.z + (b2.size.z / 2f) + (b.size.z / 2f)
                result = true
            }
        }
        return result
    }
}