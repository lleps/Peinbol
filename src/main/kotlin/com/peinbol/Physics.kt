package com.peinbol

import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.DynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import javax.vecmath.Matrix4f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f

class Physics(
    private val mode: Mode,
    private val gravity: Vector3f = Vector3f(0f, -10f, 0f)
) {

    enum class Mode {
        CLIENT, // follow the server (should predict?)
        SERVER  // generates the collisions
    }

    companion object {
        private const val DELTA_TO_BE_IN_GROUND = 0.01 // round error to go 0
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
        val shape = BoxShape(box.size.withOps { scale(0.5f) })
        val inertia = Vector3f()
        shape.calculateLocalInertia(box.mass, inertia)
        val constructionInfo = if (box.affectedByPhysics) {
            RigidBodyConstructionInfo(
                box.mass,
                DefaultMotionState(Transform(
                    Matrix4f(box.rotation, box.position.get(), 1f)
                )),
                shape,
                inertia
            )
        } else {
            RigidBodyConstructionInfo(
                box.mass,
                DefaultMotionState(Transform(
                    Matrix4f(box.rotation, box.position.get(), 1f)
                )),
                shape
            )
        }
        //constructionInfo.restitution = box.bounceMultiplier
        val body = RigidBody(constructionInfo)
        box.rigidBody = body
        body.friction = 0.7f
        world.addRigidBody(body)
    }

    fun unRegister(box: Box) {
        if (box !in boxes) return
        boxes -= box
        world.removeRigidBody(box.rigidBody!!)
    }

    fun simulate(delta: Double, updateObjs: Boolean) {
        world.stepSimulation(delta.toFloat() / 1000f)
        // now copy to the objects pos the real data
        if (updateObjs) {
            val transform = Transform()
            for (box in boxes) {
                box.syncing = true
                // copy to box the physics info
                val rigidBody = box.rigidBody!!
                rigidBody.motionState.getWorldTransform(transform)

                box.position.set(transform.origin)
                transform.getRotation(box.rotation)
                rigidBody.getLinearVelocity(box.linearVelocity)
                rigidBody.getAngularVelocity(box.angularVelocity)
                box.inGround = Math.abs(box.linearVelocity.y) < DELTA_TO_BE_IN_GROUND
                box.syncing = false
            }
        }
    }
}