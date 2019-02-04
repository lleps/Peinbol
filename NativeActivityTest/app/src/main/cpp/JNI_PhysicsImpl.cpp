#include "btBulletDynamicsCommon.h"
#include <jni.h>

// try to replace with that, as those names are painful as fuck
//#define PHYSICS_FUNC(f) Java_io_snower_game_client_BulletPhysicsNativeImpl_##

extern "C" {
JNIEXPORT jlong JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_createWorld(JNIEnv * env, jobject obj);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_deleteWorld(JNIEnv * env, jobject obj, jlong handle);
JNIEXPORT jlong JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_createBodyInWorld(JNIEnv * env, jobject obj, jlong worldHandle, jfloat x, jfloat y, jfloat z);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_deleteBodyFromWorld(JNIEnv * env, jobject obj, jlong worldHandle, jlong bodyHandle);
JNIEXPORT void JNICALL Java_io_snower_game_client_BulletPhysicsNativeImpl_simulate(JNIEnv * env, jobject obj, jfloat step);
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
    return 0;
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_deleteWorld
(JNIEnv * env, jobject obj, jlong handle) {

}

JNIEXPORT jlong JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_createBodyInWorld
(JNIEnv * env, jobject obj, jlong worldHandle, jfloat x, jfloat y, jfloat z) {
    return 0;
}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_deleteBodyFromWorld
(JNIEnv * env, jobject obj, jlong worldHandle, jlong bodyHandle) {

}
JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_simulate
(JNIEnv * env, jobject obj, jfloat step) {

}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_getBodyOpenGLMatrix
(JNIEnv * env, jobject obj, jlong bodyHandle, jfloatArray dst) {

}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_getBodyHandleData
(JNIEnv * env, jobject obj, jlong bodyHandle, jfloatArray dst) {

}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_updateBodyWorldTransform
(JNIEnv * env, jobject obj, jlong bodyHandle,
        jfloat x, jfloat y, jfloat z,
        jfloat q1, jfloat q2, jfloat q3, jfloat q4) {

}

JNIEXPORT void JNICALL
Java_io_snower_game_client_BulletPhysicsNativeImpl_updateBodyVelocity
(JNIEnv * env, jobject obj, jlong bodyHandle,
        jfloat lX, jfloat lY, jfloat lZ,
        jfloat aX, jfloat aY, jfloat aZ) {

}
