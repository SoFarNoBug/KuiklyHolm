# Review · kmp-wgpu-independent-repo · r2

- 结论：有条件通过
- 依据：上轮 [`review-kmp-wgpu-independent-repo.md`](review-kmp-wgpu-independent-repo.md)；Plan「kmp-wgpu 独立仓与独立发布」；ADR 0015；changelog「Review 打回修复 · wasm setFragmentShader + 删 liquid」
- 日期：2026-11-17
- 范围：同基线；对照整改：H5/wasm 重编、liquid 全删、`DeviceSession` 删除。不含真机勾选执行。

## 对照上轮

| 桶 ID | 状态 | 说明 |
|---|---|---|
| P0-silent-fail | 已关 | `h5App`/`npm` glue 已暴露 `setFragmentShader`；无 `setCustomFs`/`setLiquid`；`HolmWgpuNative.sessionSetCustomFs` 硬依赖新 ABI |
| P1-plan-soft-drift | 已关 | 独立仓无 `DeviceSession*`；`HolmSurface`/四端壳/GLES 无 liquid 泵；Present 走 `FullscreenPass` |
| P1-device-verify | 未关 | 四端通面 / Maven·local 切换仍须真机表；本轮未勾选 |
| P2-orphan-holm-tree | 未关 | Holm 仍留 `kmp-wgpu*` + `MOVED.md` |
| P2-comment-noise-ios | 已关 | `KRHolmSurface.m` 叠注释已收 |
| P2-ohos-dts-liquid | 新开 | `libholm_xc/index.d.ts` 仍导出已删 NAPI `setLiquid` |
| P2-doc-cleartri | 新开 | `HolmSurface` KDoc 仍写「renderer-wgpu → ClearTri」（已废） |

## P0

无。

## P1

### P1-device-verify（未关）独立仓 + Holm 四端通面 / Maven·local 切换
- 证据：代码审确认 glue ABI 与 liquid 删除；未执行装包/浏览器/iOS/Ohos 出画
- 影响：放行条件未闭环
- 处置：必修 → 可执行档 [`device-check-list/kmp-wgpu-independent-repo.md`](device-check-list/kmp-wgpu-independent-repo.md)
- 代码审不了：H5 #7–#8 隧道；Android #1–#6；iOS #9–#10；Ohos #11–#12

## P2

### P2-orphan-holm-tree（未关）
- 证据：`kmp-wgpu*/MOVED.md` 仍在；settings 不 include
- 处置：可选 — 删旧树或缩成指针

### P2-ohos-dts-liquid（新开）
- 证据：[`ohosApp/.../libholm_xc/index.d.ts`](../../ohosApp/entry/src/main/cpp/types/libholm_xc/index.d.ts) 仍有 `setLiquid`；`holm_xc_plugin.cpp` 已无 `ExportFn(..., "setLiquid", …)`
- 影响：ArkTS 类型与 NAPI 漂移；ets 已不调，非主路径崩溃
- 处置：可选 — 删 dts 行或注明废止

### P2-doc-cleartri（新开）
- 证据：[`HolmSurface.kt`](../../holm-core/src/commonMain/kotlin/com/jlj/kuiklybase/holm/HolmSurface.kt) fragmentShader KDoc「→ ClearTri」
- 影响：口径旧；行为已是无片元只清屏
- 处置：可选 — 改「只清屏」

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/kmp-wgpu-independent-repo.md`](device-check-list/kmp-wgpu-independent-repo.md)。

## Plan 对齐

- H5 wasm ABI / liquid 删除 / DeviceSession 迁出 — 已关上轮打回点
- 独立仓 + Maven/includeBuild — 代码侧仍覆盖；真机未勾
- changelog「Review 打回修复」已确认存在

## changelog

已确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 含「Review 打回修复 · wasm setFragmentShader + 删 liquid」。

## 待补桶

无。
