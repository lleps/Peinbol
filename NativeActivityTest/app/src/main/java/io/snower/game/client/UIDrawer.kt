package io.snower.game.client

/** Implements all methods and constants to draw in immediate-mode. */
interface UIDrawer {
    fun begin(title: String, x: Float, y: Float, width: Float, height: Float, background: Int = -1, flags: Int = 0): Boolean
    fun end()
    fun layoutRowDynamic(height: Float, columns: Int)
    fun layoutRowStatic(height: Float, width: Float, columns: Int)
    fun label(text: String, align: Int)
    fun strokeCircle(x: Float, y: Float, diameter: Float, thickness: Float, color: Int)
    fun fillCircle(x: Float, y: Float, diameter: Float, color: Int)
    fun progress(current: Int, max: Int, color: Int = -1, background: Int = -1)

    val WINDOW_TITLE: Int
    val WINDOW_NO_SCROLLBAR: Int

    val TEXT_LEFT: Int
    val TEXT_CENTER: Int
    val TEXT_RIGHT: Int
}