package com.jlj.kuiklybase.holm

import android.view.Surface
import com.jlj.kmp.wgpu.surface.ModelPass

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: Holm 网格 Present 薄包（Android）；底层 [ModelPass]，非 FullscreenPass。
 */
class HolmModelPass private constructor(
    private val _pass: ModelPass,
) {
    val handle: Long get() = _pass.handle

    fun resize(width: Int, height: Int) {
        _pass.resize(width, height)
    }

    fun setMeshes(verts: FloatArray, indices: IntArray): Boolean =
        _pass.setMeshes(verts, indices)

    fun setFrame(mvp: FloatArray, color: FloatArray, lit: Boolean, lightDir: FloatArray) {
        _pass.setFrame(mvp, color, lit, lightDir)
    }

    fun setPlaying(playing: Boolean) {
        _pass.setPlaying(playing)
    }

    fun tick(dtSecs: Float): Boolean = _pass.tick(dtSecs)

    fun backendId(): String = _pass.backendId()

    fun lastErrorCode(): String = _pass.lastErrorCode()

    fun lastErrorMessage(): String = _pass.lastErrorMessage()

    fun drainErrors(): List<Pair<String, String>> = _pass.drainErrors()

    fun destroy() {
        _pass.destroy()
    }

    companion object {
        fun create(surface: Surface, width: Int, height: Int): HolmModelPass? {
            val pass = ModelPass.create(surface, width, height) ?: return null
            return HolmModelPass(pass)
        }
    }
}
