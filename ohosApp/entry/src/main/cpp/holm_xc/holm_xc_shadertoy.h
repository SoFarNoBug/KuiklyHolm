/**
 * @file holm_xc_shadertoy.h
 * @brief ADR 0011 · Shadertoy wrap / 全屏 vert（与 Kotlin SoT 对齐）
 * @date 2026-11-15
 * @author Auto
 *
 * SoT:
 *   KuiklyHolmCore/.../HolmShadertoyShell.kt · wrapFragment
 *   KuiklyHolmCore/.../HolmBuiltinShaders.kt · gles2Vertex
 * 改 SoT 须同步本文件，并在 dualtrack / ohos changelog 留痕。
 */
#ifndef HOLM_XC_SHADERTOY_H
#define HOLM_XC_SHADERTOY_H

#include <string>

namespace HolmXcShadertoy {

/** 对齐 HolmBuiltinShaders.gles2Vertex */
inline const char *Gles2Vertex()
{
    return R"(attribute vec2 a_position;
varying vec2 v_uv;
void main() {
    v_uv = a_position * 0.5 + 0.5;
    gl_Position = vec4(a_position, 0.0, 1.0);
})";
}

/**
 * 对齐 HolmShadertoyShell.wrapFragment(body)。
 * body 须非空；调用方负责 trim。
 */
inline std::string WrapFragment(const std::string &body)
{
    return std::string(
               "#ifdef GL_FRAGMENT_PRECISION_HIGH\n"
               "precision highp float;\n"
               "#else\n"
               "precision mediump float;\n"
               "#endif\n"
               "varying vec2 v_uv;\n"
               "uniform float u_time;\n"
               "uniform vec2 u_resolution;\n"
               "\n"
               "#define iTime u_time\n"
               "#define iResolution vec3(u_resolution, 1.0)\n"
               "\n") +
           body +
           "\n"
           "\n"
           "void main() {\n"
           "    vec4 col = vec4(0.0);\n"
           "    mainImage(col, v_uv * u_resolution.xy);\n"
           "    gl_FragColor = col;\n"
           "}\n";
}

} // namespace HolmXcShadertoy

#endif /* HOLM_XC_SHADERTOY_H */
