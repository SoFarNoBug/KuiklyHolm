# Review · holm-ohos-android-align

- 结论：有条件通过（码农已关全部 P2 口径；仅余 `P1-device-verify`）
- 依据：Plan「Ohos 一步到位对齐 Android（三包 + kmp-wgpu Present）」；ADR 0008/0010/0011/0013；dualtrack / kuikly-dev-ohos changelog
- 日期：2026-11-16
- 范围：Ohos `kmp-wgpu*` / `holm-renderer-*` ohosArm64、Provider、`HolmRuntimeOhosBridge` / `holm_ohos_bridge_*`、`holm_xc` 停直调 C；不含 Maven/分仓、不含泵迁进 KN、不含 Ohos Compute 产品化
- 码农整改：2026-11-16 changelog/runbook/真机表口径；无行为回归改动；闭 P2 待 r2 勾表确认
- 码农整改（同日）：P2 四项已关口径（见下）；**仅余** `P1-device-verify`（DevEco 签名装机）

## 对照上轮

无。（新 task-id；承接 iOS `holm-ios-android-align` 同构，本波仅 Ohos）

## P0

无。

## P1

### P1-device-verify
- 证据：Plan DoD 要求真机 DualTrack（Runtime `wgpu-m0*` ↔ Lite `gles2-ohos`、Provider / 缺库负向）；本轮 `./ohos_build.sh` + `assembleHap` 编译门禁过；HAP 内 `libshared.so` 导出全部 `holm_ohos_bridge_*`，`NEEDED libholm_runtime.so` + `RUNPATH [$ORIGIN]`；`hdc list targets` 见 `FMR0223927006189`；`hdc install` unsigned → `code:9568320 no signature file`；`signingConfigs` 空、SignHap 跳过
- 影响：Present/切轨/Provider 闸是否诚实出画，代码审不闭环
- 处置：必修（放行条件）— DevEco 自动签名后 `./ohosApp/runOhosApp.sh` 勾表
- 可执行档 → [`device-check-list/holm-ohos-android-align.md`](device-check-list/holm-ohos-android-align.md)

## P2

### P2-provider-negative-hard
- 证据：Demo `HolmOhos.configure` / umbrella `ensure` 使「未 register」负向在默认宿主难触发；壳 `requireProvider` → `BLOCKED` 已接线（Lite/Runtime start）
- 处置：**码农已关口径** — 真机表 #8 / runbook #9 标可选特制宿主；默认 Demo 不挡放行

### P2-kmp-wgpu-holm-setters
- 证据：（基线笔误）实际 set* 已在 `HolmSessionBindings`，**不**挂 `OhosWgpuPlatform`；`HolmOhosNative` 转发 — 与 iOS r2「已关」同构
- 影响：原语层仍有 Holm 文件，产品面已隔离
- 处置：**码农已关口径**（changelog NOTE）；残余同模块边界可接受，不强制再迁 renderer cinterop

### P2-worktree-noise
- 证据：`static_server/serve/**`、审查截图 `_holm-*.png/jpeg` 等与本波无关未跟踪/噪声
- 处置：**码农已关口径** — 提交白名单本波路径；勿写回 `signingConfigs`

### P2-lite-needs-bridge
- 证据：`StartLiteIfNeeded` 亦 `EnsureBridgeResolved` + `requireProvider`（即使 Present 仍走本机 EGL）
- 影响：libshared 未加载时 Lite 亦 BLOCKED（不再 silent GLES）；与「无 Provider 不静默」一致，装载时序依赖 Demo 先起
- 处置：**码农已关口径**（故意行为；runbook §验收 #3 已写明）；真机冷启仍归 `P1-device-verify`

## 规范候选观察

无。

## 真机

挂 `P1-device-verify`。代码审不了：真机隧道出画、Lite↔Runtime 互切、`maxFps`/`renderScale`、缺 `libholm_runtime.so` 负向观感。可执行档：[`device-check-list/holm-ohos-android-align.md`](device-check-list/holm-ohos-android-align.md)。

本轮运行结论：**编译/HAP/符号门禁通过；装机被签名挡住**（设备在线，unsigned 拒装）。

## Plan / 整改对齐

| Plan 项 | 状态 |
|---|---|
| settings.ohos + kmp-wgpu/surface/renderer ohosArm64 | 已 |
| create_ohos + HolmSessionBindings + OhosSurfaces | 已 |
| FullscreenPass / HolmOhosNative / HolmRuntimeNative | 已 |
| Bridge @CName（Demo 导出）+ UmbrellaBootstrap + configure | 已 |
| holm_xc 停直调 `holm_runtime_session_*`；dlsym Present | 已（rg 仅注释） |
| 泵仍 libholm_xc；Lite 仍 EGL | 已 |
| ADR/00/runbook/changelog | 已 |
| 真机 DualTrack DoD | **未跑（无签名）** → P1-device-verify |

卡 F：对称 teardown（DestroyRuntimeSession + UnregisterFrame / DestroyLite）保留；方言热切仍拒非本轨；未改 Shadertoy 双源；本波无新增 ExportFn，`libholm_xc` dts 既有 `hasWgpu`/`setRenderScale`。

## changelog

请码农确认已 append（审核员不代写）：
- `docs/specs/dualtrack-lite-vs-standard/changelog.md`（Ohos 三包对齐）
- `docs/specs/kuikly-dev-ohos/changelog.md`

## 待补桶

无。
