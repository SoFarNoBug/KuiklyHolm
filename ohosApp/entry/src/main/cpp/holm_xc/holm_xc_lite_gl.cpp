/**
 * @file holm_xc_lite_gl.cpp
 * @brief ADR 0011 · EGL/GLES2 Lite 实现
 * @date 2026-11-15
 * @author Auto
 */
#include "holm_xc_lite_gl.h"
#include "holm_xc_shadertoy.h"

#include <algorithm>
#include <chrono>
#include <cstring>
#include <string>

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
    eglBindAPI(EGL_OPENGL_ES_API);
    if (eglMakeCurrent(gl.display, gl.surface, gl.surface, gl.context) != EGL_TRUE) {
        return false;
    }
    return eglGetCurrentContext() == gl.context;
}

const GLubyte *GlGetStringSafe(GLenum name)
{
    const GLubyte *ver = glGetString(name);
    if (ver != nullptr) {
        return ver;
    }
    using GetStringFn = const GLubyte *(*)(GLenum);
    auto getString = reinterpret_cast<GetStringFn>(eglGetProcAddress("glGetString"));
    if (getString == nullptr) {
        return nullptr;
    }
    return getString(name);
}

GLuint CompileShader(GLenum type, const char *source, std::string *errCode, std::string *errMsg)
{
    while (glGetError() != GL_NO_ERROR) {
    }
    GLuint shader = glCreateShader(type);
    if (shader == 0) {
        const GLenum glErr = glGetError();
        const EGLint eglErr = eglGetError();
        const bool ctxOk = eglGetCurrentContext() != EGL_NO_CONTEXT;
        SetErr(errCode, errMsg, "SHADER_COMPILE",
               (std::string("glCreateShader failed glErr=") + std::to_string(glErr) +
                " eglErr=" + std::to_string(eglErr) + " ctx=" + (ctxOk ? "1" : "0"))
                   .c_str());
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
            glGetShaderInfoLog(shader, logLen, nullptr, &log[0]);
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
    // 先解绑再 BindAPI：wgpu-GL 切轨后线程可能仍挂着旧 current / 错 API
    eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    if (eglBindAPI(EGL_OPENGL_ES_API) != EGL_TRUE) {
        SetErr(errCode, errMsg, "NO_GLES", "eglBindAPI(OPENGL_ES) failed");
        return false;
    }
    EGLint major = 0;
    EGLint minor = 0;
    if (eglInitialize(display, &major, &minor) != EGL_TRUE) {
        SetErr(errCode, errMsg, "NO_GLES", "eglInitialize failed");
        return false;
    }

    // WINDOW|PBUFFER；Ohos 官方推荐 GLES3（OpenGLWrapper 对 ES2 切轨后常拒 CreateShader）
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
                              EGL_NONE};
    EGLConfig config = nullptr;
    EGLint numConfigs = 0;
    if (eglChooseConfig(display, attribs, &config, 1, &numConfigs) != EGL_TRUE || numConfigs < 1) {
        // 回退 ES2 config
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
                                     EGL_NONE};
        if (eglChooseConfig(display, attribsEs2, &config, 1, &numConfigs) != EGL_TRUE || numConfigs < 1) {
            SetErr(errCode, errMsg, "NO_GLES", "eglChooseConfig failed");
            return false;
        }
    }

    const EGLint pbufAttr[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
    EGLSurface pbuffer = eglCreatePbufferSurface(display, config, pbufAttr);
    if (pbuffer == EGL_NO_SURFACE) {
        const EGLint eglErr = eglGetError();
        SetErr(errCode, errMsg, "NO_GLES",
               (std::string("eglCreatePbufferSurface failed eglError=") + std::to_string(eglErr))
                   .c_str());
        return false;
    }

    // 优先 ES3 context；失败再 ES2
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
    const GLubyte *ver = GlGetStringSafe(GL_VERSION);
    if (ver == nullptr) {
        const EGLint eglErr = eglGetError();
        const EGLenum api = eglQueryAPI();
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroyContext(display, context);
        eglDestroySurface(display, pbuffer);
        SetErr(errCode, errMsg, "NO_GLES",
               (std::string("glGetString null on pbuffer eglErr=") + std::to_string(eglErr) +
                " api=" + std::to_string(api))
                   .c_str());
        return false;
    }

    EGLSurface surface = eglCreateWindowSurface(
        display, config, reinterpret_cast<EGLNativeWindowType>(nativeWindow), nullptr);
    if (surface == EGL_NO_SURFACE) {
        const EGLint eglErr = eglGetError();
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroyContext(display, context);
        eglDestroySurface(display, pbuffer);
        SetErr(errCode, errMsg, "NO_GLES",
               (std::string("eglCreateWindowSurface failed eglError=") + std::to_string(eglErr)).c_str());
        return false;
    }

    gl.display = display;
    gl.surface = surface;
    gl.context = context;
    gl.config = config;
    gl.nativeWindow = nativeWindow;
    gl.viewW = static_cast<int32_t>(std::max(1u, width));
    gl.viewH = static_cast<int32_t>(std::max(1u, height));

    // 窗口挂上后丢弃探测用 pbuffer（context 仍有效）
    eglDestroySurface(display, pbuffer);

    if (!MakeCurrent(gl)) {
        HolmXcLiteGlDestroy(gl);
        SetErr(errCode, errMsg, "NO_GLES", "eglMakeCurrent(window) failed");
        return false;
    }
    if (GlGetStringSafe(GL_VERSION) == nullptr) {
        HolmXcLiteGlDestroy(gl);
        SetErr(errCode, errMsg, "NO_GLES", "glGetString null after window MakeCurrent");
        return false;
    }
    while (glGetError() != GL_NO_ERROR) {
    }
    GLuint smoke = glCreateShader(GL_VERTEX_SHADER);
    if (smoke == 0) {
        const GLenum glErr = glGetError();
        const EGLint eglErr = eglGetError();
        HolmXcLiteGlDestroy(gl);
        SetErr(errCode, errMsg, "NO_GLES",
               (std::string("glCreateShader smoke failed glErr=") + std::to_string(glErr) +
                " eglErr=" + std::to_string(eglErr))
                   .c_str());
        return false;
    }
    glDeleteShader(smoke);
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
                                 std::string *errMsg, double *outWrapMs, double *outCompileMs)
{
    auto setMs = [](double *slot, double v) {
        if (slot != nullptr) {
            *slot = v;
        }
    };
    setMs(outWrapMs, 0.0);
    setMs(outCompileMs, 0.0);

    std::string trimmed = TrimCopy(body);
    if (trimmed.empty()) {
        SetErr(errCode, errMsg, "FRAGMENT_REQUIRED", "fragmentShader required (Shadertoy body)");
        return false;
    }
    if (trimmed == gl.fragmentBody && gl.program != 0) {
        return true;
    }

    using Clock = std::chrono::steady_clock;
    auto wrapT0 = Clock::now();
    std::string fragSrc = HolmXcShadertoy::WrapFragment(trimmed);
    const char *vertSrc = HolmXcShadertoy::Gles2Vertex();
    setMs(outWrapMs, std::chrono::duration<double, std::milli>(Clock::now() - wrapT0).count());

    auto tryLink = [&](double *compileMsOut) -> bool {
        if (!gl.ready || !MakeCurrent(gl)) {
            SetErr(errCode, errMsg, "NO_GLES", "Lite GL not current");
            return false;
        }
        auto compileT0 = Clock::now();
        GLuint vert = CompileShader(GL_VERTEX_SHADER, vertSrc, errCode, errMsg);
        GLuint frag = CompileShader(GL_FRAGMENT_SHADER, fragSrc.c_str(), errCode, errMsg);
        if (vert == 0 || frag == 0) {
            setMs(compileMsOut, std::chrono::duration<double, std::milli>(Clock::now() - compileT0).count());
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
                glGetProgramInfoLog(program, logLen, nullptr, &log[0]);
                while (!log.empty() && (log.back() == '\0' || log.back() == '\n')) {
                    log.pop_back();
                }
            }
            glDeleteProgram(program);
            setMs(compileMsOut, std::chrono::duration<double, std::milli>(Clock::now() - compileT0).count());
            SetErr(errCode, errMsg, "SHADER_LINK", log.c_str());
            return false;
        }
        setMs(compileMsOut, std::chrono::duration<double, std::milli>(Clock::now() - compileT0).count());

        if (gl.program != 0) {
            glDeleteProgram(gl.program);
        }
        gl.program = program;
        gl.fragmentBody = trimmed;
        gl.posLoc = glGetAttribLocation(program, "a_position");
        gl.timeLoc = glGetUniformLocation(program, "u_time");
        gl.resolutionLoc = glGetUniformLocation(program, "u_resolution");
        return true;
    };

    if (tryLink(outCompileMs)) {
        return true;
    }
    // CreateShader 失败：整轨拆掉重建一次（wgpu-GL 切轨后常见）
    const bool createShaderFail =
        errCode != nullptr && *errCode == "SHADER_COMPILE" &&
        errMsg != nullptr && errMsg->find("glCreateShader failed") != std::string::npos;
    if (!createShaderFail || gl.nativeWindow == nullptr) {
        return false;
    }
    void *win = gl.nativeWindow;
    uint32_t w = static_cast<uint32_t>(std::max(1, gl.viewW));
    uint32_t h = static_cast<uint32_t>(std::max(1, gl.viewH));
    HolmXcLiteGlDestroy(gl);
    eglReleaseThread();
    std::string recreateCode;
    std::string recreateMsg;
    if (!HolmXcLiteGlCreate(gl, win, w, h, &recreateCode, &recreateMsg)) {
        SetErr(errCode, errMsg, recreateCode.c_str(), recreateMsg.c_str());
        return false;
    }
    if (errCode != nullptr) {
        errCode->clear();
    }
    if (errMsg != nullptr) {
        errMsg->clear();
    }
    return tryLink(outCompileMs);
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
