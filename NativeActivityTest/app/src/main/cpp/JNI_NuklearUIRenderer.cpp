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

float
nk_sina(float x)
{
    float sin = 0.f;

    if (x < -3.14159265f)
        x += 6.28318531f;
    else
    if (x >  3.14159265f)
        x -= 6.28318531f;

    if (x < 0)
    {
        sin = 1.27323954f * x + .405284735f * x * x;

        if (sin < 0)
            sin = .225f * (sin *-sin - sin) + sin;
        else
            sin = .225f * (sin * sin - sin) + sin;
    }
    else
    {
        sin = 1.27323954f * x - 0.405284735f * x * x;

        if (sin < 0)
            sin = .225f * (sin *-sin - sin) + sin;
        else
            sin = .225f * (sin * sin - sin) + sin;
    }
    return sin;
}

float
nk_cosa(float x)
{
    return nk_sina(x + 1.57079632f);
}

#define NK_SIN nk_sina
#define NK_COS nk_cosa

#include "nuklear.h"
#include "nk_gles2_impl.h"
#define MAX_VERTEX_MEMORY (512 * 128)
#define MAX_ELEMENT_MEMORY (128 * 128)

#define  LOG_TAG    "snower-jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

struct nk_context* ctx = nullptr;

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

static nk_color java2nuklear(jint col) {
    auto rgba = static_cast<nk_uint>(col);
    // for some reason nuklear / openGL displays it as ARGB instead of RGBA
    return nk_rgba_u32(rgba);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_snower_game_client_NuklearUIRenderer_createNuklearContext(JNIEnv * env, jobject obj, jobject fontPointer) {
    if (ctx != nullptr) {
        LOGI("Context already initialized. Return it.");
    } else {
        ctx = nk_gles_init();// isn't it deleted when going out of scope?
        struct nk_font_atlas *atlas;
        nk_gles_font_stash_begin(&atlas);
        auto* fontPtr = (jbyte*) env->GetDirectBufferAddress(fontPointer);
        nk_font* font = nullptr;
        if (fontPtr != nullptr) {
            jlong size = env->GetDirectBufferCapacity(fontPointer);
            font = nk_font_atlas_add_from_memory(atlas, fontPtr, static_cast<nk_size>(size), 20, nullptr);
            LOGI("Passed a font pointer to createNuklearContext");
        } else {
            LOGI("Use default font in createNuklearContext");
        }
        nk_gles_font_stash_end();
        if (font != nullptr) {
            nk_style_set_font(ctx, &font->handle);
        }
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
    nk_style_push_color(ctx, &ctx->style.window.background, java2nuklear(background));
    nk_style_push_style_item(ctx, &ctx->style.window.fixed_background, nk_style_item_color(java2nuklear(background)));
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
    nk_stroke_circle(cmdBuffer, nk_rect(x, y, diameter, diameter), thickness, java2nuklear(color));
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_fillCircle
(JNIEnv* env, jobject obj, jfloat x, jfloat y, jfloat diameter, jint color) {
    struct nk_command_buffer* cmdBuffer = nk_window_get_canvas(ctx);
    nk_fill_circle(cmdBuffer, nk_rect(x, y, diameter, diameter), java2nuklear(color));
}

extern "C"
JNIEXPORT void JNICALL
Java_io_snower_game_client_NuklearUIRenderer_progress
(JNIEnv* env, jobject obj, jint current, jint max, jint color, jint background) {
    auto prog = (size_t)current;
    ctx->style.progress.normal.data.color = nk_rgba_u32(0x99010101);
    ctx->style.progress.cursor_normal.data.color = java2nuklear(color);
    ctx->style.progress.border = 5.0f;
    ctx->style.progress.rounding = 20.0f;
    ctx->style.progress.cursor_border = 15.0f;
    ctx->style.progress.cursor_rounding = 15.0f;
    nk_progress(ctx, &prog, (nk_size)(max), nk_false);
}