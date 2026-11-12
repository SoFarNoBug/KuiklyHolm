package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: fullscreen_warp GLSL ES 同源源码（全屏场扭曲；与 shaders/fullscreen_warp.frag.glsl 对齐）。
 */
object FullscreenWarpShaderSource {
    private const val BODY_FRAG = """
precision mediump float;
varying vec2 v_uv;
uniform float u_time;
uniform float u_warpAmount;

vec3 fieldColor(vec2 uv) {
    vec2 p = uv + vec2(u_time * 0.02, u_time * 0.015);
    float stripe = smoothstep(0.32, 0.68, fract(p.x * 4.2 + p.y * 1.1));
    vec3 a = vec3(0.08, 0.22, 0.48);
    vec3 b = vec3(0.55, 0.78, 0.95);
    vec3 col = mix(a, b, stripe);
    float blob = 1.0 - smoothstep(0.08, 0.28, length(p - vec2(0.4 + 0.08 * sin(u_time * 0.4), 0.55)));
    col = mix(col, vec3(0.95, 0.55, 0.25), blob * 0.55);
    float blob2 = 1.0 - smoothstep(0.06, 0.22, length(p - vec2(0.72, 0.35 + 0.06 * cos(u_time * 0.5))));
    col = mix(col, vec3(0.35, 0.9, 0.7), blob2 * 0.45);
    return col;
}

void main() {
    vec2 uv = v_uv;
    float amp = max(u_warpAmount, 0.0);
    float w1 = sin(uv.y * 12.0 + u_time * 1.35) * 0.55
             + sin(uv.y * 7.0 - u_time * 0.85 + 1.2) * 0.45;
    float w2 = cos(uv.x * 10.0 + u_time * 1.05) * 0.55
             + cos(uv.x * 5.5 + u_time * 0.65) * 0.45;
    vec2 offset = vec2(w1, w2) * amp;
    vec2 sampleUv = clamp(uv + offset, 0.0, 1.0);
    vec3 col = fieldColor(sampleUv);
    float haze = 0.04 * amp * (0.5 + 0.5 * sin(u_time * 2.0 + uv.x * 6.0));
    col += vec3(haze);
    gl_FragColor = vec4(col, 1.0);
}
"""

    private const val WEB_FRAG = """
#version 300 es
precision mediump float;
in vec2 v_uv;
uniform float u_time;
uniform float u_warpAmount;
out vec4 fragColor;

vec3 fieldColor(vec2 uv) {
    vec2 p = uv + vec2(u_time * 0.02, u_time * 0.015);
    float stripe = smoothstep(0.32, 0.68, fract(p.x * 4.2 + p.y * 1.1));
    vec3 a = vec3(0.08, 0.22, 0.48);
    vec3 b = vec3(0.55, 0.78, 0.95);
    vec3 col = mix(a, b, stripe);
    float blob = 1.0 - smoothstep(0.08, 0.28, length(p - vec2(0.4 + 0.08 * sin(u_time * 0.4), 0.55)));
    col = mix(col, vec3(0.95, 0.55, 0.25), blob * 0.55);
    float blob2 = 1.0 - smoothstep(0.06, 0.22, length(p - vec2(0.72, 0.35 + 0.06 * cos(u_time * 0.5))));
    col = mix(col, vec3(0.35, 0.9, 0.7), blob2 * 0.45);
    return col;
}

void main() {
    vec2 uv = v_uv;
    float amp = max(u_warpAmount, 0.0);
    float w1 = sin(uv.y * 12.0 + u_time * 1.35) * 0.55
             + sin(uv.y * 7.0 - u_time * 0.85 + 1.2) * 0.45;
    float w2 = cos(uv.x * 10.0 + u_time * 1.05) * 0.55
             + cos(uv.x * 5.5 + u_time * 0.65) * 0.45;
    vec2 offset = vec2(w1, w2) * amp;
    vec2 sampleUv = clamp(uv + offset, 0.0, 1.0);
    vec3 col = fieldColor(sampleUv);
    float haze = 0.04 * amp * (0.5 + 0.5 * sin(u_time * 2.0 + uv.x * 6.0));
    col += vec3(haze);
    fragColor = vec4(col, 1.0);
}
"""

    /** Android GLES2 / H5 WebGL1 */
    val gles2Fragment: String = BODY_FRAG.trimIndent()

    /** H5 WebGL2（预留） */
    val webgl2Fragment: String = WEB_FRAG.trimIndent()
}
