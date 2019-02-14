package io.snower.game.client

import android.opengl.GLES20
import java.nio.Buffer

class GLESImpl : GLInterface {
    override val GL_VERTEX_SHADER: Int
        get() = GLES20.GL_VERTEX_SHADER
    override val GL_FRAGMENT_SHADER: Int
        get() = GLES20.GL_FRAGMENT_SHADER
    override val GL_DEPTH_BUFFER_BIT: Int
        get() = GLES20.GL_DEPTH_BUFFER_BIT
    override val GL_COLOR_BUFFER_BIT: Int
        get() = GLES20.GL_COLOR_BUFFER_BIT
    override val GL_CULL_FACE: Int
        get() = GLES20.GL_CULL_FACE
    override val GL_DEPTH_TEST: Int
        get() = GLES20.GL_DEPTH_TEST
    override val GL_TEXTURE0: Int
        get() = GLES20.GL_TEXTURE0
    override val GL_SRC_ALPHA: Int
        get() = GLES20.GL_SRC_ALPHA
    override val GL_ONE_MINUS_SRC_ALPHA: Int
        get() = GLES20.GL_ONE_MINUS_SRC_ALPHA
    override val GL_BLEND: Int
        get() = GLES20.GL_BLEND
    override val GL_FLOAT: Int
        get() = GLES20.GL_FLOAT
    override val GL_TRIANGLES: Int
        get() = GLES20.GL_TRIANGLES
    override val GL_LINK_STATUS: Int
        get() = GLES20.GL_LINK_STATUS
    override val GL_COMPILE_STATUS: Int
        get() = GLES20.GL_COMPILE_STATUS
    override val GL_LINEAR_MIPMAP_LINEAR: Int
        get() = GLES20.GL_LINEAR_MIPMAP_LINEAR
    override val GL_REPEAT: Int
        get() = GLES20.GL_REPEAT
    override val GL_UNPACK_ALIGNMENT: Int
        get() = GLES20.GL_UNPACK_ALIGNMENT
    override val GL_TEXTURE_MIN_FILTER: Int
        get() = GLES20.GL_TEXTURE_MIN_FILTER
    override val GL_TEXTURE_MAG_FILTER: Int
        get() = GLES20.GL_TEXTURE_MAG_FILTER
    override val GL_TEXTURE_WRAP_S: Int
        get() = GLES20.GL_TEXTURE_WRAP_S
    override val GL_TEXTURE_WRAP_T: Int
        get() = GLES20.GL_TEXTURE_WRAP_T
    override val GL_RGBA: Int
        get() = GLES20.GL_RGBA
    override val GL_UNSIGNED_BYTE: Int
        get() = GLES20.GL_UNSIGNED_BYTE
    override val GL_TEXTURE_2D: Int
        get() = GLES20.GL_TEXTURE_2D

    override fun glGetUniformLocation(handle: Int, name: String): Int {
        return GLES20.glGetUniformLocation(handle, name)
    }

    override fun glGetAttribLocation(handle: Int, name: String): Int {
        return GLES20.glGetAttribLocation(handle, name)
    }

    override fun glClearColor(r: Float, g: Float, b: Float, a: Float) {
        GLES20.glClearColor(r, g, b, a)
    }

    override fun glClear(bits: Int) {
        GLES20.glClear(bits)
    }

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        GLES20.glViewport(x, y, width, height)
    }

    override fun glEnable(feature: Int) {
        GLES20.glEnable(feature)
    }

    override fun glDisable(feature: Int) {
        GLES20.glDisable(feature)
    }

    override fun glUseProgram(program: Int) {
        GLES20.glUseProgram(program)
    }

    override fun glActiveTexture(texture: Int) {
        GLES20.glActiveTexture(texture)
    }

    override fun glUniform1i(handle: Int, value: Int) {
        GLES20.glUniform1i(handle, value)
    }

    override fun glVertexAttribPointer(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        ptr: Buffer
    ) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, ptr)
    }

    override fun glVertexAttribPointerOffset(
        index: Int,
        size: Int,
        type: Int,
        normalized: Boolean,
        stride: Int,
        offset: Int
    ) {
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, offset)
    }

    override fun glEnableVertexAttribArray(handle: Int) {
        GLES20.glEnableVertexAttribArray(handle)
    }

    override fun glUniformMatrix4fv(handle: Int, transpose: Boolean, value: FloatArray) {
        GLES20.glUniformMatrix4fv(handle, 0, transpose, value, 0)
    }

    override fun glUniform3f(handle: Int, v1: Float, v2: Float, v3: Float) {
        GLES20.glUniform3f(handle, v1, v2, v3)
    }

    override fun glBlendFunc(param1: Int, param2: Int) {
        GLES20.glBlendFunc(param1, param2)
    }

    override fun glDrawArrays(shape: Int, first: Int, count: Int) {
        GLES20.glDrawArrays(shape, first, count)
    }

    override fun glCreateShader(type: Int): Int {
        return GLES20.glCreateShader(type)
    }

    override fun glShaderSource(shader: Int, source: String) {
        GLES20.glShaderSource(shader, source)
    }

    override fun glCompileShader(shader: Int) {
        GLES20.glCompileShader(shader)
    }

    override fun glGetShaderiv(shader: Int, data: Int, dst: IntArray) {
        GLES20.glGetShaderiv(shader, data, dst, 0)
    }

    override fun glDeleteShader(shader: Int) {
        GLES20.glDeleteShader(shader)
    }

    override fun glGetShaderInfoLog(shader: Int): String {
        return GLES20.glGetShaderInfoLog(shader)
    }

    override fun glCreateProgram(): Int {
        return GLES20.glCreateProgram()
    }

    override fun glAttachShader(program: Int, shader: Int) {
        GLES20.glAttachShader(program, shader)
    }

    override fun glBindAttribLocation(program: Int, index: Int, name: String) {
        GLES20.glBindAttribLocation(program, index, name)
    }

    override fun glLinkProgram(program: Int) {
        GLES20.glLinkProgram(program)
    }

    override fun glGetProgramiv(program: Int, data: Int, dst: IntArray) {
        GLES20.glGetProgramiv(program, data, dst, 0)
    }

    override fun glDeleteProgram(program: Int) {
        GLES20.glDeleteProgram(program)
    }

    override fun glGetProgramInfoLog(program: Int): String {
        return GLES20.glGetProgramInfoLog(program)
    }

    override fun glGenTextures(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        return ids[0]
    }

    override fun glPixelStorei(pname: Int, param: Int) {
        GLES20.glPixelStorei(pname, param)
    }

    override fun glTexParameteri(target: Int, pname: Int, param: Int) {
        GLES20.glTexParameteri(target, pname, param)
    }

    override fun glTexImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        type: Int,
        pixels: Buffer
    ) {
        GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)
    }

    override fun glGenerateMipmap(target: Int) {
        GLES20.glGenerateMipmap(target)
    }

    override fun glBindTexture(target: Int, id: Int) {
        GLES20.glBindTexture(target, id)
    }

    override fun glDeleteTexture(id: Int) {
        GLES20.glDeleteTextures(1, intArrayOf(id), 0)
    }
}