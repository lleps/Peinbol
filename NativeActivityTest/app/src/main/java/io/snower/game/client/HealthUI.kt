package io.snower.game.client

/**
 * Draws the crosshair on the middle of the screen.
 * Crosshair size depends on the given velocity.
 */
class HealthUI : UIDrawable {

    //private val progressPtr = BufferUtils.createPointerBuffer(1).put(0, 220)

    var health: Int = 100
    var visible = true

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        if (!visible) return
        /*MemoryStack.stackPush().use { stack ->
            val crosshairCenterColor = NkColor.callocStack(stack).set(0xC6.toByte(), 0x28.toByte(), 0x28.toByte(), 0xFF.toByte())
            val BLACK_TRANSPARENT = Nuklear.nk_rgba(0, 0, 0, 100, NkColor.callocStack())
            val col2 = NkColor.callocStack(stack).set(0x63.toByte(), 0xFF.toByte(), 0x99.toByte(), 0xFF.toByte())
            val barWidth = 400f
            val barHeight = 30f
            nkBeginTransparentWindow(drawer, "health", (screenWidth / 2f) - (barWidth / 2f), screenHeight - 50f, barWidth, barHeight+10) {
                nk_layout_row_dynamic(drawer, barHeight, 1)
                progressPtr.clear().put(0, health.toLong())
                //drawer.style().progress().active().data().color().set(crosshairCenterColor)
                //drawer.style().progress().cursor_active().data().color().set(crosshairCenterColor)
                drawer.style().progress().cursor_normal().data().color().set(crosshairCenterColor)
                drawer.style().progress().normal().data().color(BLACK_TRANSPARENT)
                nk_progress(drawer, progressPtr, 100, false)
            }
        }*/
    }

    /*private fun nkDrawCircle(
        ctx: NkContext,
        centerX: Float,
        centerY: Float,
        radius: Float,
        thickness: Float,
        color: NkColor
    ) {
        val cmdBuffer = nk_window_get_canvas(ctx)!!
        nk_stroke_circle(
            cmdBuffer,
            // must divide radius by 2 to use the "real"
            NkRect.callocStack().set(centerX - radius, centerY - radius, radius*2f, radius*2f),
            thickness,
            color
        )
    }*/
}