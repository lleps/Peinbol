package io.snower.game.client

/**
 * Unified interface to resolve assets (i.e shaders, textures, fonts)
 * in android and desktop. And probably the web.
 */
interface AssetResolver {
    fun getAsString(path: String): String
    fun getAsByteArray(path: String): ByteArray
}