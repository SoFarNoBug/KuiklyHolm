package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/13
 * @Author: jlj
 * @Description: Shadertoy 正文 → GLES2/WebGL1 可编译片元（补 precision / u_time / u_resolution / main）。
 */
object HolmShadertoyShell {
    /**
     * 业务只传算法正文（可含 `mainImage`）；库补信封。
     * 契约：`iTime`、`iResolution`（vec3）、`mainImage(out vec4, in vec2)`。
     */
    fun wrapFragment(body: String): String {
        val trimmed = body.trim()
        require(trimmed.isNotEmpty()) { "Shadertoy body must be non-blank" }
        return """
precision mediump float;
varying vec2 v_uv;
uniform float u_time;
uniform vec2 u_resolution;

#define iTime u_time
#define iResolution vec3(u_resolution, 1.0)

$trimmed

void main() {
    vec4 col = vec4(0.0);
    mainImage(col, v_uv * u_resolution.xy);
    gl_FragColor = col;
}
""".trimIndent()
    }
}
