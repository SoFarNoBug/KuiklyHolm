/**
 * @file holm_xc_lite_gl.h
 * @brief ADR 0011 · 同 OHNativeWindow 上 EGL/GLES2 Shadertoy Lite
 * @date 2026-11-15
 * @author Auto
 */
#ifndef HOLM_XC_LITE_GL_H
#define HOLM_XC_LITE_GL_H

#include <GLES2/gl2.h>
#include <EGL/egl.h>

#include <cstdint>
#include <string>

struct HolmXcLiteGl {
    EGLDisplay display = EGL_NO_DISPLAY;
    EGLSurface surface = EGL_NO_SURFACE;
    EGLContext context = EGL_NO_CONTEXT;
    EGLConfig config = nullptr;
    void *nativeWindow = nullptr;

    GLuint program = 0;
    GLint posLoc = -1;
    GLint timeLoc = -1;
    GLint resolutionLoc = -1;
    GLint liquidBoostLoc = -1;
    GLint liquidBendLoc = -1;

    int32_t viewW = 1;
    int32_t viewH = 1;
    float animTimeSec = 0.f;
    float liquidBoost = 1.35f;
    float liquidBend = 0.05f;
    std::string fragmentBody;
    bool ready = false;
};

/** 错误码写入 errCode/errMsg（可空）；成功返回 true。 */
bool HolmXcLiteGlCreate(HolmXcLiteGl &gl, void *nativeWindow, uint32_t width, uint32_t height,
                        std::string *errCode, std::string *errMsg);

void HolmXcLiteGlDestroy(HolmXcLiteGl &gl);

bool HolmXcLiteGlResize(HolmXcLiteGl &gl, uint32_t width, uint32_t height, std::string *errCode,
                        std::string *errMsg);

bool HolmXcLiteGlSetFragmentBody(HolmXcLiteGl &gl, const std::string &body, std::string *errCode,
                                 std::string *errMsg);

void HolmXcLiteGlSetLiquid(HolmXcLiteGl &gl, float boost, float bend);

void HolmXcLiteGlDrawFrame(HolmXcLiteGl &gl, float dtSec);

bool HolmXcLiteGlHasProgram(const HolmXcLiteGl &gl);

#endif /* HOLM_XC_LITE_GL_H */
