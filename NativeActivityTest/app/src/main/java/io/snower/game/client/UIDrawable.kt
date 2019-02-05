package io.snower.game.client


/** An UI element which supports instant mode ui drawing */
interface UIDrawable {
    fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float)
}
