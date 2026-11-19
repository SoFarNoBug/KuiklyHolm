/**
 * @file holm_xc_model_gl.cpp
 * @brief ADR 0019 · EGL/GLES Model 网格 Present（Lambert + orbit + depth/cull）
 * @date 2026/11/19
 * @author jlj
 */
#include "holm_xc_model_gl.h"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <string>
#include <vector>

namespace {

void SetErr(std::string *errCode, std::string *errMsg, const char *code, const char *msg)
{
    if (errCode != nullptr) {
        *errCode = code != nullptr ? code : "";
    }
    if (errMsg != nullptr) {
        *errMsg = msg != nullptr ? msg : "";
    }
}

bool MakeCurrent(HolmXcModelGl &gl)
{
    if (gl.display == EGL_NO_DISPLAY || gl.surface == EGL_NO_SURFACE || gl.context == EGL_NO_CONTEXT) {
        return false;
    }
    eglBindAPI(EGL_OPENGL_ES_API);
    return eglMakeCurrent(gl.display, gl.surface, gl.surface, gl.context) == EGL_TRUE;
}

GLuint CompileShader(GLenum type, const char *source)
{
    while (glGetError() != GL_NO_ERROR) {
    }
    GLuint shader = glCreateShader(type);
    if (shader == 0) {
        return 0;
    }
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    GLint status = GL_FALSE;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status == GL_FALSE) {
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

GLuint LinkProgram()
{
    static const char *kVert = R"(#version 300 es
layout(location = 0) in vec3 a_pos;
layout(location = 1) in vec3 a_nrm;
uniform mat4 u_mvp;
out vec3 v_nrm;
void main() {
  v_nrm = a_nrm;
  gl_Position = u_mvp * vec4(a_pos, 1.0);
}
)";
    static const char *kFrag = R"(#version 300 es
precision mediump float;
in vec3 v_nrm;
uniform vec4 u_color;
uniform vec3 u_lightDir;
uniform int u_lit;
uniform float u_lightIntensity;
out vec4 fragColor;
void main() {
  if (u_lit == 0) {
    fragColor = u_color;
  } else {
    float ndl = max(dot(normalize(v_nrm), normalize(u_lightDir)), 0.15);
    fragColor = vec4(u_color.rgb * ndl * u_lightIntensity, u_color.a);
  }
}
)";
    GLuint vs = CompileShader(GL_VERTEX_SHADER, kVert);
    GLuint fs = CompileShader(GL_FRAGMENT_SHADER, kFrag);
    if (vs == 0 || fs == 0) {
        if (vs != 0) {
            glDeleteShader(vs);
        }
        if (fs != 0) {
            glDeleteShader(fs);
        }
        return 0;
    }
    GLuint program = glCreateProgram();
    glAttachShader(program, vs);
    glAttachShader(program, fs);
    glLinkProgram(program);
    glDeleteShader(vs);
    glDeleteShader(fs);
    GLint status = GL_FALSE;
    glGetProgramiv(program, GL_LINK_STATUS, &status);
    if (status == GL_FALSE) {
        glDeleteProgram(program);
        return 0;
    }
    return program;
}

void MultiplyMM(float *result, const float *lhs, const float *rhs)
{
    for (int c = 0; c < 4; ++c) {
        for (int r = 0; r < 4; ++r) {
            result[c * 4 + r] = lhs[r] * rhs[c * 4] + lhs[4 + r] * rhs[c * 4 + 1] +
                                lhs[8 + r] * rhs[c * 4 + 2] + lhs[12 + r] * rhs[c * 4 + 3];
        }
    }
}

void SetLookAtM(float *rm, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ)
{
    // 与 HolmModelMeshUpload.setLookAtM 对齐：up=(0,1,0)，s = f × up
    float fx = centerX - eyeX;
    float fy = centerY - eyeY;
    float fz = centerZ - eyeZ;
    float rlf = 1.f / std::max(std::sqrt(fx * fx + fy * fy + fz * fz), 1e-8f);
    fx *= rlf;
    fy *= rlf;
    fz *= rlf;
    const float upX = 0.f;
    const float upY = 1.f;
    const float upZ = 0.f;
    float sx = fy * upZ - fz * upY;
    float sy = fz * upX - fx * upZ;
    float sz = fx * upY - fy * upX;
    float rls = 1.f / std::max(std::sqrt(sx * sx + sy * sy + sz * sz), 1e-8f);
    sx *= rls;
    sy *= rls;
    sz *= rls;
    float ux = sy * fz - sz * fy;
    float uy = sz * fx - sx * fz;
    float uz = sx * fy - sy * fx;
    rm[0] = sx;
    rm[1] = ux;
    rm[2] = -fx;
    rm[3] = 0.f;
    rm[4] = sy;
    rm[5] = uy;
    rm[6] = -fy;
    rm[7] = 0.f;
    rm[8] = sz;
    rm[9] = uz;
    rm[10] = -fz;
    rm[11] = 0.f;
    rm[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ);
    rm[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ);
    rm[14] = fx * eyeX + fy * eyeY + fz * eyeZ;
    rm[15] = 1.f;
}

