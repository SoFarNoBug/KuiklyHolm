# CONTEXT · Holm（kuikly-holm）

## 一句话

**通用渲染底座**：业务定岛的位置与大小，挂上 `HolmContent`（片元或最小 glTF 模型）出帧；目标拆成 `holm-core` / `holm-renderer-gl` / `holm-renderer-wgpu`（+ 可选 `holm-gltf`），须显式 Provider。其下为与 Holm 无关的 `kmp-wgpu` 原语层（ADR 0013）。

## Language

**Holm**：
通用 shader-in / surface-out 渲染底座产品名；不绑定单一业务。
_Avoid_：玻璃特效产品、粒子引擎；通用 GPU 算力库

**岛 / HolmSurface / KRHolmSurface**：
页面上由布局决定位置与大小的局部 GPU 表面及 API / viewName。
_Avoid_：整页自绘引擎

**holm-core**：
目标骨架依赖：统一岛 API、生命周期、挂 `HolmContent` / 选后端；不含画像素实现。
_Avoid_：把特效目录或 glTF 解析塞进 core

**HolmContent**：
交给岛的「画什么」交付物；实现为 `HolmShaderContent` 或 `HolmModelContent`。
_Avoid_：Session（易与旧 Runtime Session 混）；在 Surface 上并列 fragmentShader+modelUri

**HolmShaderContent**：
片元内容（源码字符串 + 可搬迁的 knobs）；取代对外 `fragmentShader` Attr。
_Avoid_：在 ShaderContent 上假装有真透视相机

**HolmModelContent**：
最小 glTF/glb 英雄模型内容；真相机、可选轨道手势、业务可配灯/材质覆盖。
_Avoid_：游戏场景图；Filament；库内默认自转；http 加载

**holm-gltf**：
可选 Kotlin 模块：CPU 侧解析 glb → 网格/材质数据；不引则无模型能力。
_Avoid_：把解析放进 kmp-wgpu-native；强制进 umbrella 必选依赖

**holm-renderer-gl**：
轻量 GL 族后端依赖（Android/iOS GLES、H5 WebGL 等）；含片元 Present，并将含最小网格 Model Present。（原草案名 `holm-gl`）
_Avoid_：Lite（易混旧特效产品档）

**holm-renderer-wgpu**：
Holm 的 wgpu Provider / 岛 Present 薄适配；依赖 `kmp-wgpu`（+可选 surface）；片元与最小网格 Model Present。（原草案名 `holm-wgpu`）
_Avoid_：把通用原语或算力 API 塞进 holm-renderer-wgpu；用 GLES 冒充 wgpu 通面

**kmp-wgpu**：
与 Holm/Kuikly 无关的跨端 wgpu 原语库（Kotlin 脸；近 1:1 wgpu 概念）。
_Avoid_：Holm Session；Kuikly 桥；官方算法/特效目录；第二图形后端

**kmp-wgpu-surface**：
可选 Present/Surface 模块；含 **FullscreenPass**（换片元 + tick）；无窗口算力可不依赖。
_Avoid_：把 Present 塞进 kmp-wgpu core 必选路径；liquid/scene/ClearTri（已废）

**FullscreenPass**：
通用全屏片元 Present（属 `kmp-wgpu-surface`）；`setFragmentShader` / tick / playing；无片元只清屏。
_Avoid_：Holm Session；liquid/scene；amber ClearTri 进库默认

**kmp_wgpu_native**：
Rust+wgpu 实现 crate；长期独立仓 `~/Studio/kmp/kmp-wgpu`（ADR 0015）；C ABI `kmp_wgpu_*`（ADR 0014）。
_Avoid_：holm-runtime / holm_runtime / HolmRuntime / holm_fullscreen（已废旧名）

**Provider**：
宿主必须显式注册的后端实现入口；未设置则失败。
_Avoid_：仅靠 classpath 静默默选

**隧道样本**：
Demo 当前主推的宿主片元示例（霓虹隧道）。
_Avoid_：官方五席产品目录

**包壳耗时**：
本次片元装载中，把宿主正文补成可编译源码所花的时间（renderer-gl 的 Shadertoy 薄壳等）。
_Avoid_：和编译耗时混成一个数；把首帧绘制算进来

**编译耗时**：
本次片元装载中，驱动编译/链接或 renderer-wgpu 建 pipeline 所花的时间。
_Avoid_：和包壳耗时混成一个数；把首帧绘制算进来

**onShaderCompiled**：
岛上每次片元装载尝试结束（含未进入包壳/编译的拒绝、以及 Surface/上下文恢复触发的重编）向业务回传成败与包壳/编译耗时的独立事件；与 `onError` 可双发。
_Avoid_：onShaderReady；塞进 onReady / onPerf；用缺省回调表示「尝试已结束」；仅业务改 Attr 才回调

**五席（已删源码）**：
历史 frost/glow/liquid_approx/gradient_bg/fullscreen_warp；官方实现已从 Core/runtime/iOS/Demo 移除。
_Avoid_：官方特效完成态 / scene=frost 出画

**iOS renderer-wgpu Session**：
iOS 上 wgpu Session 通面形态：容器岛内 `CAMetalLayer` 子 View + ObjC 主线程外泵；Present 经 `HolmWgpuIosBridge` → kmp-wgpu cinterop → C ABI（ADR 0009；壳不直调 Session）。
_Avoid_：用 GLES 冒充 renderer-wgpu；KN 拥有 DisplayLink；MTKView 抢 wgpu drawable

**iOS renderer-gl GLES**：
同容器下 `CAEAGLLayer` 子 View + Shadertoy `fragmentShader`；`onReady.backend=gles2-ios`。
_Avoid_：根 layer 热换 CAEAGL；用 GLES 冒充 wgpu

