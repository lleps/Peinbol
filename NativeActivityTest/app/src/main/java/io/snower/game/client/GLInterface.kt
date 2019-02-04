package io.snower.game.client

import java.nio.Buffer

/**
 * Defines used OpenGL methods, so the drawing implementations may be
 * reused with WebGL, GLES, LWJGL, etc.
 */
interface GLInterface {
    // Constants
    val GL_VERTEX_SHADER: Int
    val GL_FRAGMENT_SHADER: Int
    val GL_DEPTH_BUFFER_BIT: Int
    val GL_COLOR_BUFFER_BIT: Int
    val GL_CULL_FACE: Int
    val GL_DEPTH_TEST: Int
    val GL_TEXTURE0: Int
    val GL_SRC_ALPHA: Int
    val GL_ONE_MINUS_SRC_ALPHA: Int
    val GL_BLEND: Int
    val GL_FLOAT: Int
    val GL_TRIANGLES: Int
    val GL_LINK_STATUS: Int
    val GL_COMPILE_STATUS: Int
    val GL_LINEAR_MIPMAP_LINEAR: Int
    val GL_REPEAT: Int
    val GL_UNPACK_ALIGNMENT: Int
    val GL_TEXTURE_MIN_FILTER: Int
    val GL_TEXTURE_MAG_FILTER: Int
    val GL_TEXTURE_WRAP_S: Int
    val GL_TEXTURE_WRAP_T: Int
    val GL_RGBA: Int
    val GL_UNSIGNED_BYTE: Int
    val GL_TEXTURE_2D: Int

    // Commands
    fun glGetUniformLocation(handle: Int, name: String): Int
    fun glGetAttribLocation(handle: Int, name: String): Int
    fun glClearColor(r: Float, g: Float, b: Float, a: Float)
    fun glClear(bits: Int)
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glEnable(feature: Int)
    fun glDisable(feature: Int)
    fun glUseProgram(program: Int)
    fun glActiveTexture(texture: Int)
    fun glUniform1i(handle: Int, value: Int)
    fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, ptr: Buffer)
    fun glEnableVertexAttribArray(handle: Int)
    fun glUniformMatrix4fv(handle: Int, transpose: Boolean, value: FloatArray)
    fun glUniform3f(handle: Int, v1: Float, v2: Float, v3: Float)
    fun glBlendFunc(param1: Int, param2: Int)
    fun glDrawArrays(shape: Int, first: Int, count: Int)
    fun glCreateShader(type: Int): Int
    fun glShaderSource(shader: Int, source: String)
    fun glCompileShader(shader: Int)
    fun glGetShaderiv(shader: Int, data: Int, dst: IntArray)
    fun glDeleteShader(shader: Int)
    fun glGetShaderInfoLog(shader: Int): String
    fun glCreateProgram(): Int
    fun glAttachShader(program: Int, shader: Int)
    fun glBindAttribLocation(program: Int, index: Int, name: String)
    fun glLinkProgram(program: Int)
    fun glGetProgramiv(program: Int, data: Int, dst: IntArray)
    fun glDeleteProgram(program: Int)
    fun glGetProgramInfoLog(program: Int): String
    fun glGenTextures(): Int
    fun glPixelStorei(pname: Int, param: Int)
    fun glTexParameteri(target: Int, pname: Int, param: Int)
    fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: Buffer)
    fun glGenerateMipmap(target: Int)
    fun glBindTexture(target: Int, id: Int)
    fun glDeleteTexture(id: Int)
}
