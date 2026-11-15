/**
 * @file holm_xc_lite_gl.cpp
 * @brief ADR 0011 · EGL/GLES2 Lite 实现
 * @date 2026-11-15
 * @author Auto
 */
#include "holm_xc_lite_gl.h"
#include "holm_xc_shadertoy.h"

#include <algorithm>
#include <cstring>

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

bool MakeCurrent(HolmXcLiteGl &gl)
{
    if (gl.display == EGL_NO_DISPLAY || gl.surface == EGL_NO_SURFACE || gl.context == EGL_NO_CONTEXT) {
        return false;
    }
    return eglMakeCurrent(gl.display, gl.surface, gl.surface, gl.context) == EGL_TRUE;
}

GLuint CompileShader(GLenum type, const char *source, std::string *errCode, std::string *errMsg)
{
    GLuint shader = glCreateShader(type);
    if (shader == 0) {
        SetErr(errCode, errMsg, "SHADER_COMPILE", "glCreateShader failed");
        return 0;
    }
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    GLint status = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (status == GL_FALSE) {
        GLint logLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLen);
        std::string log = "compile failed";
        if (logLen > 1) {
            log.assign(static_cast<size_t>(logLen), '\0');
            glGetShaderInfoLog(shader, logLen, nullptr, log.data());
            while (!log.empty() && (log.back() == '\0' || log.back() == '\n')) {
                log.pop_back();
            }
        }
        glDeleteShader(shader);
        SetErr(errCode, errMsg, "SHADER_COMPILE", log.c_str());
        return 0;
    }
    return shader;
}

std::string TrimCopy(const std::string &src)
{
    size_t start = 0;
    while (start < src.size() &&
           (src[start] == ' ' || src[start] == '\n' || src[start] == '\r' || src[start] == '\t')) {
        ++start;
    }
    size_t end = src.size();
    while (end > start &&
           (src[end - 1] == ' ' || src[end - 1] == '\n' || src[end - 1] == '\r' || src[end - 1] == '\t')) {
        --end;
    }
    return src.substr(start, end - start);
}

} // namespace

bool HolmXcLiteGlCreate(HolmXcLiteGl &gl, void *nativeWindow, uint32_t width, uint32_t height,
                        std::string *errCode, std::string *errMsg)
{
    HolmXcLiteGlDestroy(gl);
    if (nativeWindow == nullptr) {
        SetErr(errCode, errMsg, "BLOCKED", "OHNativeWindow null");
        return false;
    }

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        SetErr(errCode, errMsg, "NO_GLES", "eglGetDisplay failed");
        return false;
    }
    EGLint major = 0;
    EGLint minor = 0;
    if (eglInitialize(display, &major, &minor) != EGL_TRUE) {
        SetErr(errCode, errMsg, "NO_GLES", "eglInitialize failed");
        return false;
    }

    const EGLint attribs[] = {EGL_SURFACE_TYPE,
                              EGL_WINDOW_BIT,
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
                              EGL_NONE};
    EGLConfig config = nullptr;
    EGLint numConfigs = 0;
    if (eglChooseConfig(display, attribs, &config, 1, &numConfigs) != EGL_TRUE || numConfigs < 1) {
        eglTerminate(display);
        SetErr(errCode, errMsg, "NO_GLES", "eglChooseConfig failed");
        return false;
    }

    // HarmonyOS OHNativeWindow：本波直接 eglCreateWindowSurface；失败走 NO_GLES。
    EGLSurface surface = eglCreateWindowSurface(
        display, config, reinterpret_cast<EGLNativeWindowType>(nativeWindow), nullptr);
    if (surface == EGL_NO_SURFACE) {
        eglTerminate(display);
        SetErr(errCode, errMsg, "NO_GLES", "eglCreateWindowSurface failed");
        return false;
    }

    const EGLint ctxAttribs[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    EGLContext context = eglCreateContext(display, config, EGL_NO_CONTEXT, ctxAttribs);
    if (context == EGL_NO_CONTEXT) {
        eglDestroySurface(display, surface);
        eglTerminate(display);
        SetErr(errCode, errMsg, "NO_GLES", "eglCreateContext failed");
        return false;
    }

    gl.display = display;
    gl.surface = surface;
    gl.context = context;
    gl.config = config;
    gl.nativeWindow = nativeWindow;
    gl.viewW = static_cast<int32_t>(std::max(1u, width));
    gl.viewH = static_cast<int32_t>(std::max(1u, height));

    if (!MakeCurrent(gl)) {
        HolmXcLiteGlDestroy(gl);
        SetErr(errCode, errMsg, "NO_GLES", "eglMakeCurrent failed");
        return false;
    }
    glViewport(0, 0, gl.viewW, gl.viewH);
    gl.ready = true;
    return true;
}

