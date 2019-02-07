package io.snower.game.client


/** To shot latency, fps, etc. For devs. */
class PlayersInfoUI : UIDrawable {
    private val players = listOf(
        "pepito",
        "juanelpro16",
        "jorgito",
        "{SG} TXD__DESTROYER_DXT d(^.^)b",
        "Pancho_Marquez"
    )

    private val WIDTH_PER_CHARACTER = 8f
    private val longestMsg = players.maxBy { it.length }?.length ?: 1
    private val BLACK_TRANSPARENT = 0x00000064

    private val windowWidth = longestMsg * WIDTH_PER_CHARACTER * 3
    private val windowHeight = 26f * (1 + players.size) // title + players

    var visible = false

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        if (!visible) return
        if (drawer.begin(
            "Players info",
                (screenWidth / 2f) - (windowWidth / 2f),
                (screenHeight / 2f) - (windowHeight / 2f),
                windowWidth, windowHeight,
                BLACK_TRANSPARENT,
                drawer.WINDOW_TITLE or drawer.WINDOW_NO_SCROLLBAR)) {
            for (player in players) {
                drawer.layoutRowDynamic(20f, 3)
                drawer.label(player, drawer.TEXT_LEFT)
                drawer.label("55", drawer.TEXT_CENTER)
                drawer.label("102 ms", drawer.TEXT_RIGHT)
            }
        }
        drawer.end()
    }
}