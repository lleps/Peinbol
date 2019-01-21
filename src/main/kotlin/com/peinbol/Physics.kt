package com.peinbol

import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.StaticPlaneShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.DynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.MotionState
import com.bulletphysics.linearmath.Transform
import javax.vecmath.Matrix4f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f

class Physics(private val gravity: Vector3f = Vector3f(0f, -0.05f, 0f)) {
    companion object {
        private const val DELTA_TO_STOP_BOUNCING = 0.1 // round error to go 0
    }

    private lateinit var world: DynamicsWorld
    private val boxes = mutableListOf<Box>()

    fun init() {
        val broadphase = DbvtBroadphase()
        val collisionConfiguration = DefaultCollisionConfiguration()
        val collisionDispatcher = CollisionDispatcher(collisionConfiguration)
        val constraintSolver = SequentialImpulseConstraintSolver()
        world = DiscreteDynamicsWorld(collisionDispatcher, broadphase, constraintSolver, collisionConfiguration)
        world.setGravity(Vector3f(0f, -10f, 0f))
    }

    fun register(box: Box) {
        if (box in boxes) return
        boxes += box
        val constructionInfo = RigidBodyConstructionInfo(
            box.mass,
            DefaultMotionState(Transform(
                Matrix4f(Quat4f(0f, 0f, 0f, 1f), box.position.get(), 1f)
            )),
            BoxShape(box.size.withOps { scale(0.5f) })
        )
        constructionInfo.restitution = box.bounceMultiplier
        val body = RigidBody(constructionInfo)
        world.addRigidBody(body)
        box.rigidBody = body
    }

    fun unRegister(box: Box) {
        if (box !in boxes) return
        boxes -= box
        world.removeRigidBody(box.rigidBody!!)
    }

    fun simulate(delta: Double) {
        world.stepSimulation(delta.toFloat() / 1000f)
        // now copy to the objects pos the real data
        val transform = Transform()
        for (box in boxes) {
            val rigidBody = box.rigidBody!!
            rigidBody.motionState.getWorldTransform(transform)
            // copy to box the physics info
            box.position = transform.origin.get()
            rigidBody.getLinearVelocity(box.velocity)
            rigidBody.getAngularVelocity(box.angularVelocity)
            // TODO: set inGround if velocity y is low
            // TODO: also read quaternion
        }
    }
}