void PerspectiveM(float *m, float fovyDeg, float aspect, float zNear, float zFar)
{
    float f = 1.f / std::tan((fovyDeg * (static_cast<float>(M_PI) / 180.f)) * 0.5f);
    float rangeInv = 1.f / (zNear - zFar);
    m[0] = f / aspect;
    m[1] = 0.f;
    m[2] = 0.f;
    m[3] = 0.f;
    m[4] = 0.f;
    m[5] = f;
    m[6] = 0.f;
    m[7] = 0.f;
    m[8] = 0.f;
    m[9] = 0.f;
    m[10] = (zFar + zNear) * rangeInv;
    m[11] = -1.f;
    m[12] = 0.f;
    m[13] = 0.f;
    m[14] = 2.f * zFar * zNear * rangeInv;
    m[15] = 0.f;
}

void BuildMvp(const HolmXcModelGl &gl, float aspect, float *out16)
{
    float view[16];
    float proj[16];
    float eyeDist = gl.radius * 3.2f;
    float cy = std::cos(gl.pitch);
    float eyeX = gl.centerX + eyeDist * cy * std::sin(gl.yaw);
    float eyeY = gl.centerY + eyeDist * std::sin(gl.pitch);
    float eyeZ = gl.centerZ + eyeDist * cy * std::cos(gl.yaw);
    SetLookAtM(view, eyeX, eyeY, eyeZ, gl.centerX, gl.centerY, gl.centerZ);
    float zNear = std::max(eyeDist * 0.05f, 0.01f);
    float zFar = std::max(eyeDist * 10.f, zNear * 100.f);
    PerspectiveM(proj, 45.f, std::max(aspect, 0.01f), zNear, zFar);
    MultiplyMM(out16, proj, view);
}

bool EnsureProgram(HolmXcModelGl &gl)
{
    if (gl.program != 0) {
        return true;
    }
    gl.program = LinkProgram();
    if (gl.program == 0) {
        return false;
    }
    gl.uMvp = glGetUniformLocation(gl.program, "u_mvp");
    gl.uColor = glGetUniformLocation(gl.program, "u_color");
    gl.uLightDir = glGetUniformLocation(gl.program, "u_lightDir");
    gl.uLit = glGetUniformLocation(gl.program, "u_lit");
    gl.uLightIntensity = glGetUniformLocation(gl.program, "u_lightIntensity");
    gl.aPos = 0;
    gl.aNrm = 1;
    return true;
}

} // namespace

