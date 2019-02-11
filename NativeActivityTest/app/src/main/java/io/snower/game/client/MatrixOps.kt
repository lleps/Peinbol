package io.snower.game.client

/** Defines matrix-vector and matrix-matrix operations used in the renderer. */
interface MatrixOps {
    fun multiplyMM(dst: FloatArray, lhs: FloatArray, rhs: FloatArray)
    fun multiplyMV(dstVec: FloatArray, lhsMat: FloatArray, rhsVec: FloatArray)
    fun identity(dst: FloatArray)
    fun perspective(dst: FloatArray, fovy: Float, aspect: Float, zNear: Float, zFar: Float)
    fun lookAt(dst: FloatArray,
               eyeX: Float, eyeY: Float, eyeZ: Float,
               centerX: Float, centerY: Float, centerZ: Float,
               upX: Float, upY: Float, upZ: Float)
    fun translate(dst: FloatArray, x: Float, y: Float, z: Float)
    fun rotate(dst: FloatArray, angleDegrees: Float, x: Float, y: Float, z: Float)
    fun scale(dst: FloatArray, x: Float, y: Float, z: Float)
}