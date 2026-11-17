# Review · kmp-wgpu-extract-v1 · r2

- 结论：有条件通过
- 依据：上轮 [`review-kmp-wgpu-extract-v1.md`](review-kmp-wgpu-extract-v1.md)；Plan「kmp-wgpu 抽芯 v1（方案 B）」；ADR 0013；changelog「修审查 P0/P1」
- 日期：2026-11-16
- 范围：同基线；本轮含 H5 浏览器冒烟（`localhost:8080/?page_name=holm_runtime_m0`）+ 对照整改 diff。不含 Android 装包。

## 对照上轮

| 桶 ID | 状态 | 说明 |
|---|---|---|
| P0-silent-fail | 已关 | H5 `Device.sessionTick` 成败只信 throw/`Result`；`HolmRuntimeNative.sessionSetCustomFs`/`sessionTick` 注释与实现同口径；冷启 Runtime 隧道 `compile ok` + `fps≈60` |
| P1-plan-soft-drift | 已关 | `kmp-wgpu` `DeviceSession` 已无 `setCustomFs`/`setScene`/`liquid`；Holm 语义在 `HolmFullscreenPass` / `HolmRuntimeNative` |
| P1-device-verify | 未关 | H5 #6/#8 过；#7 出画恢复但红字/泵需切轨清（见下）；Android #1–#5/#9 未跑 |
| P2-abiVersion-hardcode | 已关 | Android `HolmRuntimeNative.abiVersion()` → `androidPlatform().abiVersion()` |
| P2-worktree-noise | 未关 | 工作树仍可能含无关 diff；提交前自行拆 |

## P0

无。

## P1

### P1-device-verify（未关）Android 未验；H5 #7 残余体验
- 证据：
  - H5 冷启 Runtime+隧道：`backend=wgpu-m0`、`compile ok`、`fps≈60`（截图 `_holm-h5-runtime-tunnel-ok.png`）
  - H5 故意失败：`SHADER_CUSTOM` / `compile fail` 诚实红字
  - H5 失败→隧道：`compile ok` 且隧道出画，但同屏仍叠旧 `SHADER_CUSTOM` 红字、`fps` 一度≈1；切 Lite（`webgl-worker`）再回 Runtime 后红字清、`fps≈59`
  - Android 装包/缺 so/热换/resize：本轮未执行
- 影响：H5 主路径可用；DoD「热换后再成功」体验未完全闭环；Android 放行条件未闭环
- 处置：必修（放行条件）→ 可执行档仍 [`device-check-list/kmp-wgpu-extract-v1.md`](device-check-list/kmp-wgpu-extract-v1.md)；建议码农另查成功装载后 `flushRuntimeSessionErrors` 是否误投旧 pending、以及 Demo `onError` 停泵后成功样本是否应强制 `playing=true`（本复审不改码）

## P2

### P2-worktree-noise（未关）
- 证据：同基线
- 处置：可选

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/kmp-wgpu-extract-v1.md`](device-check-list/kmp-wgpu-extract-v1.md)。  
H5 本轮已勾部分步骤（见该表「本轮勾选」）；Android 仍待装包。

## Plan 对齐

- 方案 B / 双模块 / Android+H5 门面 / 不迁 iOS·Ohos — 符合
- P0 sticky 否决成功帧 — 已关
- Holm 语义迁出 kmp-wgpu — 已关
- changelog「修审查 P0/P1」已 append

## changelog

已确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 含「kmp-wgpu v1 · 修审查 P0/P1」。

## 待补桶

无。
