# Review · holm-three-package-split

- 结论：有条件通过
- 依据：Plan「Holm 三包物理拆（Android+H5）+ kmp-wgpu 抽芯收尾」；changelog「Holm 三包物理拆 · Android+H5 + kmp-wgpu 抽芯收尾」
- 日期：2026-11-16
- 范围：`:holm-core` / `:holm-renderer-gl` / `:holm-renderer-wgpu`；`:KuiklyHolmCore` umbrella；Provider SPI；Android/H5 `KRHolmSurfaceView` → `HolmFullscreenPass`；`HolmAndroidJni` 迁出 `AndroidWgpuPlatform`；ADR 0008 包名。不含 iOS/Ohos 物理拆、不含 native 符号 rename。

## P0

无。

## P1

### P1-plan-soft-drift（未关）
- 证据：`HolmBackendProvider` 仅 `id`/`executorKey` 标记；`KRHolmSurfaceView` 仍硬编码双轨（Android/JS）；Demo 未显式 `HolmProviders.register`（依赖 `HolmUmbrellaBootstrap`）；Plan §1「创建/绑定岛 View」与 §2「View 留 umbrella」落地偏薄
- 影响：Gradle 三包与注册门闸已有；Provider 尚未成为真正后端分发面；「显式注册」演示路径未走 Demo
- 处置：建议 — 下波把 Lite/Runtime 半部收到 Provider 实现，或 Demo 启动显式 register 并在 ADR 写明 umbrella 例外

### P1-doc-drift（未关）
- 证据：[`docs/product/README.md`](../product/README.md) 仍写 `holm-gl` / `holm-wgpu`、且「物理拆包另单」；ADR 0008 Decision §3「宿主必须显式注册」与 umbrella 静默 `register`+`setDefault` 未在产品 README/决策表写清例外
- 影响：下任/外部读者仍按旧包名与「必手写 register」理解
- 处置：必修 — 对齐 README / D11 表述与 umbrella 例外一句

### P1-device-verify（未关）
- 缺口：代码审无法替代 DualTrack Present 回归（FullscreenPass + 新 JNI 类名）与 Provider 缺省 BLOCKED 观感
- 可执行档：[`device-check-list/holm-three-package-split.md`](device-check-list/holm-three-package-split.md)
- 处置：必修（放行条件）— 勾 Android+H5 DualTrack 主路径

## P2

### P2-provider-ready-backend（未关）
- 证据：Android `applyExecutorMode` Provider 失败仍 `fireReady()`；`fireReady` 在 `executor=lite` 时回报 `backend=gles2`（`KRHolmSurfaceView.kt`），与已发 `BLOCKED` 双源；H5 同路径强制 `_backend=wgpu-blocked`
- 处置：建议 — Provider 失败时 Android 与 H5 对齐诚实 backend

### P2-h5-worker-dual-path（未关）
- 证据：H5 主线程走 `HolmFullscreenPass`；Worker 仍 `applyRuntimeShaders(session)` + `HolmRuntimeNative.sessionSet*`
- 处置：可选 — 与 Plan「适配层统一」下波收敛

### P2-worktree-noise（未关）
- 证据：同树另有 `iosApp` / `ohosApp` / `static_server` / `_holm-now.jpeg` 等与本单无关改动
- 处置：可选 — 提交前勿带入

## 规范候选观察

- [ ] Gradle 物理拆后若 umbrella 自动 `register`+`setDefault`，ADR/产品文须写明「兼容例外」，否则与「禁止仅靠 classpath 静默默选」冲突｜2026-11-16｜自评：未达标（本单 P1-doc-drift）

## 真机

挂 `P1-device-verify`。可执行档：[`device-check-list/holm-three-package-split.md`](device-check-list/holm-three-package-split.md)。

## Plan 对齐

符合：三包名 `holm-renderer-*`、Android+H5 Gradle 拆、Provider 门闸、Core umbrella、FullscreenPass 接通、Holm JNI 迁出 kmp-wgpu、不做 rename/iOS·Ohos 拆。软偏：Provider SPI 偏薄、Demo 未显式 register。

## changelog

已见 `docs/specs/dualtrack-lite-vs-standard/changelog.md`「Holm 三包物理拆 · Android+H5 + kmp-wgpu 抽芯收尾」；请码农确认仍在（审核员不代写）。

## 待补桶

无。
