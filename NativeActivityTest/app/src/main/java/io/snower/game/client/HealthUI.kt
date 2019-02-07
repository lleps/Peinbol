package io.snower.game.client

class HealthUI : UIDrawable {
    private val HEALTH_COLOR = 0xC62828FF.toInt()
    private val HEALTH_BACKGROUND = 0x00000064

    var health: Int = 100
    var visible = true

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        if (!visible) return
        val barWidth = 400f
        val barHeight = 30f
        if (drawer.begin("health",
                (screenWidth / 2f) - (barWidth / 2f), screenHeight - 50f,
                barWidth, barHeight+10,
                background = 0x0,
                flags = drawer.WINDOW_NO_SCROLLBAR)) {
            drawer.layoutRowDynamic(barHeight, 1)
            drawer.progress(health, 100, HEALTH_COLOR, HEALTH_BACKGROUND)
        }
        drawer.end()
    }
}