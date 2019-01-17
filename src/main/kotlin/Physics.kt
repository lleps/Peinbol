class Physics(val gravity: Double = -9.8) {
    var boxes: List<Box> = emptyList()

    fun simulate(delta: Double) {
        val deltaSec = delta / 1000.0 // multiplying by delta means box velocity is meters/sec

        for (b in boxes) {
            if (b.affectedByPhysics) {

                // apply gravity
                b.vy += gravity * deltaSec

                // move by velocity
                b.x += b.vx
                b.y += b.vy
                b.z += b.vz

                // check collision
                for (b2 in boxes) {
                    if (b2 == b) continue

                    // check if b is inside b2 (floor)
                    if (b.y- (b.sy / 2.0) < b2.y + (b2.sy / 2.0) && // distinta
                        // x
                        b.x + (b.sx/2.0) > b2.x - (b2.sx/2.0) &&
                        b.x - (b.sx/2.0) < b2.x + (b2.sx/2.0) &&
                        // z
                        b.z + (b.sz/2.0) > b2.z - (b2.sz/2.0) &&
                        b.z - (b.sz/2.0) < b2.z + (b2.sz/2.0)
                    ) {
                        // collide with
                        b.vy = -(b.vy * 0.5)
                        b.y = b2.y + (b2.sy / 2.0) + (b.sy / 2.0)
                    }
                }
            }
        }
    }
}