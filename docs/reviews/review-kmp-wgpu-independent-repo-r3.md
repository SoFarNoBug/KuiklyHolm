# Review · kmp-wgpu-independent-repo · r3

- 结论：有条件通过
- 依据：上轮 [`review-kmp-wgpu-independent-repo-r2.md`](review-kmp-wgpu-independent-repo-r2.md)；Plan「kmp-wgpu 独立仓与独立发布」；ADR 0015；真机表 [`device-check-list/kmp-wgpu-independent-repo.md`](device-check-list/kmp-wgpu-independent-repo.md)
- 日期：2026-11-17
- 范围：复审 + **关 `kmp.wgpu.local` 纯 Maven**（mavenLocal `0.1.0`）Android 装包通面；对照 r2 开放桶。

## 对照上轮

| 桶 ID | 状态 | 说明 |
|---|---|---|
| P1-device-verify | 已关 | 四端通面（用户 r2 前已确认）+ 本轮 #6：关 local → `io.github.sofarnobug:kmp-wgpu*:0.1.0` → `installDebug` → `holm_dualtrack` 隧道出画 `backend=wgpu-m0` |
| P2-orphan-holm-tree | 未关 | Holm 仍留 `kmp-wgpu*` + `MOVED.md` |
| P2-ohos-dts-liquid | 未关 | `libholm_xc/index.d.ts` 仍导出 `setLiquid` |
| P2-doc-cleartri | 未关 | `HolmSurface` fragmentShader KDoc 仍写 ClearTri |

## P0

无。

## P1

无。

## P2

### P2-orphan-holm-tree（未关）
- 证据：仓内 `kmp-wgpu/` 等仍在；settings 默认不 include（ADR 0015 Maven）
- 处置：可选 — 删旧树或缩成指针

### P2-ohos-dts-liquid（未关）
- 证据：[`ohosApp/.../libholm_xc/index.d.ts`](../../ohosApp/entry/src/main/cpp/types/libholm_xc/index.d.ts) 仍有 `setLiquid`
- 处置：可选 — 删 dts 行或注明废止

### P2-doc-cleartri（未关）
- 证据：[`HolmSurface.kt`](../../holm-core/src/commonMain/kotlin/com/jlj/kuiklybase/holm/HolmSurface.kt) KDoc「→ ClearTri」
- 处置：可选 — 改「只清屏」

## 规范候选观察

无。

## 真机

`P1-device-verify` 已关。可执行档：[`device-check-list/kmp-wgpu-independent-repo.md`](device-check-list/kmp-wgpu-independent-repo.md)（本轮勾选 #6）。

本轮 Maven 证据摘要：
- `local.properties`：`kmp.wgpu.local=true` 已注释
- `dependencyInsight`：`io.github.sofarnobug:kmp-wgpu-android:0.1.0` / `kmp-wgpu-surface-android:0.1.0`（非 includeBuild substitute）
- SM-A505F：`installDebug` → `holm_dualtrack`；`executor=renderer-wgpu` · `sample=tunnel` · `onReady backend=wgpu-m0` · compile ok · 隧道动效

## Plan 对齐

- 独立仓 + 默认 Maven / local 旁路 — 本轮纯 Maven 路径已验收
- H5 wasm ABI / liquid / DeviceSession — 上轮已关，本轮无回归开卡

## changelog

已确认上轮「Review 打回修复」条目仍在；本轮审核员未改 changelog。

## 待补桶

无。

## 后续另开（本 Phase 不闭环）

| 项 | 状态 | 说明 |
|---|---|---|
| Ohos `includeBuild` 缺 `ohosArm64` | **已修（码农）** | 独立仓 `ohosInclude/` + 模块 `build.ohos`；`settings.ohos` local → 该入口；`./ohos_build.sh` BUILD SUCCESSFUL |
| Central 正式发版 | **已修（码农）** | `publish-maven.sh` BUILD SUCCESSFUL；`io.github.sofarnobug:kmp-wgpu*:0.1.0`（automaticRelease） |
| 删 Holm 旧树 `kmp-wgpu*` | **已修（码农）** | 已删 `kmp-wgpu` / `kmp-wgpu-surface` / `kmp-wgpu-native`；`P2-orphan-holm-tree` 可关 |
