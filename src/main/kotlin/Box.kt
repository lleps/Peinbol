class Box(
        var x: Double, var y: Double, var z: Double,
        var sx: Double, var sy: Double, var sz: Double,
        var vx: Double = 0.0, var vy: Double = 0.0, var vz: Double = 0.0,
        var affectedByPhysics: Boolean = true
)