# 术语表（Holm）

| 术语 | 含义 |
| --- | --- |
| **Holm** | 通用渲染底座（shader-in / surface-out）；不绑定单一业务（ADR 0007） |
| **holm-core** | 目标骨架包：岛 API / 生命周期 / Provider 槽（ADR 0008） |
| **holm-gl** | 目标轻量 GL 族后端包（Android/iOS GLES、H5 WebGL…） |
| **holm-wgpu** | 目标 wgpu Runtime 后端包 |
| **Provider** | 必须显式注册的后端实现；未设则失败 |
| **渲染底座 / shader-in / surface-out** | 入着色器，出表面帧 |
| **D12** | 身份：通用渲染底座 |
| **D13** | 目标三包 + Provider（ADR 0008） |
| **HolmSurface / KRHolmSurface** | API / viewName |
| **岛** | 布局决定位置与大小的局部 GPU 区域 |
| **隧道样本** | Demo 主推宿主片元示例 |
| **五席（已删源码）** | 历史 frost/glow/…；官方实现已移除；Demo 主推隧道示例1 |
| **fragmentShader / Shadertoy 薄壳** | 宿主喂片元（ADR 0005） |
| **setCustomFs** | Runtime 热换 WGSL 片元 |
| **Android Runtime Session** | arm64 `.so` + SurfaceView 外泵（ADR 0006） |
| **急拆** | 毁 Surface 前立刻 destroy Session |
| **[holm-perf]** | Demo JSONL 采集前缀 |
| **示例消费者** | 如 ParticleEngine——非身份绑定 |

更多：`../00-context-and-decisions.md` · [ADR 0007](../../adr/0007-holm-as-generic-render-substrate.md) · [ADR 0008](../../adr/0008-holm-core-gl-wgpu-packages.md)。
