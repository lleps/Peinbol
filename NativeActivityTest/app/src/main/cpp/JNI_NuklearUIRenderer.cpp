#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include <cstdlib>

#define NK_INCLUDE_FIXED_TYPES
#define NK_INCLUDE_STANDARD_IO
#define NK_INCLUDE_STANDARD_VARARGS
#define NK_INCLUDE_DEFAULT_ALLOCATOR
#define NK_INCLUDE_VERTEX_BUFFER_OUTPUT
#define NK_INCLUDE_FONT_BAKING
#define NK_INCLUDE_DEFAULT_FONT
#define NK_IMPLEMENTATION
#define NK_GLES2_IMPLEMENTATION
#include "nuklear.h"
#include "nk_gles2_impl.h"
#define MAX_VERTEX_MEMORY 512 * 1024
#define MAX_ELEMENT_MEMORY 128 * 1024

#define  LOG_TAG    "libgl2jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

struct nk_context* ctx = NULL;

void renderStandardExample(int width, int height) {
    /* GUI */
    if (nk_begin(ctx, "Demo", nk_rect(50, 50, 200, 200),
                 NK_WINDOW_BORDER|NK_WINDOW_MOVABLE|NK_WINDOW_SCALABLE|
                 NK_WINDOW_CLOSABLE|NK_WINDOW_MINIMIZABLE|NK_WINDOW_TITLE))
    {
        nk_menubar_begin(ctx);
        nk_layout_row_begin(ctx, NK_STATIC, 25, 2);
        nk_layout_row_push(ctx, 45);
        if (nk_menu_begin_label(ctx, "FILE", NK_TEXT_LEFT, nk_vec2(120, 200))) {
            nk_layout_row_dynamic(ctx, 30, 1);
            nk_menu_item_label(ctx, "OPEN", NK_TEXT_LEFT);
            nk_menu_item_label(ctx, "CLOSE", NK_TEXT_LEFT);
            nk_menu_end(ctx);
        }
        nk_layout_row_push(ctx, 45);
        if (nk_menu_begin_label(ctx, "EDIT", NK_TEXT_LEFT, nk_vec2(120, 200))) {
            nk_layout_row_dynamic(ctx, 30, 1);
            nk_menu_item_label(ctx, "COPY", NK_TEXT_LEFT);
            nk_menu_item_label(ctx, "CUT", NK_TEXT_LEFT);
            nk_menu_item_label(ctx, "PASTE", NK_TEXT_LEFT);
            nk_menu_end(ctx);
        }
        nk_layout_row_end(ctx);
        nk_menubar_end(ctx);

        enum {EASY, HARD};
        static int op = EASY;
        static int property = 20;
        nk_layout_row_static(ctx, 30, 80, 1);
        if (nk_button_label(ctx, "button"))
            fprintf(stdout, "button pressed\n");
        nk_layout_row_dynamic(ctx, 30, 2);
        if (nk_option_label(ctx, "easy", op == EASY)) op = EASY;
        if (nk_option_label(ctx, "hard", op == HARD)) op = HARD;
        nk_layout_row_dynamic(ctx, 25, 1);
        nk_property_int(ctx, "Compression:", 0, &property, 100, 10, 1);
        nk_layout_row_dynamic(ctx, 25, 1);
        size_t prog = 50;

        nk_progress(ctx, &prog, 100, nk_false);
    }
    nk_end(ctx);

    /* -------------- EXAMPLES ---------------- */
    /*calculator(ctx);*/
    /*overview(ctx);*/
    /*node_editor(ctx);*/
    /* ----------------------------------------- */

}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_snower_game_client_NuklearUIRenderer_createNuklearContext(JNIEnv * env, jobject obj) {
    if (ctx != NULL) {
        LOGI("Context already initialized. Return it.");
    } else {
        ctx = nk_gles_init();// isn't it deleted when going out of scope?
        struct nk_font_atlas *atlas;
        nk_gles_font_stash_begin(&atlas);
        // TODO: add a better font
        /*struct nk_font *droid = nk_font_atlas_add_from_file(atlas, "../../../extra_font/DroidSans.ttf", 14, 0);*/
        /*struct nk_font *roboto = nk_font_atlas_add_from_file(atlas, "../../../extra_font/Roboto-Regular.ttf", 16, 0);*/
        /*struct nk_font *future = nk_font_atlas_add_from_file(atlas, "../../../extra_font/kenvector_future_thin.ttf", 13, 0);*/
        /*struct nk_font *clean = nk_font_atlas_add_from_file(atlas, "../../../extra_font/ProggyClean.ttf", 12, 0);*/
        /*struct nk_font *tiny = nk_font_atlas_add_from_file(atlas, "../../../extra_font/ProggyTiny.ttf", 10, 0);*/
        /*struct nk_font *cousine = nk_font_atlas_add_from_file(atlas, "../../../extra_font/Cousine-Regular.ttf", 13, 0);*/
        nk_gles_font_stash_end();
    }
    return (long)ctx;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_setCurrentNuklearContext(JNIEnv * env, jobject obj, jlong context) {
    // ignored for now, always use ctx.
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_drawNuklearOutput(JNIEnv * env, jobject obj, jint width, jint height) {
    //LOGI("drawNuklearOutput()\n");
    // for now, just draw standard code like in the example.
    //renderStandardExample(width, height);
    nk_gles_render(NK_ANTI_ALIASING_ON, MAX_VERTEX_MEMORY, MAX_ELEMENT_MEMORY, width, height);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_snower_game_client_NuklearUIRenderer_begin
(JNIEnv * env, jobject obj, jstring text, jfloat x, jfloat y, jfloat width, jfloat height, jint background, jint flags) {
    nk_style_push_color(ctx, &ctx->style.window.background, nk_rgba_u32((nk_uint)background));
    nk_style_push_style_item(ctx, &ctx->style.window.fixed_background, nk_style_item_color(nk_rgba_u32((nk_uint)background)));
    const char* c_text = env->GetStringUTFChars(text, nullptr);
    int result = nk_begin(ctx, c_text, nk_rect(x, y, width, height), (nk_flags)flags);
    env->ReleaseStringUTFChars(text, c_text);
    return (jboolean)(result != 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_end(JNIEnv * env, jobject obj) {
    nk_end(ctx);
    nk_style_pop_style_item(ctx);
    nk_style_pop_color(ctx);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_layoutRowDynamic(JNIEnv * env, jobject obj, jfloat height, jint columns) {
    nk_layout_row_dynamic(ctx, height, columns);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_layoutRowStatic
(JNIEnv * env, jobject obj, jfloat height, jfloat width, jint columns) {
    nk_layout_row_static(ctx, height, (int)width, columns);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_label
(JNIEnv * env, jobject obj, jstring label, jint align) {
    const char* c_label = env->GetStringUTFChars(label, nullptr);
    nk_label(ctx, c_label, align);
    env->ReleaseStringUTFChars(label, c_label);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_strokeCircle
(JNIEnv* env, jobject obj, jfloat x, jfloat y, jfloat diameter, jfloat thickness, jint color) {
    struct nk_command_buffer* cmdBuffer = nk_window_get_canvas(ctx);
    nk_stroke_circle(cmdBuffer, nk_rect(x, y, diameter, diameter), thickness, nk_rgba_u32((nk_uint)color));
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_fillCircle
(JNIEnv* env, jobject obj, jfloat x, jfloat y, jfloat diameter, jint color) {
    struct nk_command_buffer* cmdBuffer = nk_window_get_canvas(ctx);
    nk_fill_circle(cmdBuffer, nk_rect(x, y, diameter, diameter), nk_rgba_u32(static_cast<nk_uint>(color)));
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_progress
(JNIEnv* env, jobject obj, jint current, jint max, jint color, jint background) {
    auto prog = (size_t)current;
    ctx->style.progress.cursor_normal.data.color = nk_rgba_u32((nk_uint)color);
    nk_progress(ctx, &prog, (nk_size)(max), nk_false);
}