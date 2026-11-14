package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/14
 * @Author: jlj
 * @Description: Lite 内置全屏顶点；官方五席已删，片元须由宿主 fragmentShader 提供。
 */
object HolmBuiltinShaders {
    val gles2Vertex: String = """
attribute vec2 a_position;
varying vec2 v_uv;
void main() {
    v_uv = a_position * 0.5 + 0.5;
    gl_Position = vec4(a_position, 0.0, 1.0);
}
""".trimIndent()
}
