package com.peinbol.client

import org.lwjgl.nuklear.NkColor
import org.lwjgl.nuklear.NkContext
import org.lwjgl.nuklear.NkRect
import org.lwjgl.nuklear.NkStyleItem
import org.lwjgl.nuklear.Nuklear.*
import org.lwjgl.system.MemoryStack
import javax.vecmath.Vector3f

/**
 * Draws the crosshair on the middle of the screen.
 * Crosshair size depends on the given velocity.
 */
class CrosshairUI(val velocitySupplier: () -> Vector3f) : NkUIDrawable {

    companion object {
        private const val CROSSHAIR_MIN_RADIUS = 30f
        private const val CROSSHAIR_MAX_RADIUS = 60f
    }

    var visible = true

    override fun draw(ctx: NkContext, screenWidth: Float, screenHeight: Float) {
        if (!visible) return
        MemoryStack.stackPush().use { stack ->
            val crosshairRingColor = NkColor.callocStack(stack).set(0xE0.toByte(), 0xE0.toByte(), 0xE0.toByte(), 0xE0.toByte())
            val crosshairCenterColor = NkColor.callocStack(stack).set(0xC6.toByte(), 0x28.toByte(), 0x28.toByte(), 0xFF.toByte())
            var radius = CROSSHAIR_MIN_RADIUS
            radius += velocitySupplier().length() * 3.5f
            radius = radius.coerceAtMost(CROSSHAIR_MAX_RADIUS)
            val screenCenterX = (screenWidth / 2f)
            val screenCenterY = (screenHeight / 2f)
            nkBeginTransparentWindow(ctx, "crosshair", screenCenterX-200f, screenCenterY-200f, 400f, 400f) {
                nk_layout_row_static(ctx, 400f, 400, 1) // alloc some space to draw
                nkDrawCircle(ctx, screenCenterX, screenCenterY, radius, 4f, crosshairRingColor)
                nkDrawCircle(ctx, screenCenterX, screenCenterY, 1f, 3f, crosshairCenterColor)
            }
        }
    }

    private fun nkDrawCircle(
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
            NkRect.callocStack().set(centerX - radius, centerY - radius, radius*2f, radius*2f),
            thickness,
            color
        )
    }
}