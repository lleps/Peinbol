package io.snower.game.client

class ChatUI : UIDrawable {
    companion object {
        private const val BLACK_TRANSPARENT = 0x00000064
        private const val WIDTH_PER_CHARACTER = 8.25f
        private const val MSG_EXPIRY_MILLIS = 10 * 1000
    }

    private class MessageEntry(val msg: String, val timeOfCreation: Long)
    private var messages = listOf<MessageEntry>()

    fun addMessage(msg: String) {
        messages += MessageEntry(msg, System.currentTimeMillis())
        if (messages.size > 20) messages = messages.takeLast(20)
    }

    var visible = true

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        if (!visible) return
        messages = messages.filter { System.currentTimeMillis() - it.timeOfCreation < MSG_EXPIRY_MILLIS }
        val longestMsg = messages.maxBy { it.msg.length }?.msg?.length ?: 1
        val width = longestMsg * WIDTH_PER_CHARACTER
        val padding = 15f
        val x = screenWidth - width - padding
        val y = padding
        val height = messages.size * 24f
        drawer.begin("Chat", x, y, width, height, background = BLACK_TRANSPARENT)
        for (msg in messages) {
            drawer.layoutRowDynamic(20f, 1)
            drawer.label(msg.msg, drawer.TEXT_RIGHT)
        }
        drawer.end()
    }
}