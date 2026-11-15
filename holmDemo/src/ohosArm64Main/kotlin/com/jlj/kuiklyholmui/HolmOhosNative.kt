package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.HolmOhos
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

/**
 * NAPI 入口（须在 shared 产物模块导出）。
 * 目录注入 + 遗留 magnifier stub（壳仍链符号，Holm 无像素放大镜）。
 */
object HolmOhosNative {
    fun configure(resfileDir: String, cacheDir: String) {
        HolmOhos.init(resfileDir, cacheDir)
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("holm_set_ohos_dirs")
fun holmSetOhosDirs(resfileDir: CPointer<ByteVar>?, cacheDir: CPointer<ByteVar>?) {
    val res = resfileDir?.toKString().orEmpty()
    val cache = cacheDir?.toKString().orEmpty()
    if (res.isEmpty() && cache.isEmpty()) {
        return
    }
    HolmOhos.init(res, cache)
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("holm_set_resfile_dir")
fun holmSetResfileDir(dir: CPointer<ByteVar>?) {
    if (dir == null) {
        return
    }
    HolmOhos.init(dir.toKString())
}

/** 遗留 NAPI 符号：Holm 无放大镜像素，恒返回 0。 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@CName("holm_magnifier_blit_snapshot")
fun holmMagnifierBlitSnapshot(
    generation: CPointer<IntVar>?,
    width: CPointer<IntVar>?,
    height: CPointer<IntVar>?,
    rgba: CPointer<UByteVar>?,
    rgbaLen: CPointer<IntVar>?,
    borderWidth: CPointer<FloatVar>?,
    borderColor: CPointer<IntVar>?,
    showCross: CPointer<IntVar>?,
    crossColor: CPointer<IntVar>?,
    crossLineWidth: CPointer<FloatVar>?,
    crossLineLength: CPointer<FloatVar>?,
    showCropLineMirror: CPointer<IntVar>?,
    cropLineColor: CPointer<IntVar>?,
    cropLineWidth: CPointer<FloatVar>?,
    touchViewX: CPointer<FloatVar>?,
    touchViewY: CPointer<FloatVar>?,
    viewPointsXy: CPointer<FloatVar>?,
    viewPointsLen: CPointer<IntVar>?,
): Int {
    if (generation != null) generation.pointed.value = 0
    if (width != null) width.pointed.value = 0
    if (height != null) height.pointed.value = 0
    if (rgbaLen != null) rgbaLen.pointed.value = 0
    if (viewPointsLen != null) viewPointsLen.pointed.value = 0
    // 未使用缓冲指针；保持签名与 napi_init 一致
    listOf(
        rgba, borderWidth, borderColor, showCross, crossColor, crossLineWidth, crossLineLength,
        showCropLineMirror, cropLineColor, cropLineWidth, touchViewX, touchViewY, viewPointsXy,
    )
    return 0
}
