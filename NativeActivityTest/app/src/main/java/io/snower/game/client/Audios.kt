package io.snower.game.client

object Audios {
    const val WALK = 1
    const val HIT = 2
    const val SHOOT = 3
    const val SPLASH = 4

    val FILES = mapOf(
        WALK to "steps.ogg",
        HIT to "hit.ogg",
        SHOOT to "shoot.ogg",
        SPLASH to "splash.ogg"
    )
}