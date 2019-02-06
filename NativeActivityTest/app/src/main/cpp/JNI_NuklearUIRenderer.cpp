#include <jni.h>
#include <stdio.h>
#include <android/log.h>

#define  LOG_TAG    "libgl2jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

extern "C"
JNIEXPORT jlong JNICALL
Java_io_snower_game_client_NuklearUIRenderer_createNuklearContext(JNIEnv * env, jobject obj) {
    LOGI("createNuklearContext()\n");
    return 0L;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_setCurrentNuklearContext(JNIEnv * env, jobject obj, jlong context) {
    LOGI("setCurrentNuklearContext()\n");
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_drawNuklearOutput(JNIEnv * env, jobject obj, jint width, jint height) {
    LOGI("drawNuklearOutput()\n");
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_begin
(JNIEnv * env, jobject obj, jstring text, jfloat x, jfloat y, jfloat width, jfloat height, jint background, jint flags) {

}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_end(JNIEnv * env, jobject obj) {

}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_layoutRowDynamic(JNIEnv * env, jobject obj, jfloat height, jint columns) {

}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_layoutRowStatic
(JNIEnv * env, jobject obj, jfloat height, jfloat width, jint columns) {

}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_label
(JNIEnv * env, jobject obj, jstring label, jint align) {

}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_strokeCircle
(JNIEnv* env, jobject obj, jfloat x, jfloat y, jfloat diameter, jfloat thickness, jint color) {

}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_fillCircle
(JNIEnv* env, jobject obj, jfloat x, jfloat y, jfloat diameter, jint color) {

}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_progress
(JNIEnv* env, jobject obj, jint current, jint max, jint color, jint background) {

}