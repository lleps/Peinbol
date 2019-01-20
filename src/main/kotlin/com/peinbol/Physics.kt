package com.peinbol

class Physics(
    val gravity: Double = -1.5
) {
    companion object {
        private const val DELTA_TO_STOP_BOUNCING = 0.3 // for
    }
    var boxes: List<Box> = emptyList()

    fun simulate(delta: Double) {
        val deltaSec = delta / 1000.0 // multiplying by delta means box velocity is meters/sec

        // TODO use delta, to make parameters clear!
        for (b in boxes) {
            if (b.affectedByPhysics) {
                var inGround = false
                for (b2 in boxes) {
                    if (b2 == b) continue

                    // Ground
                    if (b.y + b.vy - (b.sy/2.0) <= b2.y + (b2.sy/2.0) && // distinta
                        b.y + (b.sy/2.0) >= b2.y - (b2.sy/2.0) &&
                        // x
                        b.x + (b.sx/2.0) > b2.x - (b2.sx/2.0) &&
                        b.x - (b.sx/2.0) < b2.x + (b2.sx/2.0) &&
                        // z
                        b.z + (b.sz/2.0) > b2.z - (b2.sz/2.0) &&
                        b.z - (b.sz/2.0) < b2.z + (b2.sz/2.0)
                    ) {
                        b.vy *= -b.bounceMultiplier
                        if (Math.abs(b.vy) < DELTA_TO_STOP_BOUNCING) b.vy = 0.0
                        b.y = b2.y + (b2.sy / 2.0) + (b.sy / 2.0)
                        inGround = true
                    }
                    // TODO make roof collision
                    if (b.vx > 0) {
                        if (b.x + (b.vx) + (b.sx/2.0) > b2.x - (b2.sx/2.0) &&
                            b.x - (b.sx/2.0) < b2.x + (b2.sx/2.0) &&

                            // y
                            b.y + (b.sy/2.0) > b2.y - (b2.sy/2.0) &&
                            b.y - (b.sy/2.0) < b2.y + (b2.sy/2.0) &&
                            // z
                            b.z + (b.sz/2.0) > b2.z - (b2.sz/2.0) &&
                            b.z - (b.sz/2.0) < b2.z + (b2.sz/2.0)
                        ) {
                            b.vx *= -b.bounceMultiplier
                            if (Math.abs(b.vx) < DELTA_TO_STOP_BOUNCING) b.vx = 0.0
                            b.x = b2.x - (b2.sx / 2.0) - (b.sx / 2.0)
                        }
                    } else if (b.vx < 0) {
                        if (b.x + (b.sx/2.0) > b2.x - (b2.sx/2.0) &&
                            b.x + b.vx - (b.sx/2.0) < b2.x + (b2.sx/2.0) &&

                            // y
                            b.y + (b.sy/2.0) > b2.y - (b2.sy/2.0) &&
                            b.y - (b.sy/2.0) < b2.y + (b2.sy/2.0) &&
                            // z
                            b.z + (b.sz/2.0) > b2.z - (b2.sz/2.0) &&
                            b.z - (b.sz/2.0) < b2.z + (b2.sz/2.0)
                        ) {
                            b.vx *= -b.bounceMultiplier
                            if (Math.abs(b.vx) < DELTA_TO_STOP_BOUNCING) b.vx = 0.0
                            b.x = b2.x + (b2.sx / 2.0) + (b.sx / 2.0)
                        }
                    }
                    // collide with z
                    if (b.vz > 0) {
                        if (b.z + b.vz + (b.sz/2.0) > b2.z - (b2.sz/2.0) &&
                            b.z - (b.sz/2.0) < b2.z + (b2.sz/2.0) &&

                            // y
                            b.y + (b.sy/2.0) > b2.y - (b2.sy/2.0) &&
                            b.y - (b.sy/2.0) < b2.y + (b2.sy/2.0) &&
                            // x
                            b.x + (b.sx/2.0) > b2.x - (b2.sx/2.0) &&
                            b.x - (b.sx/2.0) < b2.x + (b2.sx/2.0)
                        ) {
                            b.vz *= -b.bounceMultiplier
                            if (Math.abs(b.vz) < DELTA_TO_STOP_BOUNCING) b.vz = 0.0
                            b.z = b2.z - (b2.sz / 2.0) - (b.sz / 2.0)
                        }
                    } else if (b.vz < 0) {
                        if (b.z + (b.sz/2.0) > b2.z - (b2.sz/2.0) &&
                            b.z + b.vz - (b.sz/2.0) < b2.z + (b2.sz/2.0) &&

                            // y
                            b.y + (b.sy/2.0) > b2.y - (b2.sy/2.0) &&
                            b.y - (b.sy/2.0) < b2.y + (b2.sy/2.0) &&
                            // x
                            b.x + (b.sx/2.0) > b2.x - (b2.sx/2.0) &&
                            b.x - (b.sx/2.0) < b2.x + (b2.sx/2.0)
                        ) {
                            b.vz *= -b.bounceMultiplier
                            if (Math.abs(b.vz) < DELTA_TO_STOP_BOUNCING) b.vz = 0.0
                            b.z = b2.z + (b2.sz / 2.0) + (b.sz / 2.0)
                        }
                    }
                }

                b.x += b.vx
                b.z += b.vz
                b.inGround = inGround
                if (!inGround) {
                    b.vy += gravity * deltaSec
                    b.y += b.vy
                }
                val friction = if (inGround) 1.5 else 1.0/deltaSec
                b.vx *= (friction * deltaSec)
                b.vz *= (friction * deltaSec)
                // TODO fix friction, is kinda weird. Should simply substract?
            }
        }
    }
}