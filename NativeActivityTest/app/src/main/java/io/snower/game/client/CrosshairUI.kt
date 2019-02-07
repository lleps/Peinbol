package io.snower.game.client

import javax.vecmath.Vector3f

/**
 * Draws the crosshair on the middle of the screen.
 * Crosshair size depends on the given velocity.
 */
class CrosshairUI(val velocitySupplier: () -> Vector3f) : UIDrawable {
    companion object {
        private const val CROSSHAIR_MIN_RADIUS = 30f
        private const val CROSSHAIR_MAX_RADIUS = 60f
        private const val CROSSHAIR_RING_COLOR = 0xE0E0E0E0.toInt()
        private const val CROSSHAIR_CENTER_COLOR = 0x662828FF.toInt()
    }

    var visible = true

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        if (!visible) return
        var radius = CROSSHAIR_MIN_RADIUS
        radius += velocitySupplier().length() * 3.5f
        radius = radius.coerceAtMost(CROSSHAIR_MAX_RADIUS)
        val screenCenterX = (screenWidth / 2f)
        val screenCenterY = (screenHeight / 2f)
        if (drawer.begin("crosshair",
                screenCenterX-200f, screenCenterY-200f,
                400f, 400f,
                background = 0x0,
                flags = drawer.WINDOW_NO_SCROLLBAR)) {
            drawer.layoutRowStatic(400f, 400f, 1) // alloc some space to draw
            drawCircle(drawer, screenCenterX, screenCenterY, radius, 4f, CROSSHAIR_RING_COLOR)
            drawCircle(drawer, screenCenterX, screenCenterY, 1f, 3f, CROSSHAIR_CENTER_COLOR)
        }
        drawer.end()
    }

    private fun drawCircle(
        drawer: UIDrawer,
        centerX: Float,
        centerY: Float,
        radius: Float,
        thickness: Float,
        color: Int
    ) = drawer.strokeCircle(centerX - radius, centerY - radius, radius*2f, thickness, color)
}