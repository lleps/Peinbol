package com.peinbol.client

import org.lwjgl.nuklear.NkColor
import org.lwjgl.nuklear.NkContext
import org.lwjgl.nuklear.Nuklear
import org.lwjgl.nuklear.Nuklear.*

class ChatUI : NkUIDrawable {
    companion object {
        private val BLACK_TRANSPARENT = Nuklear.nk_rgba(0, 0, 0, 100, NkColor.callocStack())
        private const val WIDTH_PER_CHARACTER = 8.25f
        private const val MSG_EXPIRY_MILLIS = 10 * 1000
    }

    private class MessageEntry(val msg: String, val timeOfCreation: Long)
    private var messages = listOf<MessageEntry>()

    fun addMessage(msg: String) {
        messages += MessageEntry(msg, System.currentTimeMillis())
        if (messages.size > 20) messages = messages.takeLast(20)
    }

    override fun draw(ctx: NkContext, screenWidth: Float, screenHeight: Float) {
        messages = messages.filter { System.currentTimeMillis() - it.timeOfCreation < MSG_EXPIRY_MILLIS }
        val longestMsg = messages.maxBy { it.msg.length }?.msg?.length ?: 1
        val width = longestMsg * WIDTH_PER_CHARACTER
        val padding = 15f
        val x = screenWidth - width - padding
        val y = padding
        val height = messages.size * 24f
        nkBeginTransparentWindow(ctx, "Chat", x, y, width, height, background = BLACK_TRANSPARENT) {
            for (msg in messages) {
                nk_layout_row_dynamic(ctx, 20f, 1)
                nk_label(ctx, msg.msg, NK_TEXT_RIGHT)
            }
        }
    }
}