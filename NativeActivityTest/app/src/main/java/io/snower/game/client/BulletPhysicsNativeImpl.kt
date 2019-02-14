package io.snower.game.client

import io.snower.game.common.*
import java.util.*

/** Implements physics with bullet 2.x using JNI mostly. */
class BulletPhysicsNativeImpl : PhysicsInterface {

    // Native physics functions.
    private external fun createWorld(): Long
    private external fun deleteWorld(handle: Long)
    private external fun createBodyInWorld(
        worldHandle: Long,
        type: Int, // TYPE_* in companion
        mass: Float,
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float
    ): Long
    private external fun deleteBodyFromWorld(worldHandle: Long, bodyHandle: Long)
    private external fun simulate(worldHandle: Long, time: Float)
    private external fun getBodyOpenGLMatrix(bodyHandle: Long, dst: FloatArray)
    private external fun getBodyHandleData(bodyHandle: Long, dst: FloatArray) // to update box data with simulation data
    private external fun updateBodyWorldTransform(
        bodyHandle: Long,
        x: Float, y: Float, z: Float,
        q1: Float, q2: Float, q3: Float, q4: Float)
    private external fun updateBodyVelocity(
        bodyHandle: Long,
        linearX: Float, linearY: Float, linearZ: Float,
        angularX: Float, angularY: Float, angularZ: Float)

    private val boxes = mutableSetOf<Box>()
    private var worldHandle: Long = 0L
    private val bodyDataDst = FloatArray(BODY_DATA_SIZE) // tmp, to read simulation data for each box

    fun init() {
        check(worldHandle == 0L) { "worldHandle already initialized (is $worldHandle)"}
        worldHandle = createWorld()
    }

    fun destroy() {
        check(worldHandle != 0L) { "worldHandle not initialized (is $worldHandle)"}
        deleteWorld(worldHandle)
        boxes.clear()
    }

    override fun register(box: Box) {
        if (box !in boxes) {
            val (x, y, z) = box.position
            val (sx, sy, sz) = box.size
            // TODO: make this properly from common, to server-client
            val type = if (box.isSphere) TYPE_BULLET else if (box.isCharacter) TYPE_CHARACTER else TYPE_BOX
            val bodyHandle = createBodyInWorld(worldHandle, type, box.mass, x, y, z, sx, sy, sz)
            box.physicsHandle = bodyHandle
            boxes += box
        }
    }

    override fun unRegister(box: Box) {
        if (box in boxes) {
            val bodyHandle = box.physicsHandle as Long
            deleteBodyFromWorld(worldHandle, bodyHandle)
            boxes -= box
        }
    }

    override fun getBoxOpenGLMatrix(box: Box, dst: FloatArray) {
        // this must be done natively.
        val handle = box.physicsHandle as Long
        getBodyOpenGLMatrix(handle, dst)
    }

    override fun simulate(delta: Int, updateObjs: Boolean, updateId: Int) {
        val start = System.currentTimeMillis()

        // Commit changes to the engine, if any.
        for (box in boxes) {
            if (box.shouldCommitTransformChanges) {
                val handle = box.physicsHandle as Long
                val (x, y, z) = box.position
                val (rX, rY, rZ, rW) = box.rotation
                updateBodyWorldTransform(handle, x, y, z, rX, rY, rZ, rW)
                box.shouldCommitTransformChanges = false
            }
            if (box.shouldCommitMomentumChanges) {
                val handle = box.physicsHandle as Long
                val (lX, lY, lZ) = box.linearVelocity
                val (aX, aY, aZ) = box.angularVelocity
                updateBodyVelocity(handle, lX, lY, lZ, aX, aY, aZ)
                box.shouldCommitMomentumChanges = false
            }
        }

        // Simulate
        simulate(worldHandle, delta.toFloat()/1000f)

        // Poll simulation results back to java
        for (box in boxes) {
            if (box.mass == 0f) continue

            val handle = box.physicsHandle as Long
            getBodyHandleData(handle, bodyDataDst)

            // update position
            box.position.x = bodyDataDst[POSITION_OFFSET + 0]
            box.position.y = bodyDataDst[POSITION_OFFSET + 1]
            box.position.z = bodyDataDst[POSITION_OFFSET + 2]

            // update quaternion
            box.rotation.x = bodyDataDst[QUATERNION_OFFSET + 0]
            box.rotation.y = bodyDataDst[QUATERNION_OFFSET + 1]
            box.rotation.z = bodyDataDst[QUATERNION_OFFSET + 2]
            box.rotation.w = bodyDataDst[QUATERNION_OFFSET + 3]

            // update velocity
            box.linearVelocity.x = bodyDataDst[LINEAR_VELOCITY_OFFSET + 0]
            box.linearVelocity.y = bodyDataDst[LINEAR_VELOCITY_OFFSET + 1]
            box.linearVelocity.z = bodyDataDst[LINEAR_VELOCITY_OFFSET + 2]

            // update angular velocity
            box.angularVelocity.x = bodyDataDst[ANGULAR_VELOCITY_OFFSET + 0]
            box.angularVelocity.y = bodyDataDst[ANGULAR_VELOCITY_OFFSET + 1]
            box.angularVelocity.z = bodyDataDst[ANGULAR_VELOCITY_OFFSET + 2]
        }

        lastSimulationMillis = (System.currentTimeMillis() - start).toFloat()
    }

    override var lastSimulationMillis: Float = 0f
        private set

    companion object {
        // offsets for getBodyHandleData
        private const val POSITION_OFFSET = 0
        private const val QUATERNION_OFFSET = 3
        private const val LINEAR_VELOCITY_OFFSET = 7
        private const val ANGULAR_VELOCITY_OFFSET = 10
        private const val BODY_DATA_SIZE = 13

        private const val TYPE_BOX = 0
        private const val TYPE_CHARACTER = 1
        private const val TYPE_BULLET = 2
    }
}