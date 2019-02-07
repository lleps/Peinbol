package io.snower.game.client

import android.util.Log
import javax.vecmath.Vector2f
import kotlin.math.atan2

class AndroidMovementUI : UIDrawable {
    companion object {
        var mouseX: Float? = null
        var mouseY: Float? = null

        var forward = false
        var backwards = false
        var left = false
        var right = false

        private const val RING_RADIUS = 90f
        private const val INNER_RADIUS = 20f
        private const val RING_COLOR = 0xFF8080FF.toInt()
        private const val INNER_COLOR = 0x5FF050FF
        private const val X_PAD = 100f //
        private const val Y_PAD = 100f // padding from bottom-left
    }

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        // first, get some screen pos.
        // and, some radius. CircleRadius
        if (drawer.begin(
                "controls",
                0f, screenHeight / 2f,
                screenWidth / 2f, screenHeight / 2f,
                background = 0x0,
                flags = drawer.WINDOW_NO_SCROLLBAR)) {
            val centerX = X_PAD + RING_RADIUS
            val centerY = screenHeight - Y_PAD - RING_RADIUS

            forward = false
            backwards = false
            left = false
            right = false

            drawCircle(drawer,
                centerX, centerY,
                RING_RADIUS,
                RING_COLOR,
                10f)
            val mx = mouseX
            val my = mouseY
            if (mx == null) {
                drawCircle(drawer,
                    centerX, centerY,
                    INNER_RADIUS,
                    INNER_COLOR,
                    0f)

            } else {
                // now, to vector
                val direction = Vector2f(mx - centerX, my!! - centerY)
                if (direction.length() > RING_RADIUS) {
                    Log.i("tag", "out of the ring")
                    direction.normalize()
                    direction.scale(RING_RADIUS)
                } else {
                    Log.i("tag", "in the ring")
                }

                // check direction
                val angle = Math.toDegrees(atan2(direction.y, direction.x).toDouble()).toInt() / (360/8)
                left = angle == 3 || angle == -3
                right = angle == 0
                forward = angle == -1 || angle == -2
                backwards = angle == 1 || angle == 2
                // = Math.toDegrees(Vector2f().angle(direction).toDouble())
                Log.i("tag", "angle: $angle")
                drawCircle(drawer,
                    centerX + direction.x, centerY + direction.y,
                    INNER_RADIUS,
                    INNER_COLOR,
                    0f)
            }
            //Log.i("tag", "mouse: ($mouseX, $mouseY)")
        }
        drawer.end()
    }

    private fun drawCircle(
        drawer: UIDrawer,
        x: Float, y: Float,
        radius: Float,
        color: Int,
        thickness: Float = 0f) {
        if (thickness != 0f) {
            drawer.strokeCircle(x - radius, y - radius, radius*2f, thickness, color)
        } else {
            drawer.fillCircle(x - radius, y - radius, radius*2f, color)
        }
    }
}