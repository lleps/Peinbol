package io.snower.game.client


/** To shot latency, fps, etc. For devs. */
class PlayersInfoUI : UIDrawable {

    /*val players = listOf(
        "pepito",
        "juanelpro16",
        "jorgito",
        "{SG} TXD__DESTROYER_DXT d(^.^)b",
        "Pancho_Marquez"
    )

    private val WIDTH_PER_CHARACTER = 8f
    private val longestMsg = players.maxBy { it.length }?.length ?: 1
    private val BLACK_TRANSPARENT = Nuklear.nk_rgba(0, 0, 0, 100, NkColor.callocStack())

    private val windowWidth = longestMsg * WIDTH_PER_CHARACTER * 3
    private val windowHeight = 26f * (1 + players.size) // title + players

    */
    var visible = false

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        if (!visible) return
        /*nkBeginTransparentWindow(
            drawer,
            "Players info",
                (screenWidth / 2f) - (windowWidth / 2f),
                (screenHeight / 2f) - (windowHeight / 2f),
                windowWidth, windowHeight,
                NK_WINDOW_TITLE + NK_WINDOW_NO_SCROLLBAR,
                BLACK_TRANSPARENT
        ) {
            for (player in players) {
                nk_layout_row_dynamic(drawer, 20f, 3)
                nk_label(drawer,
                    player, NK_TEXT_ALIGN_LEFT
                )
                nk_label(drawer,
                    "55", NK_TEXT_ALIGN_CENTERED
                )
                nk_label(drawer,
                    "102 ms", NK_TEXT_ALIGN_RIGHT
                )
            }
        }*/
    }
}