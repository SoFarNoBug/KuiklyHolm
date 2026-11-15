package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.HolmExecutor

/**
 * @CreateDate: 2026/11/15
 * @Author: jlj
 * @Description: Demo 双轨样本目录（霓虹隧道 + Raymarching 基础图形 + 故意编不过）；chip 切换用。
 */
enum class DemoSample(
    val id: String,
    val label: String,
    val jsonExample: Int,
) {
    TUNNEL("tunnel", "隧道", 1),
    /** 展示名对齐 Shadertoy「Raymarching - 基础图形」；id 仍 iq_primitives。 */
    IQ_PRIMITIVES("iq_primitives", "Raymarching - 基础图形", 2),
    /** 故意编不过：展示 onError 捕获（Runtime=WGSL 保留字 ref；Lite=未定义标识符）。 */
    BAD_COMPILE("bad_compile", "故意失败", 3),
    ;

    fun liteBody(): String = when (this) {
        TUNNEL -> NeonTunnelShaders.LITE_BODY
        IQ_PRIMITIVES -> IqPrimitivesShaders.LITE_BODY
        BAD_COMPILE -> BadCompileShaders.LITE_BODY
    }

    fun runtimeFs(): String = when (this) {
        TUNNEL -> NeonTunnelShaders.RUNTIME_FS
        IQ_PRIMITIVES -> IqPrimitivesShaders.RUNTIME_FS
        BAD_COMPILE -> BadCompileShaders.RUNTIME_FS
    }

    fun fragmentFor(executor: String): String =
        if (HolmExecutor.isRuntime(executor)) runtimeFs() else liteBody()
}

object DemoSampleCatalog {
    val ALL: List<DemoSample> = DemoSample.entries
}

/**
 * @CreateDate: 2026/11/15
 * @Author: jlj
 * @Description: 故意非法片元，仅供 Demo 展示 SHADER_* onError；勿当正样本。
 */
object BadCompileShaders {
    val LITE_BODY: String = """
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // undefined_holm_demo_error：故意让 GLES 编译失败 → onError SHADER_COMPILE
    fragColor = vec4(undefined_holm_demo_error, 0.0, 0.0, 1.0);
}
""".trimIndent()

    val RUNTIME_FS: String = """
@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    // WGSL 保留字 ref：故意让 naga/wgpu 失败 → onError SHADER_CUSTOM
    let ref = 1.0;
    return vec4f(ref, 0.0, 0.0, 1.0);
}
""".trimIndent()
}
