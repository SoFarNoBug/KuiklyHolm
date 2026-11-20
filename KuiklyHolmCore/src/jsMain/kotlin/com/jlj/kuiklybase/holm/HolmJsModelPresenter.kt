package com.jlj.kuiklybase.holm

import com.jlj.kuiklybase.holm.gltf.GltfDocument
import com.jlj.kuiklybase.holm.gltf.GltfMesh
import com.jlj.kuiklybase.holm.gltf.GltfMinimalGlb
import com.jlj.kuiklybase.holm.gltf.GltfParseException
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.Uint32Array
import org.w3c.fetch.Response
import kotlin.js.Promise

/**
 * @CreateDate: 2026/11/19
 * @Author: jlj
 * @Description: H5 Model Present — 异步 fetch+Worker parse；WebGL / wgpu ModelPass 出画。
 */
internal object HolmJsModelPresenter {

    fun attach(view: KRHolmSurfaceView, content: HolmModelContent) {
        val gen = view.nextModelLoadGen()
        view.enterModelPresentMode(content)
        waitDracoReady()
            .then { loadBytes(content) }
            .then { bytes ->
                if (gen != view.modelLoadGen()) return@then null
                parseOffMain(bytes, gen, view, content)
            }
            .catch { err ->
                if (gen != view.modelLoadGen()) return@catch
                val msg = (err as? Throwable)?.message
                    ?: err?.toString()
                    ?: "load failed"
                view.onModelLoadFailed("MODEL_LOAD", msg)
            }
    }

    fun detach(view: KRHolmSurfaceView) {
        cancelPending(view)
        view.exitModelPresentMode()
    }

    fun cancelPending(view: KRHolmSurfaceView) {
        view.bumpModelLoadGen()
    }

    /** Worker 优先；失败回退主线程 GltfDocument.parse（ADR 0020 P1）。 */
    private fun parseOffMain(
        bytes: ByteArray,
        gen: Int,
        view: KRHolmSurfaceView,
        content: HolmModelContent,
    ): Promise<Unit> {
        return Promise { resolveOuter, _ ->
            parseInWorker(bytes)
                .catch { _ ->
                    Promise { resolve, reject ->
                        try {
                            resolve.invoke(GltfDocument.parse(bytes))
                        } catch (t: Throwable) {
                            reject.invoke(t)
                        }
                    }
                }
                .then { doc ->
                    if (gen == view.modelLoadGen()) {
                        view.onModelDocumentReady(content, doc)
                    }
                    resolveOuter.invoke(Unit)
                }
                .catch { err ->
                    if (gen == view.modelLoadGen()) {
                        val msg = when (err) {
                            is GltfParseException -> err.message ?: "parse failed"
                            is Throwable -> err.message ?: err::class.simpleName ?: "load failed"
                            else -> err?.toString() ?: "load failed"
                        }
                        view.onModelLoadFailed("MODEL_LOAD", msg)
                    }
                    resolveOuter.invoke(Unit)
                }
        }
    }

    private fun parseInWorker(bytes: ByteArray): Promise<GltfDocument> {
        return Promise { resolve, reject ->
            val hasWorker = js("typeof Worker !== 'undefined'").unsafeCast<Boolean>()
            if (!hasWorker) {
                reject.invoke(GltfParseException("Worker unavailable"))
                return@Promise
            }
            try {
                val script = HolmJsModelParseWorkerSource.SCRIPT
                val u8 = byteArrayToUint8Array(bytes)
                js(
                    """
                    (function(script, u8, ok, fail) {
                      var blob = new Blob([script], { type: 'application/javascript' });
                      var url = URL.createObjectURL(blob);
                      var worker = new Worker(url);
                      var settled = false;
                      var timer = 0;
                      function finish(fn, arg) {
                        if (settled) return;
                        settled = true;
                        if (timer) { try { clearTimeout(timer); } catch (e0) {} }
                        try { worker.terminate(); } catch (e1) {}
                        try { URL.revokeObjectURL(url); } catch (e2) {}
                        fn(arg);
                      }
                      timer = setTimeout(function() {
                        finish(fail, new Error('parse worker timeout'));
                      }, 12000);
                      worker.onmessage = function(ev) {
                        var d = ev.data || {};
                        if (d.ok) finish(ok, d);
                        else finish(fail, new Error(d.err || 'parse worker failed'));
                      };
                      worker.onerror = function(e) {
                        finish(fail, e);
                      };
                      try {
                        var ab = u8.buffer.slice(u8.byteOffset, u8.byteOffset + u8.byteLength);
                        var base = (typeof location !== 'undefined' && location.origin)
                          ? (location.origin + '/')
                          : '/';
                        worker.postMessage({ ab: ab, base: base }, [ab]);
                      } catch (e) {
                        finish(fail, e);
                      }
                    })
                    """,
                ).unsafeCast<(String, Uint8Array, (dynamic) -> Unit, (dynamic) -> Unit) -> Unit>()(
                    script,
                    u8,
                    { data ->
                        try {
                            resolve.invoke(documentFromWorkerData(data))
                        } catch (t: Throwable) {
                            reject.invoke(t)
                        }
                    },
                    { err ->
                        val msg = (err as? Throwable)?.message
                            ?: err?.asDynamic()?.message?.toString()
                            ?: err?.toString()
                            ?: "parse worker failed"
                        reject.invoke(GltfParseException(msg))
                    },
                )
            } catch (t: Throwable) {
                reject.invoke(t)
            }
        }
    }

