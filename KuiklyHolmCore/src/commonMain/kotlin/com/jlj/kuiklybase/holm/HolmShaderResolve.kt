package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/13
 * @Author: jlj
 * @Description: Lite 路径 vert/frag 解析；无宿主片元正文则失败（五席已移除）。
 */
object HolmShaderResolve {
    fun vertex(customVert: String?): String {
        val v = customVert?.trim().orEmpty()
        return if (v.isEmpty()) HolmBuiltinShaders.gles2Vertex else v
    }

    /**
     * @param scene 保留参数兼容旧调用；官方 scene 路由已废除。
     */
    fun fragment(scene: String, customFragBody: String?): String {
        val body = customFragBody?.trim().orEmpty()
        if (body.isEmpty()) {
            throw IllegalArgumentException(
                "fragmentShader required (official scenes removed; scene=$scene)",
            )
        }
        return HolmShadertoyShell.wrapFragment(body)
    }

    fun hasCustomFragment(customFragBody: String?): Boolean =
        !customFragBody.isNullOrBlank()

    /** Runtime 片元启发式（含 `@fragment` / `fs_main`）；用于切轨时跳过错方言热换。 */
    fun looksLikeWgslFragment(src: String?): Boolean {
        val t = src?.trim().orEmpty()
        if (t.isEmpty()) return false
        return t.contains("@fragment") || t.contains("fn fs_main")
    }
}
