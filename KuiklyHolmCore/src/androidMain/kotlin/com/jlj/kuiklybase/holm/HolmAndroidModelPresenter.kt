package com.jlj.kuiklybase.holm

import android.net.Uri
import com.jlj.kuiklybase.holm.gltf.GltfDocument
import com.jlj.kuiklybase.holm.gltf.GltfMinimalGlb
import com.jlj.kuiklybase.holm.gltf.GltfParseException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: Android Model Present — 异步读+parse，GL 网格出画（wgpu executor 同岛切 GLES 网格层）。
 */
internal object HolmAndroidModelPresenter {
    private val _io = Executors.newSingleThreadExecutor { r ->
        Thread(r, "holm-gltf-io").also { it.isDaemon = true }
    }
    private val _gen = AtomicInteger(0)

    fun attach(view: KRHolmSurfaceView, content: HolmModelContent) {
        val gen = _gen.incrementAndGet()
        view.enterModelPresentMode(content)
        _io.execute {
            try {
                val bytes = loadBytes(view, content)
                val doc = GltfDocument.parse(bytes)
                view.post {
                    if (gen != _gen.get()) return@post
                    view.onModelDocumentReady(content, doc)
                }
            } catch (t: Throwable) {
                val msg = when (t) {
                    is GltfParseException -> t.message ?: "parse failed"
                    else -> t.message ?: t::class.simpleName ?: "load failed"
                }
                view.post {
                    if (gen != _gen.get()) return@post
                    view.onModelLoadFailed("MODEL_LOAD", msg)
                }
            }
        }
    }

    fun detach(view: KRHolmSurfaceView) {
        cancelPending()
        view.exitModelPresentMode()
    }

    /** 仅作废进行中的 load/parse（卸岛用，不碰 Present / executor）。 */
    fun cancelPending() {
        _gen.incrementAndGet()
    }

    private fun loadBytes(view: KRHolmSurfaceView, content: HolmModelContent): ByteArray {
        content.bytes?.let { return it }
        val asset = content.assetPath
        if (!asset.isNullOrBlank()) {
            // 约定：assets/models/hero.glb 缺失时回落程序立方体，便于 Demo 零二进制进仓
            return try {
                view.context.assets.open(asset).use { it.readBytes() }
            } catch (_: Exception) {
                if (asset.contains("hero.glb")) {
                    GltfMinimalGlb.buildHeroCubeGlb()
                } else {
                    throw GltfParseException("assets missing: $asset")
                }
            }
        }
        val uri = content.fileUri
        if (!uri.isNullOrBlank()) {
            val parsed = Uri.parse(uri)
            view.context.contentResolver.openInputStream(parsed)?.use { return it.readBytes() }
                ?: throw GltfParseException("cannot open fileUri=$uri")
        }
        throw GltfParseException("HolmModelContent has no bytes/asset/fileUri")
    }
}
