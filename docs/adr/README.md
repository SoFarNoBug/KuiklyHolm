# ADR

重大决策先写 ADR，再改 docs/product/00-context-and-decisions.md 对应条目（若冲突以 00 为产品 SoT，ADR 解释背景）。

门槛：改变对外 API、重量档（Lite/Standard/Full）、命名、仓库形态、或跨端能力子集时新增。

| ID | 标题 |
| --- | --- |
| [0001](./0001-repo-shape-fat-core-shell.md) | 仓库形态 · 胖 Core 同构空壳 |
| [0002](./0002-stage0-lite-android-h5.md) | Stage 0 选 Lite(A) · Android+H5 霜化尖刺 |
| [0003](./0003-lite-official-five-scenes.md) | Lite 官方五席锁定 + Attr 可读改名 |
| [0004](./0004-lite-vs-standard-dualtrack.md) | DualTrack 收口 · Keep Lite（O0 不改） |
| [0005](./0005-business-fragment-shadertoy-shell.md) | 业务片元 Attr + Shadertoy 薄壳 |
| [0006](./0006-android-runtime-surface-session.md) | Android Runtime · SurfaceView + Kotlin 外泵 |
| [0007](./0007-holm-as-generic-render-substrate.md) | **身份** · 通用渲染底座（shader-in / surface-out） |
| [0008](./0008-holm-core-gl-wgpu-packages.md) | **目标拆包** · holm-core / holm-renderer-gl / holm-renderer-wgpu + Provider |
| [0009](./0009-ios-runtime-cametallayer-objc-session.md) | iOS Runtime · CAMetalLayer + ObjC 外泵 Session |
| [0010](./0010-ohos-runtime-xcomponent-session.md) | 鸿蒙 Runtime · XComponent + 外泵 Session |
| [0011](./0011-ohos-lite-egl-xcomponent.md) | 鸿蒙 Lite · 同 XComponent + EGL/GLES |
| [0012](./0012-on-shader-compiled-timing.md) | **onShaderCompiled** · 片元装载双耗时回调 |
| [0013](./0013-kmp-wgpu-primitives-under-holm.md) | **kmp-wgpu** · 通用 wgpu 原语层（Holm 之下） |
| [0014](./0014-rename-holm-runtime-to-kmp-wgpu-native.md) | **改名** · 消灭 holm-runtime → kmp-wgpu-native / kmp_wgpu_*（ABI break） |
| [0015](./0015-kmp-wgpu-independent-repo.md) | **独立仓** · kmp-wgpu 拆仓发版 + FullscreenPass + 关 O7 |
| [0016](./0016-holm-content-and-minimal-gltf.md) | **Content + 最小 glTF** · 废 fragmentShader Attr；关 O3（非 Filament） |
| [0017](./0017-kmp-draco-under-holm-gltf.md) | **kmp-draco** · holm-gltf 硬依赖独立 Draco 原语（KHR_draco） |
| [0018](./0018-ios-model-dual-track-present.md) | **iOS Model** · GLES + Metal ModelPass 双轨 |
| [0019](./0019-ohos-model-dual-track-present.md) | **鸿蒙 Model** · GLES + wgpu ModelPass 双轨 |
| [0020](./0020-h5-model-dual-track-present.md) | **H5 Model** · WebGL + wasm ModelPass 双轨 |
