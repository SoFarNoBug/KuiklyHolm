package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: gradient_bg GLSL ES 同源源码（斜向多色条带平移；与 shaders/gradient_bg.frag.glsl 对齐）。
 */
object GradientBgShaderSource {
    private const val BODY_FRAG = """
precision mediump float;
varying vec2 v_uv;
uniform float u_time;
uniform float u_scrollSpeed;

void main() {
    vec2 uv = v_uv;
    float speed = max(u_scrollSpeed, 0.0);
    vec2 p = uv + vec2(u_time * 0.085 * speed, u_time * 0.045 * speed);
    float t = fract(p.x * 2.8 + p.y * 1.6);

    vec3 c0 = vec3(0.10, 0.28, 0.92);
    vec3 c1 = vec3(0.18, 0.78, 0.92);
    vec3 c2 = vec3(0.95, 0.42, 0.22);
    vec3 c3 = vec3(0.95, 0.78, 0.20);
    vec3 c4 = vec3(0.55, 0.22, 0.92);

    vec3 col;
    if (t < 0.25) {
        col = mix(c0, c1, t / 0.25);
    } else if (t < 0.50) {
        col = mix(c1, c2, (t - 0.25) / 0.25);
    } else if (t < 0.75) {
        col = mix(c2, c3, (t - 0.50) / 0.25);
    } else {
        col = mix(c3, c4, (t - 0.75) / 0.25);
    }

    float vignette = smoothstep(1.15, 0.35, length(uv - 0.5));
    col *= 0.78 + 0.32 * vignette;
    gl_FragColor = vec4(col, 1.0);
}
"""

    private const val WEB_FRAG = """
#version 300 es
precision mediump float;
in vec2 v_uv;
uniform float u_time;
uniform float u_scrollSpeed;
out vec4 fragColor;

void main() {
    vec2 uv = v_uv;
    float speed = max(u_scrollSpeed, 0.0);
    vec2 p = uv + vec2(u_time * 0.085 * speed, u_time * 0.045 * speed);
    float t = fract(p.x * 2.8 + p.y * 1.6);

    vec3 c0 = vec3(0.10, 0.28, 0.92);
    vec3 c1 = vec3(0.18, 0.78, 0.92);
    vec3 c2 = vec3(0.95, 0.42, 0.22);
    vec3 c3 = vec3(0.95, 0.78, 0.20);
    vec3 c4 = vec3(0.55, 0.22, 0.92);

    vec3 col;
    if (t < 0.25) {
        col = mix(c0, c1, t / 0.25);
    } else if (t < 0.50) {
        col = mix(c1, c2, (t - 0.25) / 0.25);
    } else if (t < 0.75) {
        col = mix(c2, c3, (t - 0.50) / 0.25);
    } else {
        col = mix(c3, c4, (t - 0.75) / 0.25);
    }

    float vignette = smoothstep(1.15, 0.35, length(uv - 0.5));
    col *= 0.78 + 0.32 * vignette;
    fragColor = vec4(col, 1.0);
}
"""

    /** Android GLES2 / H5 WebGL1 */
    val gles2Fragment: String = BODY_FRAG.trimIndent()

    /** H5 WebGL2（预留） */
    val webgl2Fragment: String = WEB_FRAG.trimIndent()
}
