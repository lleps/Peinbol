package io.snower.game.client

import io.snower.game.common.Box
import io.snower.game.common.PhysicsInterface

/** Implements physics with bullet 2.x using JNI mostly. */
class BulletPhysicsNativeImpl : PhysicsInterface {

    // so, the idea is pretty much ok.
    // now, need to implement the actual jni capabilities.

    // offsets for getBodyHandleData
    private val POSITION_OFFSET = 0
    private val ROTATION_QUATERNION_OFFSET = 3
    private val LINEAR_VELOCITY_OFFSET = 7
    private val ANGULAR_VELOCITY_OFFSET = 10
    private val BODY_DATA_SIZE = 13

    // interfaces prototyping for bullet 2.x
    external fun createBody(/*all the flags, inertia? this? that? etc*/): Int
    external fun destroyBody(bodyHandle: Int)
    external fun simulate(time: Float)
    external fun readOpenGLMatrix(bodyHandle: Int, dst: FloatArray)
    external fun getBodyHandleData(bodyHandle: Int, dst: FloatArray) // to update local data with simulation.
    external fun applyForce(handle: Int, x: Float, y: Float, z: Float)

    external fun updateBodyWorldTransform(
        handle: Int, x: Float, y: Float, z: Float, q1: Float, q2: Float, q3: Float, q4: Float
    )

    external fun updateBodyVelocity(
        handle: Int, linearX: Float, linearY: Float, linearZ: Float, angularX: Float, angularY: Float, angularZ: Float
    )

    private val boxes = mutableSetOf<Box>()

    override fun register(box: Box) {
        boxes += box
    }

    override fun unRegister(box: Box) {
        boxes += box
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBoxOpenGLMatrix(box: Box, dst: FloatArray) {
        // this must be done natively.

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val bodyDataDst = FloatArray(BODY_DATA_SIZE)

    override fun simulate(delta: Int, updateObjs: Boolean, updateId: Int) {
        val start = System.currentTimeMillis()
        simulate(delta.toFloat()/1000f)
        for (box in boxes) {
            // TODO: Generates too much garbage dud. must do something about that.
            val handle = box.physicsHandle as Int
            // may only be applied to those with mass != 0f
            getBodyHandleData(handle, bodyDataDst)
            box.position.x = bodyDataDst[POSITION_OFFSET + 0]
            box.position.y = bodyDataDst[POSITION_OFFSET + 1]
            box.position.z = bodyDataDst[POSITION_OFFSET + 2]
            // etc...
        }
        lastSimulationMillis = (System.currentTimeMillis() - start).toFloat()
    }

    override var lastSimulationMillis: Float = 0f
        private set

    // 2.x or the C api?
    // need to...
    //  setup a single native function through the ndk.
    //  compile the ndk
}