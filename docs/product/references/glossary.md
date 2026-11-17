# 术语表（Holm）

| 术语 | 含义 |
| --- | --- |
| **Holm** | 通用渲染底座（shader-in / surface-out）；不绑定单一业务（ADR 0007） |
| **holm-core** | 骨架包：岛 API / 生命周期 / Provider 槽（ADR 0008） |
| **holm-renderer-gl** | 轻量 GL 族后端包（Android/iOS GLES、H5 WebGL…）；原草案名 `holm-gl` |
| **holm-renderer-wgpu** | Holm 的 wgpu **Provider / 岛 Present 薄适配**；依赖 `kmp-wgpu`（+可选 surface）；原草案名 `holm-wgpu` |
| **kmp-wgpu** | 与 Holm/Kuikly 无关的跨端 wgpu **原语库**（Kotlin 脸；近 1:1 wgpu）（ADR 0013） |
| **kmp-wgpu-surface** | 可选 Present/Surface + **FullscreenPass**；纯算力可不依赖（ADR 0013 / 0015） |
| **FullscreenPass** | 通用全屏片元 Present（surface 模块）；无片元只清屏；非 Holm Session（ADR 0015） |
| **kmp_wgpu_native** | Rust+wgpu 实现 crate；独立仓发版见 ADR 0015 |
| **liquid / scene / ClearTri** | **已废**；不得再进 kmp-wgpu 产品面（ADR 0015） |
| **setFragmentShader** | FullscreenPass 热换 WGSL 片元（取代 setCustomFs 产品名） |
| **Provider** | 必须显式注册的后端实现；未设则失败 |
| **渲染底座 / shader-in / surface-out** | 入着色器，出表面帧 |
| **D12** | 身份：通用渲染底座 |
| **D13** | 目标三包 + Provider（ADR 0008） |
| **HolmSurface / KRHolmSurface** | API / viewName |
| **岛** | 布局决定位置与大小的局部 GPU 区域 |
| **隧道样本** | Demo 主推宿主片元示例 |
| **五席（已删源码）** | 历史 frost/glow/…；官方实现已移除；Demo 主推隧道示例1 |
| **fragmentShader / Shadertoy 薄壳** | 宿主喂片元（ADR 0005） |
| **setCustomFs** | 历史名；现走 `setFragmentShader` / Holm 适配转发 |
| **包壳耗时 / wrapMs** | 片元装载中补壳墙钟；renderer-wgpu 恒 0（ADR 0012） |
| **编译耗时 / compileMs** | renderer-gl compile+link 或 renderer-wgpu pipeline 墙钟（ADR 0012） |
| **onShaderCompiled** | 每次装载尝试结束回传 ok + 双耗时 + executor；可与 onError 双发 |
| **Android renderer-wgpu Session** | arm64 `.so` + SurfaceView 外泵（ADR 0006） |
| **iOS renderer-wgpu Session** | xcframework + 容器内 `CAMetalLayer` + ObjC 主线程外泵 + C ABI（ADR 0009）；先于 iOS renderer-gl |
| **鸿蒙 renderer-wgpu Session** | `libkmp_wgpu_native.so` + `libholm_xc.so`（XComponent）+ C ABI（ADR 0010） |
| **鸿蒙 renderer-gl GLES** | 同 XComponent 上 EGL/GLES Shadertoy（ADR 0011）；`onReady.backend=gles2-ohos` |
| **gles2-ohos** | 鸿蒙 renderer-gl 后端 id（与 `gles2` / `gles2-ios` 区分） |
| **急拆** | 毁 Surface 前立刻 destroy Session |
| **[holm-perf]** | Demo JSONL 采集前缀 |
| **示例消费者** | 如 ParticleEngine——非身份绑定 |

更多：`../00-context-and-decisions.md` · [ADR 0007](../../adr/0007-holm-as-generic-render-substrate.md) · [ADR 0008](../../adr/0008-holm-core-gl-wgpu-packages.md)。
