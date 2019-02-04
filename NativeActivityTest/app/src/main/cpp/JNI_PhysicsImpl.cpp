#include "btBulletDynamicsCommon.h"
#include <jni.h>

extern "C" {
JNIEXPORT jlong JNICALL Java_io_snower_game_client_PhysicsImpl_createBoxHandle(JNIEnv * env, jobject obj, jfloat x, jfloat y, jfloat z);
};

JNIEXPORT jlong JNICALL Java_io_snower_game_client_PhysicsImpl_createBoxHandle(
        JNIEnv * env,
        jobject obj,
        jfloat x,
        jfloat y,
        jfloat z) {
    auto* state = new btDefaultMotionState();
    auto* body = new btRigidBody(1.0f, state, new btBoxShape(btVector3(1.0f, 1.0f, 1.0f)));
    return (jlong)body;
}

