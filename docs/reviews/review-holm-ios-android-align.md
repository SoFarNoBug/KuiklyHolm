# Review · holm-ios-android-align

- 结论：有条件通过
- 依据：Plan「iOS 架构对齐 Android（三包 + kmp-wgpu Present）」；ADR 0008/0009；dualtrack / kuikly-dev-ios changelog
- 日期：2026-11-16
- 范围：iOS kmp-wgpu cinterop Present、holm-renderer-* ios、Provider、`HolmRuntimeIosBridge`、`KRHolmSurface` 停直调 C；不含 Ohos 拆包、不含把泵迁进 KN、不含 iOS headless Compute

## 对照上轮

无。（新 task-id；承接 Android+H5 三包已通过之架构，本波仅 iOS）

## P0

无。

## P1

### P1-device-verify
- 证据：Plan §6 DoD 要求 Sim DualTrack（Runtime `wgpu-m0*` ↔ Lite `gles2-ios`、Provider 负向、缺库 BLOCKED）；本轮仅 Gradle `linkPodDebugFrameworkIosSimulatorArm64` / `IosArm64` 编译门禁，未跑 Xcode Sim/真机 UI
- 影响：Present/切轨/Provider 闸是否诚实出画，代码审不闭环
- 处置：必修（放行条件）
- 可执行档 → [`device-check-list/holm-ios-android-align.md`](device-check-list/holm-ios-android-align.md)

### P1-unlisted（ohos-signing-in-tree）
- 证据：工作区 `ohosApp/build-profile.json5` 新增本地 `signingConfigs`（含 cert/store 路径与 password 字段）；**非本 Plan 产物**
- 影响：误提交会把本机签名材料带进仓；与 iOS 对齐无关但挡干净提交
- 处置：必修 — 提交前 **还原 / 勿 add** 该文件；勿写入本波 commit
- 待补桶：建议 `P1-security`（敏感材料入仓）

## P2

### P2-kmp-wgpu-holm-setters
- 证据：`IosWgpuPlatform.setCustomFs/setScene/…`（`WgpuIos.kt`）承载 Holm Session 语义；Android 对标在 `HolmAndroidJni`（renderer 模块）
- 影响：原语层产品面略渗 Holm；有 `HolmIosNative` 转发，行为正确
- 处置：建议 — 另波可把 set* 迁回 renderer 自有 cinterop，或接受注释边界

### P2-worktree-noise
- 证据：`iosApp.xcodeproj/project.pbxproj` 多为条目重排；`static_server/serve/**` 未跟踪；与本波无关
- 处置：可选 — 提交只收本波路径

### P2-provider-negative-hard
- 证据：Demo `HolmIosBridge.configure` + umbrella `ensure` 使「未 register」负向在默认宿主难触发；代码路径 `requireProvider` → `BLOCKED` 已接线（`KRHolmSurface.m` startLite/Runtime）
- 处置：可选 — 真机表以默认路径为主；负向标「代码路径 / 可选特制宿主」

## 规范候选观察

无。

## 真机

挂 `P1-device-verify`。代码审不了：Sim/真机隧道出画、Lite↔Runtime 互切、`maxFps`/`renderScale`、缺 xcframework 链接期失败观感。可执行档：[`device-check-list/holm-ios-android-align.md`](device-check-list/holm-ios-android-align.md)。

## Plan / 整改对齐

| Plan 项 | 状态 |
|---|---|
| kmp-wgpu / surface ios + cinterop | 已 |
| renderer ios + FullscreenPass | 已 |
| Core umbrella api + Bootstrap + Demo register | 已 |
| ObjC 停直调 `holm_runtime_session_*` | 已（ripreg 无匹配） |
| 泵仍 ObjC | 已 |
| ADR/docs/changelog | 已 |
| Sim/真机 DualTrack DoD | **未跑** → P1-device-verify |

卡 F：对称 teardown（destroyHandle + 停 DisplayLink）保留；方言热切仍拒非本轨；未改 Shadertoy 双源。

## changelog

请码农确认已 append（审核员不代写）：
- `docs/specs/dualtrack-lite-vs-standard/changelog.md`（iOS 架构对齐）
- `docs/specs/kuikly-dev-ios/changelog.md`

## 待补桶

- 建议 `P1-security`：敏感签名/密钥材料入工作区或提交的定级
