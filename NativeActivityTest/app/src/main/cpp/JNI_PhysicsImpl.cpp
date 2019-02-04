#include "btBulletDynamicsCommon.h"
#include <jni.h>

// try to replace with that, as those names are painful as fuck
//#define PHYSICS_FUNC(f) Java_io_snower_game_client_BulletPhysicsNativeImpl_##

extern "C" {
JNIEXPORT jlong JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_createWorld(JNIEnv * env, jobject obj);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_deleteWorld(JNIEnv * env, jobject obj, jlong handle);
JNIEXPORT jlong JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_createBodyInWorld(JNIEnv * env, jobject obj, jlong worldHandle, jfloat mass, jfloat x, jfloat y, jfloat z, jfloat sx, jfloat sy, jfloat sz);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_deleteBodyFromWorld(JNIEnv * env, jobject obj, jlong worldHandle, jlong bodyHandle);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_simulate(JNIEnv * env, jobject obj, jlong worldHandle, jfloat step);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_getBodyOpenGLMatrix(JNIEnv * env, jobject obj, jlong bodyHandle, jfloatArray dst);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_getBodyHandleData(JNIEnv * env, jobject obj, jlong bodyHandle, jfloatArray dst);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_updateBodyWorldTransform(JNIEnv * env, jobject obj, jlong bodyHandle, jfloat x, jfloat y, jfloat z, jfloat q1, jfloat q2, jfloat q3, jfloat q4);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_updateBodyVelocity(JNIEnv * env, jobject obj, jlong bodyHandle, jfloat lX, jfloat lY, jfloat lZ, jfloat aX, jfloat aY, jfloat aZ);
};

/*
JNIEXPORT jlong JNICALL Java_io_snower_game_client_PhysicsImpl_createBoxHandle(
        JNIEnv * env,
        jobject obj,
        jfloat x,
        jfloat y,
        jfloat z) {
    auto* state = new btDefaultMotionState();
    auto* body = new btRigidBody(1.0f, state, new btBoxShape(btVector3(1.0f, 1.0f, 1.0f)));
    return (jlong)body;
}*/

JNIEXPORT jlong JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_createWorld
(JNIEnv * env, jobject obj) {
    auto* broadphase = new btDbvtBroadphase();
    auto* configuration = new btDefaultCollisionConfiguration();
    auto* dispatcher = new btCollisionDispatcher(configuration);
    auto* solver = new btSequentialImpulseConstraintSolver();
    auto* world = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, configuration);
    world->setGravity(btVector3(0.0f, -10.0f, 0.0f));
    return (jlong)world;
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_deleteWorld
(JNIEnv * env, jobject obj, jlong handle) {
    auto* world = (btDiscreteDynamicsWorld*) handle;
    auto* broadphase = world->getBroadphase();
    // TODO: delete configuration too. this leaks (although is tiny, on gamemode changes)
    auto* solver = world->getConstraintSolver();
    auto* dispatcher = world->getDispatcher();
    delete world;
    delete broadphase;
    delete solver;
    delete dispatcher;
}

JNIEXPORT jlong JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_createBodyInWorld
(JNIEnv * env, jobject obj, jlong worldHandle,
        jfloat mass,
        jfloat x, jfloat y, jfloat z,
        jfloat sx, jfloat sy, jfloat sz) {

    // create shape and calculate inertia
    btVector3 pos(x, y, z);
    btVector3 halfExtents(sx, sy, sz);
    halfExtents /= btScalar(2.0f);
    auto* shape = new btBoxShape(halfExtents);
    btVector3 inertia;
    shape->calculateLocalInertia(mass, inertia);

    // create world transform
    btTransform transform;
    transform.setIdentity();
    transform.setOrigin(pos);

    // create body, with their motionState and so
    auto* motionState = new btDefaultMotionState(transform);
    btRigidBody::btRigidBodyConstructionInfo rbInfo(mass, motionState, shape, inertia);
    auto* body = new btRigidBody(rbInfo);

    // add to world
    auto* world = (btDiscreteDynamicsWorld*) worldHandle;
    world->addRigidBody(body);

    return (jlong)body;
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_deleteBodyFromWorld
(JNIEnv * env, jobject obj, jlong worldHandle, jlong bodyHandle) {
    auto* world = (btDiscreteDynamicsWorld*) worldHandle;
    auto* body = (btRigidBody*) bodyHandle;
    world->removeRigidBody(body);
    delete body;
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_simulate
(JNIEnv * env, jobject obj, jlong worldHandle, jfloat step) {
    auto* world = (btDiscreteDynamicsWorld*) worldHandle;
    world->stepSimulation(step);
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_getBodyOpenGLMatrix
(JNIEnv * env, jobject obj, jlong bodyHandle, jfloatArray dst) {
    auto* body = (btRigidBody*) bodyHandle;
    auto* array = (jfloat*)env->GetPrimitiveArrayCritical(dst, NULL);
    body->getWorldTransform().getOpenGLMatrix(array);
    env->ReleasePrimitiveArrayCritical(dst, array, 0);
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_getBodyHandleData
(JNIEnv * env, jobject obj, jlong bodyHandle, jfloatArray dst) {
    // get array
    auto* body = (btRigidBody*) bodyHandle;
    auto* array = (jfloat*)env->GetPrimitiveArrayCritical(dst, NULL);

    // copy bullet data to the array
    btTransform t = body->getWorldTransform();
    btVector3 origin = t.getOrigin();
    array[0] = origin.getX();
    array[1] = origin.getY();
    array[2] = origin.getZ();

    btQuaternion quaterion = t.getRotation();
    array[3] = quaterion.getX();
    array[4] = quaterion.getY();
    array[5] = quaterion.getZ();
    array[6] = quaterion.getW();

    btVector3 linearVelocity = body->getLinearVelocity();
    array[7] = linearVelocity.getX();
    array[8] = linearVelocity.getY();
    array[9] = linearVelocity.getZ();

    btVector3 angularVelocity = body->getAngularVelocity();
    array[10] = angularVelocity.getX();
    array[11] = angularVelocity.getY();
    array[12] = angularVelocity.getZ();

    // release
    env->ReleasePrimitiveArrayCritical(dst, array, 0);
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_updateBodyWorldTransform
(JNIEnv * env, jobject obj, jlong bodyHandle,
        jfloat x, jfloat y, jfloat z,
        jfloat q1, jfloat q2, jfloat q3, jfloat q4) {
    auto* body = (btRigidBody*) bodyHandle;
    body->getWorldTransform().setOrigin(btVector3(x, y, z));
    body->getWorldTransform().setRotation(btQuaternion(q1, q2, q3, q4));
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_updateBodyVelocity
(JNIEnv * env, jobject obj, jlong bodyHandle,
        jfloat lX, jfloat lY, jfloat lZ,
        jfloat aX, jfloat aY, jfloat aZ) {
    auto* body = (btRigidBody*) bodyHandle;
    btVector3 linearVelocity(lX, lY, lZ);
    btVector3 angularVelocity(aX, aY, aZ);
    if (!linearVelocity.fuzzyZero() || !angularVelocity.fuzzyZero()) {
        body->activate();
    }
    body->setLinearVelocity(btVector3(lX, lY, lZ));
    body->setAngularVelocity(btVector3(aX, aY, aZ));
}