# Review · holm-ios-glb

- 结论：通过
- 依据：Plan `holm_ios_glb_01211488`（iOS glTF/GLB 双轨 Present）；ADR 0018；changelog `docs/specs/kuikly-dev-ios/changelog.md` + `docs/specs/holm-content-gltf/changelog.md`；用户确认 iPhone Duo 模拟器验证 OK
- 日期：2026-11-19
- diff：`holm-gltf` ios Draco；`HolmIosModelBridge`；`KRHolmGlesModelHostView`；`KRHolmSurface` model 路径；`HolmModelMeshUpload`→mobileMain；kmp-wgpu iOS `ModelPass` + xcframework rebuild；iosApp Draco/models 链接；guide/Demo/ADR
- 审查回执行：2026-11-19 码农已关 P1-concurrency + 全部 P2；同日用户确认 Duo 冒烟 → **关 `P1-device-verify`**

## P0

无。

## P1

### P1-device-verify iOS Model 双轨出画 / Tokyo / 换资产 / 灯光
- 处置：**已关**（2026-11-19）— 用户确认 iPhone Duo Simulator 验证 OK；表见 [`device-check-list/holm-ios-glb.md`](device-check-list/holm-ios-glb.md)（#1–#9、#11 过；#10 负向跳过）

### P1-concurrency HolmIosModelBridge 全局单例
- 处置：**已关**（2026-11-19）— `createSession`/`destroySession` 按岛分表

## P2

### P2-consume-snap-no-host
- 处置：**已关** — 无 host / setMeshes 失败时 `requeueSnapshot`

### P2-backend-id-gles-model
- 处置：**已关** — Model GLES `gles2-model`

### P2-ios-native-symlink-local
- 处置：**已关** — guide「iOS xcframework 旁路」`ln -sfn`

### P2-commonMain-holm-gltf-weight
- 处置：**已关** — `mobileMain` + MeshUpload；jsMain 不依赖 holm-gltf

## 规范候选观察

- iOS Model Present 状态宜按岛实例分表，避免 `object` Bridge 全局单例｜可抄码农 ios 池
- 本地 xcframework 旁路路径（`kmp-*-native` symlink）应在 guide 写清｜可抄文档池

## 真机

`P1-device-verify` 已关。可执行档：[`device-check-list/holm-ios-glb.md`](device-check-list/holm-ios-glb.md)。机型：iPhone Duo Simulator（iOS 27.1）；用户确认验证 OK。

## changelog

已见审查回执行 · 通过（`P1-device-verify` 关）。kmp-wgpu 仓 `CHANGELOG.md` Unreleased 已记 iOS ModelPass。

## 待补桶

无。
