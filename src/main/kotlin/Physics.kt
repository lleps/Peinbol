class Physics(val gravity: Double = -9.8) {
    var boxes: List<Box> = emptyList()

    fun simulate(delta: Double) {
        val deltaSec = delta / 1000.0 // multiplying by delta means box velocity is meters/sec
        for (b in boxes) {
            if (b.affectedByPhysics) {
                b.vz += gravity
                for (b2 in boxes) {
                    if (b2 != b) {
                        // check if b collides with b2
                    }
                }
            }
        }
    }
}