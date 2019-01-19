package com.peinbol

class Physics(
    val gravity: Double = -1.0
) {
    var boxes: List<Box> = emptyList()

    fun simulate(delta: Double) {
        val deltaSec = delta / 1000.0 // multiplying by delta means box velocity is meters/sec

        for (b in boxes) {
            if (b.affectedByPhysics) {
                var inGround = false
                for (b2 in boxes) {
                    if (b2 == b) continue

                    // Ground
                    if (b.y + b.vy - (b.sy/2.0) <= b2.y + (b2.sy/2.0) && // distinta
                        b.y + (b.sy/2.0) >= b2.y + (b2.sy/2.0) &&
                        // x
                        b.x + (b.sx/2.0) > b2.x - (b2.sx/2.0) &&
                        b.x - (b.sx/2.0) < b2.x + (b2.sx/2.0) &&
                        // z
                        b.z + (b.sz/2.0) > b2.z - (b2.sz/2.0) &&
                        b.z - (b.sz/2.0) < b2.z + (b2.sz/2.0)
                    ) {
                        b.vy = -(b.vy * 0.5)
                        if (Math.abs(b.vy) < 0.5) b.vy = 0.0
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
                            b.vx = 0.0
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
                            b.vx = 0.0
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
                            b.vz = 0.0
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
                            b.vz = 0.0
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
                } else {
                    // ground friction
                    b.vx *= 0.8
                    b.vz *= 0.8
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val b1 = Box(0, 1.0,1.0,5.0,  1.0,1.0,1.0)
            val b2 = Box(0, 3.0,3.0,5.0,  2.0,2.0,2.0)
            val out = Box()
            val result = boxesIntersect(b1, 1.0, 1.0, 0.0, b2, out)
            println("Intersect: $result | out: ${out.x} ${out.y} ${out.z}")
        }

        private fun boxesIntersect(
            b1: Box,
            b1VelX: Double,
            b1VelY: Double,
            b1VelZ: Double,
            b2: Box,
            intersectPos: Box
        ): Boolean {
            // The position checked for collision is b1 plus their speed
            val b1MinX = (b1.x + b1VelX) - (b1.sx / 2.0)
            val b1MaxX = (b1.x + b1VelX) + (b1.sx / 2.0)
            val b1MinY = (b1.y + b1VelY) - (b1.sy / 2.0)
            val b1MaxY = (b1.y + b1VelY) + (b1.sy / 2.0)
            val b1MinZ = (b1.z + b1VelZ) - (b1.sz / 2.0)
            val b1MaxZ = (b1.z + b1VelZ) + (b1.sz / 2.0)

            // TODO maybe expand b2 (sx,sy,sz) with velocity as well.
            val b2MinX = b2.x - (b2.sx / 2.0)
            val b2MaxX = b2.x + (b2.sx / 2.0)
            val b2MinY = b2.y - (b2.sy / 2.0)
            val b2MaxY = b2.y + (b2.sy / 2.0)
            val b2MinZ = b2.z - (b2.sz / 2.0)
            val b2MaxZ = b2.z + (b2.sz / 2.0)

            if ((b1MinX <= b2MaxX && b1MaxX >= b2MinX) &&
                (b1MinY <= b2MaxY && b1MaxY >= b2MinY) &&
                (b1MinZ <= b2MaxZ && b1MaxZ >= b2MinZ)) {
                // know that collides, now need to get the position
                // for the intersection, on which they don't collide
                // b1: Prev pos of box1
                // b1+vel: Curr pos of box1
                // b2: Target
                // Need to get b1 + (vel * q) where q is in (0..1)
                // b1PosX + q*VelX = b2MinX (if going from left to right)
                // NOTE: is not always b2MinX. Is the nearest to b1PosX.. is "b2EdgeX".
                // q = (b2EdgeX - b1PosX) / velX
                // NOTE: Is not b1PosX?
                // if can't get a proper "q" pick a random edge...
                var intersectX: Double = b1.x
                var intersectY: Double = b1.y
                var intersectZ: Double = b1.z

                if (b1VelX != 0.0) {
                    val b2EdgeX = if (b1VelX < 0.0) b2MaxX else b2MinX
                    val b1EdgeX = if (b1VelX > 0.0) b1.x + (b1.sx / 2.0) else b1.x - (b1.sx / 2.0) // edge to check for q
                    val q = (b2EdgeX - b1EdgeX) / b1VelX
                    intersectX = b1.x + (b1VelX * q)
                }

                if (b1VelY != 0.0) {
                    val b2EdgeY = if (b1VelY < 0.0) b2MaxY else b2MinY
                    val b1EdgeY = if (b1VelY > 0.0) b1.y + (b1.sy / 2.0) else b1.y - (b1.sy / 2.0) // edge to check for q
                    val q = (b2EdgeY - b1EdgeY) / b1VelY
                    intersectY = b1.y + (b1VelY * q)
                }
                if (b1VelZ != 0.0) {
                    val b2EdgeZ = if (b1VelZ < 0.0) b2MaxZ else b2MinZ
                    val b1EdgeZ = if (b1VelZ > 0.0) b1.z + (b1.sz / 2.0) else b1.z - (b1.sz / 2.0) // edge to check for q
                    val q = (b2EdgeZ - b1EdgeZ) / b1VelZ
                    intersectZ = b1.z + (b1VelZ * q)
                }

                intersectPos.x = intersectX
                intersectPos.y = intersectY
                intersectPos.z = intersectZ
                return true
            }
            return false
        }
    }

    // return where should be the box.
    // an average.

}