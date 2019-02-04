package io.snower.game.client

import android.content.res.AssetManager
import java.nio.charset.Charset

class AndroidAssetResolver(private val assetManager: AssetManager) : AssetResolver {

    // both functions may avoid copying using a pre allocated buffer

    override fun getAsByteArray(path: String): ByteArray {
        assetManager.open(path).use {
            return it.readBytes()
        }
    }

    override fun getAsString(path: String): String {
        assetManager.open(path).use {
            return it.readBytes().toString(Charset.defaultCharset())
        }
    }
}