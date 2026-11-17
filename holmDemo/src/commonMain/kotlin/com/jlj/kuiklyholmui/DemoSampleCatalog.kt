package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.HolmExecutor

/**
 * @CreateDate: 2026/11/15
 * @Author: jlj
 * @Description: Demo 双渲染后端样本目录（隧道 / Raymarching / 程序化海洋 / 电缆巢 / 故意失败）；chip 切换用。
 */
enum class DemoSample(
    val id: String,
    val label: String,
    val jsonExample: Int,
) {
    TUNNEL("tunnel", "隧道", 1),
    /** 展示名对齐 Shadertoy「Raymarching - 基础图形」；id 仍 iq_primitives。 */
    IQ_PRIMITIVES("iq_primitives", "Raymarching - 基础图形", 2),
    /** afl_ext 程序化海面（MIT · shadertoy.com/view/MdXyzX）。 */
    PROCEDURAL_OCEAN("procedural_ocean", "程序化海洋", 4),
    /** mrange 电缆巢（CC0 · shadertoy.com）。 */
    CABLE_NEST("cable_nest", "电缆巢", 5),
    /** 故意编不过：展示 onError 捕获（renderer-wgpu=WGSL 保留字 ref；renderer-gl=未定义标识符）。 */
    BAD_COMPILE("bad_compile", "故意失败", 3),
    ;

    fun glBody(): String = when (this) {
        TUNNEL -> NeonTunnelShaders.GL_BODY
        IQ_PRIMITIVES -> IqPrimitivesShaders.GL_BODY
        PROCEDURAL_OCEAN -> ProceduralOceanShaders.GL_BODY
        CABLE_NEST -> CableNestShaders.GL_BODY
        BAD_COMPILE -> BadCompileShaders.GL_BODY
    }

    fun wgpuFs(): String = when (this) {
        TUNNEL -> NeonTunnelShaders.WGPU_FS
        IQ_PRIMITIVES -> IqPrimitivesShaders.WGPU_FS
        PROCEDURAL_OCEAN -> ProceduralOceanShaders.WGPU_FS
        CABLE_NEST -> CableNestShaders.WGPU_FS
        BAD_COMPILE -> BadCompileShaders.WGPU_FS
    }

    /** 选中时展示的版权小字；无则 null。 */
    fun uiCredit(): String? = when (this) {
        IQ_PRIMITIVES -> IqPrimitivesShaders.UI_CREDIT
        PROCEDURAL_OCEAN -> ProceduralOceanShaders.UI_CREDIT
        CABLE_NEST -> CableNestShaders.UI_CREDIT
        else -> null
    }

    fun fragmentFor(executor: String): String =
        if (HolmExecutor.isWgpu(executor)) wgpuFs() else glBody()
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
    val GL_BODY: String = """
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    // undefined_holm_demo_error：故意让 GLES 编译失败 → onError SHADER_COMPILE
    fragColor = vec4(undefined_holm_demo_error, 0.0, 0.0, 1.0);
}
""".trimIndent()

    val WGPU_FS: String = """
@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    // WGSL 保留字 ref：故意让 naga/wgpu 失败 → onError SHADER_CUSTOM
    let ref = 1.0;
    return vec4f(ref, 0.0, 0.0, 1.0);
}
""".trimIndent()
}
