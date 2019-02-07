package io.snower.game.client

class AndroidMovementUI : UIDrawable {
    companion object {
        private const val RING_RADIUS = 90f
        private const val INNER_RADIUS = 20f
        private const val RING_COLOR = 0x80808099.toInt()
        private const val INNER_COLOR = 0x505050AA
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
            drawCircle(drawer,
                centerX, centerY,
                RING_RADIUS,
                RING_COLOR,
                10f)
            drawCircle(drawer,
                centerX, centerY,
                INNER_RADIUS,
                INNER_COLOR,
                0f)
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