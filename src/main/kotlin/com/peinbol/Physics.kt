package com.peinbol

import com.bulletphysics.collision.broadphase.CollisionFilterGroups
import com.bulletphysics.collision.broadphase.DbvtBroadphase
import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.dispatch.CollisionFlags
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration
import com.bulletphysics.collision.dispatch.PairCachingGhostObject
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.DynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.dynamics.RigidBodyConstructionInfo
import com.bulletphysics.dynamics.character.KinematicCharacterController
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.linearmath.DefaultMotionState
import com.bulletphysics.linearmath.Transform
import javax.vecmath.Matrix4f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f
import kotlin.experimental.or

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
        world.setGravity(Vector3f(0f, -20f, 0f))
        //val k = KinematicCharacterController(PairCachingGhostObject(), BoxShape(Vector3f(1f,1f,1f)), 0.5f)
        //world.addAction(k)
    }

    fun register(box: Box) {
        if (box in boxes) return
        boxes += box
        /*if (box.isCharacter) {
            val ghostObject = PairCachingGhostObject()
            ghostObject.setWorldTransform(Transform(Matrix4f(Quat4f(0f, 0f, 0f, 1f), box.position.get(), 1f)))
            val capsule = CapsuleShape(box.size.x/2f, box.size.y)
            ghostObject.collisionShape = capsule
            ghostObject.collisionFlags = CollisionFlags.CHARACTER_OBJECT
            val character = KinematicCharacterController(ghostObject, capsule, 0.6f)
            world.addCollisionObject(
                ghostObject,
                CollisionFilterGroups.CHARACTER_FILTER,
                CollisionFilterGroups.DEFAULT_FILTER or CollisionFilterGroups.STATIC_FILTER
            )
            world.addAction(character)
        }*/
        val shape = if (box.isSphere) {
            SphereShape(box.size.x)
        } else if (box.isCharacter) {
            CapsuleShape(box.size.x/2f, box.size.y)
        } else {
            BoxShape(box.size.withOps { scale(0.5f) })
        }

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
        if (box.isSphere) {
            body.ccdMotionThreshold = 0.2f
        }
        box.rigidBody = body
        body.friction = 0.7f
        world.addRigidBody(body)
    }

    fun unRegister(box: Box) {
        if (box !in boxes) return
        boxes -= box
        world.removeRigidBody(box.rigidBody!!)
        box.rigidBody?.destroy()
        box.rigidBody = null
    }

    fun simulate(delta: Double, updateObjs: Boolean, updateId: Int = -1) {
        world.stepSimulation(delta.toFloat() / 1000f)
        // now copy to the objects pos the real data
        val transform = Transform()
        for (box in boxes) {
            if (updateObjs || box.id == updateId) {
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