    private fun documentFromWorkerData(data: dynamic): GltfDocument {
        val bc = data.baseColor
        val baseColor = floatArrayOf(
            ((bc[0] as? Number)?.toFloat() ?: 1f),
            ((bc[1] as? Number)?.toFloat() ?: 1f),
            ((bc[2] as? Number)?.toFloat() ?: 1f),
            ((bc[3] as? Number)?.toFloat() ?: 1f),
        )
        val meshesJs = data.meshes
        val meshCount = (meshesJs.length as Number).toInt()
        val meshes = ArrayList<GltfMesh>(meshCount)
        for (i in 0 until meshCount) {
            val m = meshesJs[i]
            val name = (m.name as? String) ?: "mesh$i"
            meshes += GltfMesh(
                name = name,
                positions = float32ToFloatArray(m.positions.unsafeCast<Float32Array>()),
                normals = float32ToFloatArray(m.normals.unsafeCast<Float32Array>()),
                uvs = float32ToFloatArray(m.uvs.unsafeCast<Float32Array>()),
                indices = uint32ToIntArray(m.indices.unsafeCast<Uint32Array>()),
            )
        }
        return GltfDocument(meshes = meshes, baseColorFactor = baseColor, baseColorImage = null)
    }

    private fun float32ToFloatArray(arr: Float32Array): FloatArray {
        val len = arr.length
        val out = FloatArray(len)
        val dyn = arr.asDynamic()
        for (i in 0 until len) {
            out[i] = (dyn[i] as Number).toFloat()
        }
        return out
    }

    private fun uint32ToIntArray(arr: Uint32Array): IntArray {
        val len = arr.length
        val out = IntArray(len)
        val dyn = arr.asDynamic()
        for (i in 0 until len) {
            out[i] = (dyn[i] as Number).toInt()
        }
        return out
    }

    private fun byteArrayToUint8Array(bytes: ByteArray): Uint8Array {
        val u8 = Uint8Array(bytes.size)
        val dyn = u8.asDynamic()
        for (i in bytes.indices) {
            dyn[i] = bytes[i].toInt() and 0xFF
        }
        return u8
    }

    /** 对齐 __kmpWgpu.ready：短轮询 Draco wasm；超时后仍继续（decode 失败走 MODEL_LOAD）。 */
    private fun waitDracoReady(): Promise<Unit> {
        return Promise { resolve, _ ->
            js(
                """
                (function(ok) {
                  if (window.__kmpDraco && window.__kmpDraco.ready === true) { ok(); return; }
                  var n = 0;
                  var t = setInterval(function() {
                    n++;
                    if (window.__kmpDraco && window.__kmpDraco.ready === true) {
                      clearInterval(t); ok();
                    } else if (window.__kmpDraco && window.__kmpDraco.error) {
                      clearInterval(t); ok();
                    } else if (n >= 40) {
                      clearInterval(t); ok();
                    }
                  }, 50);
                })
                """,
            ).unsafeCast<(() -> Unit) -> Unit>()({ resolve.invoke(Unit) })
        }
    }

    private fun loadBytes(content: HolmModelContent): Promise<ByteArray> {
        content.bytes?.let { return Promise.resolve(it) }
        val asset = content.assetPath
        if (!asset.isNullOrBlank()) {
            val cleaned = asset.removePrefix("/")
            return fetchBytes(cleaned).catch { err ->
                if (cleaned.contains("hero.glb")) {
                    GltfMinimalGlb.buildHeroCubeGlb()
                } else {
                    val detail = (err as? Throwable)?.message ?: err?.toString() ?: "fetch failed"
                    throw GltfParseException("H5 asset missing: $cleaned ($detail)")
                }
            }
        }
        val uri = content.fileUri
        if (!uri.isNullOrBlank()) {
            if (uri.startsWith("http://", ignoreCase = true) ||
                uri.startsWith("https://", ignoreCase = true)
            ) {
                return Promise.reject(GltfParseException("H5 fileUri http forbidden (ADR 0020): $uri"))
            }
            return fetchBytes(uri)
        }
        return Promise.reject(GltfParseException("HolmModelContent has no bytes/asset/fileUri"))
    }

    private fun fetchBytes(path: String): Promise<ByteArray> {
        return window.fetch(path).then { response ->
            val res = response.unsafeCast<Response>()
            if (!res.ok) {
                throw GltfParseException("fetch HTTP ${res.status}: $path")
            }
            res.arrayBuffer()
        }.then { ab ->
            arrayBufferToByteArray(ab.unsafeCast<ArrayBuffer>())
        }
    }

    private fun arrayBufferToByteArray(ab: ArrayBuffer): ByteArray {
        val u8 = Uint8Array(ab)
        val len = u8.length
        val out = ByteArray(len)
        val dyn = u8.asDynamic()
        for (i in 0 until len) {
            out[i] = (dyn[i] as Number).toByte()
        }
        return out
    }
}