void HolmXcLiteGlDestroy(HolmXcLiteGl &gl)
{
    if (gl.display != EGL_NO_DISPLAY) {
        if (gl.surface != EGL_NO_SURFACE && gl.context != EGL_NO_CONTEXT) {
            eglMakeCurrent(gl.display, gl.surface, gl.surface, gl.context);
        }
        if (gl.program != 0) {
            ::glDeleteProgram(gl.program);
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
        // 不 eglTerminate(DEFAULT_DISPLAY)：避免误伤进程内其它 EGL 用户（审查 P2）。
        gl.display = EGL_NO_DISPLAY;
    }
    gl.config = nullptr;
    gl.nativeWindow = nullptr;
    gl.posLoc = -1;
    gl.timeLoc = -1;
    gl.resolutionLoc = -1;
    gl.liquidBoostLoc = -1;
    gl.liquidBendLoc = -1;
    gl.viewW = 1;
    gl.viewH = 1;
    gl.animTimeSec = 0.f;
    gl.fragmentBody.clear();
    gl.ready = false;
}

bool HolmXcLiteGlResize(HolmXcLiteGl &gl, uint32_t width, uint32_t height, std::string *errCode,
                        std::string *errMsg)
{
    if (!gl.ready) {
        SetErr(errCode, errMsg, "BLOCKED", "Lite GL not ready");
        return false;
    }
    int32_t w = static_cast<int32_t>(std::max(1u, width));
    int32_t h = static_cast<int32_t>(std::max(1u, height));
    if (w == gl.viewW && h == gl.viewH) {
        return true;
    }
    if (!MakeCurrent(gl)) {
        SetErr(errCode, errMsg, "NO_GLES", "eglMakeCurrent on resize failed");
        return false;
    }
    gl.viewW = w;
    gl.viewH = h;
    glViewport(0, 0, gl.viewW, gl.viewH);
    return true;
}

bool HolmXcLiteGlSetFragmentBody(HolmXcLiteGl &gl, const std::string &body, std::string *errCode,
                                 std::string *errMsg)
{
    std::string trimmed = TrimCopy(body);
    if (trimmed.empty()) {
        SetErr(errCode, errMsg, "FRAGMENT_REQUIRED", "fragmentShader required (Shadertoy body)");
        return false;
    }
    if (trimmed == gl.fragmentBody && gl.program != 0) {
        return true;
    }
    if (!gl.ready || !MakeCurrent(gl)) {
        SetErr(errCode, errMsg, "NO_GLES", "Lite GL not current");
        return false;
    }

    std::string fragSrc = HolmXcShadertoy::WrapFragment(trimmed);
    const char *vertSrc = HolmXcShadertoy::Gles2Vertex();
    GLuint vert = CompileShader(GL_VERTEX_SHADER, vertSrc, errCode, errMsg);
    GLuint frag = CompileShader(GL_FRAGMENT_SHADER, fragSrc.c_str(), errCode, errMsg);
    if (vert == 0 || frag == 0) {
        if (vert != 0) {
            glDeleteShader(vert);
        }
        if (frag != 0) {
            glDeleteShader(frag);
        }
        return false;
    }

    GLuint program = glCreateProgram();
    glAttachShader(program, vert);
    glAttachShader(program, frag);
    glLinkProgram(program);
    glDeleteShader(vert);
    glDeleteShader(frag);
    GLint link = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &link);
    if (link == GL_FALSE) {
        GLint logLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &logLen);
        std::string log = "link failed";
        if (logLen > 1) {
            log.assign(static_cast<size_t>(logLen), '\0');
            glGetProgramInfoLog(program, logLen, nullptr, log.data());
            while (!log.empty() && (log.back() == '\0' || log.back() == '\n')) {
                log.pop_back();
            }
        }
        glDeleteProgram(program);
        SetErr(errCode, errMsg, "SHADER_LINK", log.c_str());
        return false;
    }

    if (gl.program != 0) {
        glDeleteProgram(gl.program);
    }
    gl.program = program;
    gl.fragmentBody = trimmed;
    gl.posLoc = glGetAttribLocation(program, "a_position");
    gl.timeLoc = glGetUniformLocation(program, "u_time");
    gl.resolutionLoc = glGetUniformLocation(program, "u_resolution");
    gl.liquidBoostLoc = glGetUniformLocation(program, "u_liquidBoost");
    gl.liquidBendLoc = glGetUniformLocation(program, "u_liquidBend");
    return true;
}

void HolmXcLiteGlSetLiquid(HolmXcLiteGl &gl, float boost, float bend)
{
    gl.liquidBoost = boost;
    gl.liquidBend = bend;
}

void HolmXcLiteGlDrawFrame(HolmXcLiteGl &gl, float dtSec)
{
    if (!gl.ready || !MakeCurrent(gl)) {
        return;
    }
    float clamped = std::max(0.f, std::min(dtSec, 0.1f));
    gl.animTimeSec += clamped;

    if (gl.program == 0) {
        glClearColor(0.02f, 0.08f, 0.28f, 1.f);
        glClear(GL_COLOR_BUFFER_BIT);
    } else {
        glClearColor(0.05f, 0.07f, 0.1f, 1.f);
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(gl.program);
        if (gl.timeLoc >= 0) {
            glUniform1f(gl.timeLoc, gl.animTimeSec);
        }
        if (gl.resolutionLoc >= 0) {
            glUniform2f(gl.resolutionLoc, static_cast<GLfloat>(gl.viewW), static_cast<GLfloat>(gl.viewH));
        }
        if (gl.liquidBoostLoc >= 0) {
            glUniform1f(gl.liquidBoostLoc, gl.liquidBoost);
        }
        if (gl.liquidBendLoc >= 0) {
            glUniform1f(gl.liquidBendLoc, gl.liquidBend);
        }
        static const GLfloat kQuad[] = {
            -1.f, -1.f, 1.f, -1.f, -1.f, 1.f, 1.f, 1.f,
        };
        if (gl.posLoc >= 0) {
            glEnableVertexAttribArray(static_cast<GLuint>(gl.posLoc));
            glVertexAttribPointer(static_cast<GLuint>(gl.posLoc), 2, GL_FLOAT, GL_FALSE, 0, kQuad);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            glDisableVertexAttribArray(static_cast<GLuint>(gl.posLoc));
        }
    }
    eglSwapBuffers(gl.display, gl.surface);
}

bool HolmXcLiteGlHasProgram(const HolmXcLiteGl &gl)
{
    return gl.program != 0;
}
