# CONTEXT · Holm（kuikly-holm）

## 一句话

**通用渲染底座**：业务定岛的位置与大小，传入上色 shader，局部 GPU 岛出帧；目标拆成 `holm-core` / `holm-gl` / `holm-wgpu`，须显式 Provider。

## Language

**Holm**：
通用 shader-in / surface-out 渲染底座产品名；不绑定单一业务。
_Avoid_：玻璃特效产品、粒子引擎

**岛 / HolmSurface / KRHolmSurface**：
页面上由布局决定位置与大小的局部 GPU 表面及 API / viewName。
_Avoid_：整页自绘引擎

**holm-core**：
目标骨架依赖：统一岛 API、生命周期、喂 shader / 选后端；不含画像素实现。
_Avoid_：把特效目录塞进 core

**holm-gl**：
目标轻量 GL 族后端依赖（Android/iOS GLES、H5 WebGL 等）。
_Avoid_：Lite（易混旧特效产品档）

**holm-wgpu**：
目标 wgpu Runtime 后端依赖（含 `.so` / wasm 等按端产物）。
_Avoid_：用 GLES 冒充 wgpu 通面

**Provider**：
宿主必须显式注册的后端实现入口；未设置则失败。
_Avoid_：仅靠 classpath 静默默选

**隧道样本**：
Demo 当前主推的宿主片元示例（霓虹隧道）。
_Avoid_：官方五席产品目录

**五席（已删源码）**：
历史 frost/glow/liquid_approx/gradient_bg/fullscreen_warp；官方实现已从 Core/runtime/iOS/Demo 移除。
_Avoid_：官方特效完成态 / scene=frost 出画

**iOS Runtime Session**：
iOS 上 wgpu Session 通面形态：容器岛内 `CAMetalLayer` 子 View + ObjC 主线程外泵 + C ABI（ADR 0009）。
_Avoid_：用 GLES 冒充 Runtime；KN 拥有 DisplayLink；MTKView 抢 wgpu drawable

**iOS Lite GLES**：
同容器下 `CAEAGLLayer` 子 View + Shadertoy `fragmentShader`；`onReady.backend=gles2-ios`。
_Avoid_：根 layer 热换 CAEAGL；用 Lite 冒充 wgpu

## SoT

| 主题 | 路径 |
| --- | --- |
| 决策 / 身份 | [`docs/product/00-context-and-decisions.md`](docs/product/00-context-and-decisions.md) |
| 身份 ADR | [`docs/adr/0007-holm-as-generic-render-substrate.md`](docs/adr/0007-holm-as-generic-render-substrate.md) |
| 三包 + Provider | [`docs/adr/0008-holm-core-gl-wgpu-packages.md`](docs/adr/0008-holm-core-gl-wgpu-packages.md) |
| iOS Runtime | [`docs/adr/0009-ios-runtime-cametallayer-objc-session.md`](docs/adr/0009-ios-runtime-cametallayer-objc-session.md) |
| 架构 | [`docs/product/01-architecture.md`](docs/product/01-architecture.md) |
| 术语 | [`docs/product/references/glossary.md`](docs/product/references/glossary.md) |
| AI 工作流 | [`.cursor/agents-guide.md`](.cursor/agents-guide.md) |

## 工程壳（现状 · 尚未物理拆包）

| 项 | 值 |
| --- | --- |
| 根工程 | `KuiklyHolmUI` |
| 库 | `:KuiklyHolmCore`（对应未来 holm-core+后端暂合居） |
| Runtime | `holm-runtime`（对应未来 holm-wgpu 产物） |
| Demo | `:holmDemo` · 主推 `holm_runtime_m0` 隧道 |

## 当前状态

- 身份：ADR 0007；目标拆包：ADR 0008（文档已定，Gradle 未拆）
- Demo：Home 仅「示例1 / 双轨」；官方五席源码已删
- iOS：ADR 0009 Runtime Metal + Lite GLES 子树均已落地（`gles2-ios` / `wgpu-m0*`）
- 后单：真拆三依赖、Runtime 产物与源同步门禁、`holm_api` 规范对齐