bool HolmXcModelGlCreate(HolmXcModelGl &gl, void *nativeWindow, uint32_t width, uint32_t height,
                         std::string *errCode, std::string *errMsg)
{
    HolmXcModelGlDestroy(gl);
    if (nativeWindow == nullptr) {
        SetErr(errCode, errMsg, "BLOCKED", "OHNativeWindow null");
        return false;
    }

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        SetErr(errCode, errMsg, "NO_GLES", "eglGetDisplay failed");
        return false;
    }
    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    if (eglBindAPI(EGL_OPENGL_ES_API) != EGL_TRUE) {
        SetErr(errCode, errMsg, "NO_GLES", "eglBindAPI failed");
        return false;
    }
    EGLint major = 0;
    EGLint minor = 0;
    if (eglInitialize(display, &major, &minor) != EGL_TRUE) {
        SetErr(errCode, errMsg, "NO_GLES", "eglInitialize failed");
        return false;
    }

    const EGLint attribs[] = {EGL_SURFACE_TYPE,
                              EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
                              EGL_RENDERABLE_TYPE,
                              EGL_OPENGL_ES3_BIT,
                              EGL_RED_SIZE,
                              8,
                              EGL_GREEN_SIZE,
                              8,
                              EGL_BLUE_SIZE,
                              8,
                              EGL_ALPHA_SIZE,
                              8,
                              EGL_DEPTH_SIZE,
                              24,
                              EGL_NONE};
    EGLConfig config = nullptr;
    EGLint numConfigs = 0;
    if (eglChooseConfig(display, attribs, &config, 1, &numConfigs) != EGL_TRUE || numConfigs < 1) {
        const EGLint attribsEs2[] = {EGL_SURFACE_TYPE,
                                     EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
                                     EGL_RENDERABLE_TYPE,
                                     EGL_OPENGL_ES2_BIT,
                                     EGL_RED_SIZE,
                                     8,
                                     EGL_GREEN_SIZE,
                                     8,
                                     EGL_BLUE_SIZE,
                                     8,
                                     EGL_ALPHA_SIZE,
                                     8,
                                     EGL_DEPTH_SIZE,
                                     24,
                                     EGL_NONE};
        if (eglChooseConfig(display, attribsEs2, &config, 1, &numConfigs) != EGL_TRUE || numConfigs < 1) {
            SetErr(errCode, errMsg, "NO_GLES", "eglChooseConfig failed");
            return false;
        }
    }

    const EGLint pbufAttr[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
    EGLSurface pbuffer = eglCreatePbufferSurface(display, config, pbufAttr);
    if (pbuffer == EGL_NO_SURFACE) {
        SetErr(errCode, errMsg, "NO_GLES", "eglCreatePbufferSurface failed");
        return false;
    }

    EGLint ctxAttribs3[] = {EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE};
    EGLContext context = eglCreateContext(display, config, EGL_NO_CONTEXT, ctxAttribs3);
    if (context == EGL_NO_CONTEXT) {
        EGLint ctxAttribs2[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
        context = eglCreateContext(display, config, EGL_NO_CONTEXT, ctxAttribs2);
    }
    if (context == EGL_NO_CONTEXT) {
        eglDestroySurface(display, pbuffer);
        SetErr(errCode, errMsg, "NO_GLES", "eglCreateContext failed");
        return false;
    }
    if (eglMakeCurrent(display, pbuffer, pbuffer, context) != EGL_TRUE) {
        eglDestroyContext(display, context);
        eglDestroySurface(display, pbuffer);
        SetErr(errCode, errMsg, "NO_GLES", "eglMakeCurrent(pbuffer) failed");
        return false;
    }

    EGLSurface surface = eglCreateWindowSurface(
        display, config, reinterpret_cast<EGLNativeWindowType>(nativeWindow), nullptr);
    if (surface == EGL_NO_SURFACE) {
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroyContext(display, context);
        eglDestroySurface(display, pbuffer);
        SetErr(errCode, errMsg, "NO_GLES", "eglCreateWindowSurface failed");
        return false;
    }

    gl.display = display;
    gl.surface = surface;
    gl.context = context;
    gl.config = config;
    gl.nativeWindow = nativeWindow;
    gl.viewW = static_cast<int32_t>(std::max(1u, width));
    gl.viewH = static_cast<int32_t>(std::max(1u, height));
    eglDestroySurface(display, pbuffer);

    if (!MakeCurrent(gl)) {
        HolmXcModelGlDestroy(gl);
        SetErr(errCode, errMsg, "NO_GLES", "eglMakeCurrent(window) failed");
        return false;
    }
    if (!EnsureProgram(gl)) {
        HolmXcModelGlDestroy(gl);
        SetErr(errCode, errMsg, "NO_GLES", "Model GLES program link failed");
        return false;
    }
    glViewport(0, 0, gl.viewW, gl.viewH);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    glFrontFace(GL_CCW);
    gl.ready = true;
    return true;
}

void HolmXcModelGlDestroy(HolmXcModelGl &gl)
{
    if (gl.display != EGL_NO_DISPLAY) {
        if (gl.surface != EGL_NO_SURFACE && gl.context != EGL_NO_CONTEXT) {
            eglMakeCurrent(gl.display, gl.surface, gl.surface, gl.context);
        }
        if (gl.vbo != 0) {
            glDeleteBuffers(1, &gl.vbo);
            gl.vbo = 0;
        }
        if (gl.ibo != 0) {
            glDeleteBuffers(1, &gl.ibo);
            gl.ibo = 0;
        }
        if (gl.program != 0) {
            glDeleteProgram(gl.program);
            gl.program = 0;
        }
        eglMakeCurrent(gl.display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (gl.context != EGL_NO_CONTEXT) {
            eglDestroyContext(gl.display, gl.context);
            gl.context = EGL_NO_CONTEXT;
        }
        if (gl.surface != EGL_NO_SURFACE) {
            eglDestroySurface(gl.display, gl.surface);
            gl.surface = EGL_NO_SURFACE;
        }
        eglTerminate(gl.display);
        gl.display = EGL_NO_DISPLAY;
    }
    gl.config = nullptr;
    gl.nativeWindow = nullptr;
    gl.indexCount = 0;
    gl.ready = false;
}

bool HolmXcModelGlResize(HolmXcModelGl &gl, uint32_t width, uint32_t height, std::string *errCode,
                         std::string *errMsg)
{
    if (!gl.ready) {
        SetErr(errCode, errMsg, "NO_GLES", "Model GL not ready");
        return false;
    }
    gl.viewW = static_cast<int32_t>(std::max(1u, width));
    gl.viewH = static_cast<int32_t>(std::max(1u, height));
    if (!MakeCurrent(gl)) {
        SetErr(errCode, errMsg, "NO_GLES", "eglMakeCurrent resize failed");
        return false;
    }
    glViewport(0, 0, gl.viewW, gl.viewH);
    return true;
}

void HolmXcModelGlSetMeshes(HolmXcModelGl &gl, const float *verts, int vertFloats, const int32_t *indices,
                            int indexCount, float cx, float cy, float cz, float radius, float cr, float cg,
                            float cb, float ca)
{
    if (!gl.ready || !MakeCurrent(gl)) {
        return;
    }
    if (!EnsureProgram(gl)) {
        return;
    }
    gl.centerX = cx;
    gl.centerY = cy;
    gl.centerZ = cz;
    gl.radius = std::max(radius, 0.001f);
    gl.colorR = cr;
    gl.colorG = cg;
    gl.colorB = cb;
    gl.colorA = ca;

    if (gl.vbo == 0) {
        glGenBuffers(1, &gl.vbo);
    }
    if (gl.ibo == 0) {
        glGenBuffers(1, &gl.ibo);
    }
    if (verts == nullptr || indices == nullptr || vertFloats < 6 || indexCount < 3) {
        gl.indexCount = 0;
        return;
    }
    glBindBuffer(GL_ARRAY_BUFFER, gl.vbo);
    glBufferData(GL_ARRAY_BUFFER, static_cast<GLsizeiptr>(vertFloats * static_cast<int>(sizeof(float))), verts,
                 GL_STATIC_DRAW);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, gl.ibo);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, static_cast<GLsizeiptr>(indexCount * static_cast<int>(sizeof(int32_t))),
                 indices, GL_STATIC_DRAW);
    gl.indexCount = indexCount;
}

