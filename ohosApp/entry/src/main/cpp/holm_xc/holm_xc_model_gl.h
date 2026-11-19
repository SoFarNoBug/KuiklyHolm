/**
 * @file holm_xc_model_gl.h
 * @brief ADR 0019 · 同 OHNativeWindow 上 EGL/GLES Model 网格 Present
 * @date 2026/11/19
 * @author jlj
 */
#ifndef HOLM_XC_MODEL_GL_H
#define HOLM_XC_MODEL_GL_H

#include <GLES3/gl3.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>

#include <cstdint>
#include <string>

struct HolmXcModelGl {
    EGLDisplay display = EGL_NO_DISPLAY;
    EGLSurface surface = EGL_NO_SURFACE;
    EGLContext context = EGL_NO_CONTEXT;
    EGLConfig config = nullptr;
    void *nativeWindow = nullptr;

    GLuint program = 0;
    GLint uMvp = -1;
    GLint uColor = -1;
    GLint uLightDir = -1;
    GLint uLit = -1;
    GLint uLightIntensity = -1;
    GLint aPos = -1;
    GLint aNrm = -1;

    GLuint vbo = 0;
    GLuint ibo = 0;
    int32_t indexCount = 0;

    float centerX = 0.f;
    float centerY = 0.f;
    float centerZ = 0.f;
    float radius = 1.f;
    float colorR = 0.85f;
    float colorG = 0.55f;
    float colorB = 0.25f;
    float colorA = 1.f;
    float yaw = 0.4f;
    float pitch = 0.3f;
    bool orbitEnabled = true;
    bool lit = false;
    float lightX = 0.3f;
    float lightY = 0.8f;
    float lightZ = 0.4f;
    float lightIntensity = 1.f;

    int32_t viewW = 1;
    int32_t viewH = 1;
    bool ready = false;
};

/** 错误码写入 errCode/errMsg（可空）；成功返回 true。 */
bool HolmXcModelGlCreate(HolmXcModelGl &gl, void *nativeWindow, uint32_t width, uint32_t height,
                         std::string *errCode, std::string *errMsg);

void HolmXcModelGlDestroy(HolmXcModelGl &gl);

bool HolmXcModelGlResize(HolmXcModelGl &gl, uint32_t width, uint32_t height, std::string *errCode,
                         std::string *errMsg);

void HolmXcModelGlSetMeshes(HolmXcModelGl &gl, const float *verts, int vertFloats, const int32_t *indices,
                            int indexCount, float cx, float cy, float cz, float radius, float cr, float cg,
                            float cb, float ca);

void HolmXcModelGlSetOrbit(HolmXcModelGl &gl, float yaw, float pitch, bool orbitEnabled);

void HolmXcModelGlSetLit(HolmXcModelGl &gl, bool lit, float lx, float ly, float lz, float intensity);

/** 构建 MVP（对齐 HolmModelMeshUpload.mvp）并绘制；清屏 #051A47。 */
void HolmXcModelGlDrawFrame(HolmXcModelGl &gl, float aspect);

bool HolmXcModelGlHasMesh(const HolmXcModelGl &gl);

#endif /* HOLM_XC_MODEL_GL_H */
