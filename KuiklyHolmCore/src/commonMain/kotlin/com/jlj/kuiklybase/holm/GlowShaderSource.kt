package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: 柔光光晕 GLSL ES 同源源码（程序化光斑；与 shaders/glow.frag.glsl 对齐）。
 */
object GlowShaderSource {
    private const val BODY_FRAG = """
precision mediump float;
varying vec2 v_uv;
uniform float u_time;

void main() {
    vec2 uv = v_uv;
    vec3 col = vec3(0.04, 0.05, 0.10);

    vec2 c1 = vec2(0.32 + 0.14 * sin(u_time * 0.55), 0.48 + 0.12 * cos(u_time * 0.40));
    float b1 = 1.0 - smoothstep(0.0, 0.48, length(uv - c1));
    b1 *= 0.82 + 0.18 * sin(u_time * 1.1);
    col += vec3(0.18, 0.72, 1.0) * (b1 * b1);

    vec2 c2 = vec2(0.68 + 0.10 * cos(u_time * 0.48), 0.38 + 0.14 * sin(u_time * 0.62));
    float b2 = 1.0 - smoothstep(0.0, 0.42, length(uv - c2));
    b2 *= 0.80 + 0.20 * cos(u_time * 0.95);
    col += vec3(0.95, 0.28, 0.72) * (b2 * b2);

    vec2 c3 = vec2(0.52 + 0.08 * sin(u_time * 0.35), 0.72 + 0.08 * cos(u_time * 0.70));
    float b3 = 1.0 - smoothstep(0.0, 0.36, length(uv - c3));
    b3 *= 0.75 + 0.25 * sin(u_time * 0.8 + 1.2);
    col += vec3(0.95, 0.78, 0.28) * (b3 * b3);

    float edge = smoothstep(1.2, 0.35, length(uv - 0.5));
    col *= 0.72 + 0.38 * edge;
    gl_FragColor = vec4(col, 1.0);
}
"""

    private const val WEB_FRAG = """
#version 300 es
precision mediump float;
in vec2 v_uv;
uniform float u_time;
out vec4 fragColor;

void main() {
    vec2 uv = v_uv;
    vec3 col = vec3(0.04, 0.05, 0.10);

    vec2 c1 = vec2(0.32 + 0.14 * sin(u_time * 0.55), 0.48 + 0.12 * cos(u_time * 0.40));
    float b1 = 1.0 - smoothstep(0.0, 0.48, length(uv - c1));
    b1 *= 0.82 + 0.18 * sin(u_time * 1.1);
    col += vec3(0.18, 0.72, 1.0) * (b1 * b1);

    vec2 c2 = vec2(0.68 + 0.10 * cos(u_time * 0.48), 0.38 + 0.14 * sin(u_time * 0.62));
    float b2 = 1.0 - smoothstep(0.0, 0.42, length(uv - c2));
    b2 *= 0.80 + 0.20 * cos(u_time * 0.95);
    col += vec3(0.95, 0.28, 0.72) * (b2 * b2);

    vec2 c3 = vec2(0.52 + 0.08 * sin(u_time * 0.35), 0.72 + 0.08 * cos(u_time * 0.70));
    float b3 = 1.0 - smoothstep(0.0, 0.36, length(uv - c3));
    b3 *= 0.75 + 0.25 * sin(u_time * 0.8 + 1.2);
    col += vec3(0.95, 0.78, 0.28) * (b3 * b3);

    float edge = smoothstep(1.2, 0.35, length(uv - 0.5));
    col *= 0.72 + 0.38 * edge;
    fragColor = vec4(col, 1.0);
}
"""

    /** Android GLES2 / H5 WebGL1 */
    val gles2Fragment: String = BODY_FRAG.trimIndent()

    /** H5 WebGL2（预留） */
    val webgl2Fragment: String = WEB_FRAG.trimIndent()
}