**鸿蒙 renderer-wgpu Session**：
鸿蒙上 wgpu Session 通面形态：ETS `KRHolmSurface` 内嵌 `XComponent` + NAPI `OnFrame` 外泵 + C ABI（ADR 0010）。
_Avoid_：用 GLES 冒充 renderer-wgpu；KN 拥有帧泵；把 Present 耦进 `libentry.so`

**鸿蒙 renderer-gl GLES**：
同 XComponent 上 EGL/GLES Shadertoy（ADR 0011）；`onReady.backend=gles2-ohos`；与 renderer-wgpu 互斥切轨。
_Avoid_：`LITE_PENDING` 冒充完成；用 GLES 冒充 wgpu；另挂第二 XComponent 当默认

## SoT

| 主题 | 路径 |
| --- | --- |
| 决策 / 身份 | [`docs/product/00-context-and-decisions.md`](docs/product/00-context-and-decisions.md) |
| 身份 ADR | [`docs/adr/0007-holm-as-generic-render-substrate.md`](docs/adr/0007-holm-as-generic-render-substrate.md) |
| 三包 + Provider | [`docs/adr/0008-holm-core-gl-wgpu-packages.md`](docs/adr/0008-holm-core-gl-wgpu-packages.md) |
| kmp-wgpu 原语层 | [`docs/adr/0013-kmp-wgpu-primitives-under-holm.md`](docs/adr/0013-kmp-wgpu-primitives-under-holm.md) |
| 消灭 holm-runtime 命名 | [`docs/adr/0014-rename-holm-runtime-to-kmp-wgpu-native.md`](docs/adr/0014-rename-holm-runtime-to-kmp-wgpu-native.md) |
| kmp-wgpu 独立仓 | [`docs/adr/0015-kmp-wgpu-independent-repo.md`](docs/adr/0015-kmp-wgpu-independent-repo.md) |
| Content + 最小 glTF | [`docs/adr/0016-holm-content-and-minimal-gltf.md`](docs/adr/0016-holm-content-and-minimal-gltf.md) |
| iOS renderer-wgpu | [`docs/adr/0009-ios-runtime-cametallayer-objc-session.md`](docs/adr/0009-ios-runtime-cametallayer-objc-session.md) |
| 鸿蒙 renderer-wgpu | [`docs/adr/0010-ohos-runtime-xcomponent-session.md`](docs/adr/0010-ohos-runtime-xcomponent-session.md) |
| 鸿蒙 renderer-gl | [`docs/adr/0011-ohos-lite-egl-xcomponent.md`](docs/adr/0011-ohos-lite-egl-xcomponent.md) |
| 架构 | [`docs/product/01-architecture.md`](docs/product/01-architecture.md) |
| 术语 | [`docs/product/references/glossary.md`](docs/product/references/glossary.md) |
| AI 工作流 | [`.cursor/agents-guide.md`](.cursor/agents-guide.md) |

## 工程壳（现状 · Android+H5+iOS+Ohos 三包 Present 对齐）

| 项 | 值 |
| --- | --- |
| 根工程 | `KuiklyHolmUI` |
| 三包 | `:holm-core` / `:holm-renderer-gl` / `:holm-renderer-wgpu` |
| Umbrella | `:KuiklyHolmCore`（`api` 三包；iOS/Ohos 已接 renderer + Bridge） |
| Native | `kmp-wgpu-native/`（crate `kmp_wgpu_native`；产物 `libkmp_wgpu_native.*`）；Kotlin `:kmp-wgpu` + `:kmp-wgpu-surface`；Holm 适配 `HolmFullscreenPass` / `HolmWgpuNative` |
| Demo | `:holmDemo` · 主推 `holm_dualtrack` 双渲染后端 + 多样本 chip（隧道 / IQ / 程序化海洋 / 电缆巢） |

## 当前状态

- 身份：ADR 0007；目标拆包：ADR 0008（`holm-renderer-*`；四端 Present 已对齐；Maven/拆仓另单）
- 下沉原语：ADR 0013 v1 Present + **v1.1 headless Compute+回读**（`:kmp-wgpu`；四端）；目录/ABI 物理改名见 **ADR 0014**
- Demo：Home「示例 / 双渲染后端」(`holm_dualtrack`) +「kmp-wgpu Compute 回读」；页内 chip 切隧道 / IQ / 程序化海洋 / 电缆巢；官方五席源码已删
- iOS：ADR 0009 泵仍 ObjC；Present 经 `HolmWgpuIosBridge` → kmp-wgpu；renderer-gl GLES 子树（`gles2-ios` / `wgpu-m0*`）
- 鸿蒙：ADR 0010 renderer-wgpu + ADR 0011 renderer-gl（同 XC EGL，`gles2-ohos`）；真机隧道/互切待勾
- 独立仓：ADR 0015 已落地（`~/Studio/kmp/kmp-wgpu`；Maven `0.1.0`；关 O7；真机表已勾）
- kmp-wgpu 分发：CocoaPods / ohpm / npm / GitHub Release **0.1.x 均已发**（npm=`@sofarnobug/kmp-wgpu-native@0.1.0`；ohpm=`@jlj/…@0.1.1`；见 kmp-wgpu `CHANGELOG.md`）
- `holm_api` L3：已与 ADR 0007/0008/0013 对齐（2026-11-18）
- **下波（分支 `feat/holm-model-content-android`）**：ADR 0016 — `HolmContent` 契约；自研最小 glTF；关 O3（非 Filament）；Model 真画 Android 优先
