package io.snower.game.client

class HealthUI : UIDrawable {
    companion object {
        private const val BAR_WIDTH = 400f
        private const val BAR_HEIGHT = 35f
        private const val HEALTH_COLOR = 0xD32F2FFF.toInt()
        private const val HEALTH_BACKGROUND = 0x00000064
    }

    var health: Int = 100
    var visible = true

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        if (!visible) return

        if (drawer.begin("health",
                (screenWidth / 2f) - (BAR_WIDTH / 2f), screenHeight - 50f,
                BAR_WIDTH, BAR_HEIGHT+10,
                background = 0x0,
                flags = drawer.WINDOW_NO_SCROLLBAR)) {
            drawer.layoutRowDynamic(BAR_HEIGHT, 1)
            drawer.progress(health, 100, HEALTH_COLOR, HEALTH_BACKGROUND)
        }
        drawer.end()
    }
}