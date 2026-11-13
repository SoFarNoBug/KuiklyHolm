package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/13
 * @Author: jlj
 * @Description: Lite 路径 vert/frag 解析（自定义正文走 Shadertoy 壳；空则官方五席）。
 */
object HolmShaderResolve {
    fun vertex(customVert: String?): String {
        val v = customVert?.trim().orEmpty()
        return if (v.isEmpty()) HolmSceneShaders.gles2Vertex() else v
    }

    fun fragment(scene: String, customFragBody: String?): String {
        val body = customFragBody?.trim().orEmpty()
        return if (body.isEmpty()) {
            HolmSceneShaders.gles2Fragment(scene)
        } else {
            HolmShadertoyShell.wrapFragment(body)
        }
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
