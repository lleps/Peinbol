package com.peinbol.client

import com.peinbol.timedOscillator
import org.lwjgl.BufferUtils
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
class HealthUI : NkUIDrawable {

    private val progressPtr = BufferUtils.createPointerBuffer(1).put(0, 220)

    var health: Int = 100

    override fun draw(ctx: NkContext, screenWidth: Float, screenHeight: Float) {
        MemoryStack.stackPush().use { stack ->
            val crosshairCenterColor = NkColor.callocStack(stack).set(0xC6.toByte(), 0x28.toByte(), 0x28.toByte(), 0xFF.toByte())
            val barWidth = 400f
            val barHeight = 30f
            nkBeginTransparentWindow(ctx, "health", (screenWidth / 2f) - (barWidth / 2f), screenHeight - 50f, barWidth, barHeight+10) {
                nk_layout_row_dynamic(ctx, barHeight, 1)
                progressPtr.clear().put(0, health.toLong())
                ctx.style().progress().active().data().color().set(crosshairCenterColor)
                ctx.style().progress().cursor_active().data().color().set(crosshairCenterColor)
                ctx.style().progress().cursor_normal().data().color().set(crosshairCenterColor)
                nk_progress(ctx, progressPtr, 100, false)
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
}