package io.snower.game.client


/** An UI element which supports custom nuklear logic to draw */
interface NkUIDrawable {
    fun draw(ctx: NkContext, screenWidth: Float, screenHeight: Float)
}
