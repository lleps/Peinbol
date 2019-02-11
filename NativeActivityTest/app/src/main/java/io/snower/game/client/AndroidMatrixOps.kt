package io.snower.game.client

import android.opengl.Matrix

class AndroidMatrixOps : MatrixOps {
    override fun multiplyMM(dst: FloatArray, lhs: FloatArray, rhs: FloatArray) {
        Matrix.multiplyMM(dst, 0, lhs, 0, rhs, 0)
    }

    override fun multiplyMV(dstVec: FloatArray, lhsMat: FloatArray, rhsVec: FloatArray) {
        Matrix.multiplyMV(dstVec, 0, lhsMat, 0, rhsVec, 0)
    }

    override fun identity(dst: FloatArray) {
        Matrix.setIdentityM(dst, 0)
    }

    override fun perspective(dst: FloatArray, fovy: Float, aspect: Float, zNear: Float, zFar: Float) {
        Matrix.perspectiveM(dst, 0, fovy, aspect, zNear, zFar)
    }

    override fun lookAt(
        dst: FloatArray,
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float
    ) {
        Matrix.setLookAtM(dst, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
    }

    override fun translate(dst: FloatArray, x: Float, y: Float, z: Float) {
        Matrix.translateM(dst, 0, x, y, z)
    }

    override fun rotate(dst: FloatArray, angleDegrees: Float, x: Float, y: Float, z: Float) {
        Matrix.rotateM(dst, 0, angleDegrees, x, y, z)
    }

    override fun scale(dst: FloatArray, x: Float, y: Float, z: Float) {
        Matrix.scaleM(dst, 0, x, y, z)
    }
}