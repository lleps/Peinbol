class Box(
    var x: Double, var y: Double, var z: Double,
    var sx: Double, var sy: Double, var sz: Double,
    var vx: Double = 0.0, var vy: Double = 0.0, var vz: Double = 0.0,
    var affectedByPhysics: Boolean = true,
    var color: Color = Color()
)

class Color(val r: Double = 1.0, val g: Double = 1.0, val b: Double = 1.0, val a: Double = 1.0) {
    companion object {
        val RED = Color(1.0, 0.0, 0.0)
        val GREEN = Color(0.0, 1.0, 0.0)
        val WHITE = Color(1.0, 1.0, 1.0)
    }
}