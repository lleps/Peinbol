package com.peinbol

import org.lwjgl.nuklear.*
import org.lwjgl.nuklear.Nuklear.*
import org.lwjgl.system.MemoryStack
import javax.vecmath.Vector3f

/**
 * Draws the crosshair on the middle of the screen.
 * Crosshair size depends on the given velocity.
 */
class CrosshairUI(val velocitySupplier: () -> Vector3f) : NkUIDrawable {

    companion object {
        private const val CROSSHAIR_MIN_RADIUS = 50f
        private const val CROSSHAIR_MAX_RADIUS = 100f
    }

    override fun draw(ctx: NkContext, screenWidth: Float, screenHeight: Float) {
        MemoryStack.stackPush().use { stack ->
            val crosshairRingColor = NkColor.callocStack(stack).set(0xE0.toByte(), 0xE0.toByte(), 0xE0.toByte(), 0xE0.toByte())
            val crosshairCenterColor = NkColor.callocStack(stack).set(0xC6.toByte(), 0x28.toByte(), 0x28.toByte(), 0xFF.toByte())
            var radius = CROSSHAIR_MIN_RADIUS
            radius += velocitySupplier().length() * 10f
            radius = radius.coerceAtMost(CROSSHAIR_MAX_RADIUS)
            val screenCenterX = (screenWidth / 2f)
            val screenCenterY = (screenHeight / 2f)
            nkBeginTransparentWindow(ctx, screenCenterX-200f, screenCenterY-200f, 400f, 400f) {
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
            // must divide radius by 2 to use the "real"
            NkRect.callocStack().set(centerX - radius, centerY - radius, radius*2f, radius*2f),
            thickness,
            color
        )
    }

    private inline fun nkBeginTransparentWindow(
        ctx: NkContext,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        block: () -> Unit
    ) {
        val style = ctx.style()
        nk_style_push_color(ctx, style.window().background(), nk_rgba(0, 0, 0, 0, NkColor.callocStack()))
        nk_style_push_style_item(
            ctx,
            style.window().fixed_background(),
            nk_style_item_color(nk_rgba(0, 0, 0, 0, NkColor.callocStack()), NkStyleItem.callocStack())
        )
        val rect = NkRect.mallocStack()

        if (nk_begin(
                ctx,
                "",
                nk_rect(x, y, w, h, rect),
                NK_WINDOW_NO_SCROLLBAR
            )) {
            block()
        }

        nk_end(ctx)
        nk_style_pop_style_item(ctx)
        nk_style_pop_color(ctx)
    }

    private inline fun nKBeginDefaultWindow(
        ctx: NkContext,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        block: () -> Unit
    ) {

        val rect = NkRect.mallocStack()

        if (nk_begin(
                ctx,
                "the god.",
                nk_rect(x, y, w, h, rect),
                NK_WINDOW_NO_SCROLLBAR
            )) {
            block()
        }

        nk_end(ctx)
        //nk_style_pop_style_item(ctx)
        //nk_style_pop_color(ctx)
    }
}