void HolmXcModelGlSetOrbit(HolmXcModelGl &gl, float yaw, float pitch, bool orbitEnabled)
{
    gl.yaw = yaw;
    gl.pitch = pitch;
    gl.orbitEnabled = orbitEnabled;
}

void HolmXcModelGlSetLit(HolmXcModelGl &gl, bool lit, float lx, float ly, float lz, float intensity)
{
    gl.lit = lit;
    gl.lightX = lx;
    gl.lightY = ly;
    gl.lightZ = lz;
    gl.lightIntensity = intensity;
}

void HolmXcModelGlDrawFrame(HolmXcModelGl &gl, float aspect)
{
    if (!gl.ready || !MakeCurrent(gl) || gl.program == 0) {
        return;
    }
    glViewport(0, 0, gl.viewW, gl.viewH);
    glClearColor(0.02f, 0.08f, 0.18f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    if (gl.indexCount <= 0 || gl.vbo == 0 || gl.ibo == 0) {
        eglSwapBuffers(gl.display, gl.surface);
        return;
    }

    float mvp[16];
    BuildMvp(gl, aspect, mvp);
    glUseProgram(gl.program);
    glUniformMatrix4fv(gl.uMvp, 1, GL_FALSE, mvp);
    glUniform4f(gl.uColor, gl.colorR, gl.colorG, gl.colorB, gl.colorA);
    glUniform3f(gl.uLightDir, gl.lightX, gl.lightY, gl.lightZ);
    glUniform1i(gl.uLit, gl.lit ? 1 : 0);
    glUniform1f(gl.uLightIntensity, gl.lightIntensity);

    glBindBuffer(GL_ARRAY_BUFFER, gl.vbo);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, gl.ibo);
    glEnableVertexAttribArray(static_cast<GLuint>(gl.aPos));
    glVertexAttribPointer(static_cast<GLuint>(gl.aPos), 3, GL_FLOAT, GL_FALSE, 6 * sizeof(float),
                          reinterpret_cast<const void *>(0));
    glEnableVertexAttribArray(static_cast<GLuint>(gl.aNrm));
    glVertexAttribPointer(static_cast<GLuint>(gl.aNrm), 3, GL_FLOAT, GL_FALSE, 6 * sizeof(float),
                          reinterpret_cast<const void *>(3 * sizeof(float)));
    glDrawElements(GL_TRIANGLES, gl.indexCount, GL_UNSIGNED_INT, nullptr);
    glDisableVertexAttribArray(static_cast<GLuint>(gl.aPos));
    glDisableVertexAttribArray(static_cast<GLuint>(gl.aNrm));
    eglSwapBuffers(gl.display, gl.surface);
}

bool HolmXcModelGlHasMesh(const HolmXcModelGl &gl)
{
    return gl.ready && gl.indexCount > 0 && gl.program != 0;
}
