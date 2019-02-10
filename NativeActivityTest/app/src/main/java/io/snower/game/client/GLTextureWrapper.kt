package io.snower.game.client

import io.snower.game.common.BufferUtils
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Wraps an image buffer from a .png InputStream and
 * offers methods to load and bind it as a texture
 * in OpenGL.
 *
 * Depends on JVM ByteBuffers and PNGDecoder, so can't
 * be directly ported.
 */
class GLTextureWrapper(val buffer: ByteBuffer, val width: Int, val height: Int) {

    companion object {
        /** Create an instance from a PNG file stream. May throw exceptions. */
        fun createFromPNGInputStream(input: InputStream): GLTextureWrapper {
            val dec = PNGDecoder(input)
            val width = dec.width
            val height = dec.height
            val bpp = 4
            val buf = BufferUtils.createByteBuffer(bpp * width * height)
            dec.decode(buf, width * bpp, PNGDecoder.Format.RGBA)
            buf.flip()
            return GLTextureWrapper(buf, width, height)
        }
    }

    var loadedInGL = false
        private set

    private var id: Int = 0

    fun load(
        gl: GLInterface,
        filter: Int = gl.GL_LINEAR_MIPMAP_LINEAR,
        wrap: Int = gl.GL_REPEAT
    ) {
        if (loadedInGL) return
        loadedInGL = true
        val target = gl.GL_TEXTURE_2D
        id = gl.glGenTextures()
        bind(gl)
        gl.glPixelStorei(gl.GL_UNPACK_ALIGNMENT, 1)
        gl.glTexParameteri(target, gl.GL_TEXTURE_MIN_FILTER, filter)
        gl.glTexParameteri(target, gl.GL_TEXTURE_MAG_FILTER, filter)
        gl.glTexParameteri(target, gl.GL_TEXTURE_WRAP_S, wrap)
        gl.glTexParameteri(target, gl.GL_TEXTURE_WRAP_T, wrap)
        gl.glTexImage2D(target, 0, gl.GL_RGBA, width, height, 0, gl.GL_RGBA, gl.GL_UNSIGNED_BYTE, buffer)
        gl.glGenerateMipmap(target)
    }

    fun bind(gl: GLInterface) {
        if (!loadedInGL) return
        gl.glBindTexture(gl.GL_TEXTURE_2D, id)
    }

    fun unload(gl: GLInterface) {
        if (!loadedInGL) return
        gl.glDeleteTexture(id)
        loadedInGL = false
    }
}