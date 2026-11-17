# Review · holm-three-package-split-r2

- 结论：有条件通过
- 依据：上轮 [`review-holm-three-package-split.md`](review-holm-three-package-split.md)；changelog「三包拆 · 清审查 P1/P2 可修项」
- 日期：2026-11-16
- 范围：审查整改 diff（Demo/宿主显式 register、`_providerBlocked`、ADR/README 包名）；H5 DualTrack 本轮勾表。不含 Android 真机、不含 Provider View 分发重构。

## 对照上轮

| 桶 | 状态 | 说明 |
|---|---|---|
| P1-plan-soft-drift | 降级未关 | Demo/宿主显式 register **已关半**；Provider 仍标记式、View 双轨硬编码 **仍开** |
| P1-doc-drift | 降级未关 | 抬头/`holm-renderer-*`/umbrella 例外 **已关**；[`docs/product/README.md`](../product/README.md) 状态表仍写「尚未物理拆」**仍开** |
| P1-device-verify | 未关 | H5 #5–#6 本轮已过；Android #1–#4 未勾 |
| P2-provider-ready-backend | 已关 | `_providerBlocked` → `onReady.backend=wgpu-blocked` |
| P2-h5-worker-dual-path | 未关 | 仍 Worker session 路径 |
| P2-worktree-noise | 未关 | 提交时仍勿带 |

## P0

无。

## P1

### P1-plan-soft-drift（未关 · 降级）
- 证据：`HolmBackendProvider` 仍仅 id/executorKey；`KRHolmSurfaceView` 双轨仍合居；`HolmDemoBootstrap` / `KRApplication` / H5 `Main` 已显式 register
- 处置：建议 — View 半部分发另波；本半 Demo 注册可接受

### P1-doc-drift（未关 · 降级）
- 证据：`docs/product/README.md`「状态」表「目标拆包 | ADR 0008（尚未物理拆）」与同页抬头「Android+H5 已物理拆」矛盾
- 处置：必修 — 改状态表一行

### P1-device-verify（未关）
- 缺口：Android DualTrack Present（FullscreenPass + HolmAndroidJni）未真机勾
- 可执行档：[`device-check-list/holm-three-package-split.md`](device-check-list/holm-three-package-split.md)
- 处置：必修（放行条件）— 至少勾 Android #1–#4

## P2

### P2-h5-worker-dual-path（未关）
- 处置：可选 — 下波

### P2-worktree-noise（未关）
- 处置：可选 — 提交前拆开

### P2-compile-sticky-ui（新开 · 观察）
- 证据：H5 Lite↔Runtime 后状态行仍可能残留 `compile fail … (lite|runtime)`，同时 `fps=60` / `backend=wgpu-m0` 正常
- 处置：可选 — 与既有 sticky UI 债一并清；不挡本波 Present

## 规范候选观察

无。

## 真机

挂 `P1-device-verify`。可执行档：[`device-check-list/holm-three-package-split.md`](device-check-list/holm-three-package-split.md)。本轮 H5 #5–#6 已勾。

## Plan / 整改对齐

符合审查建议的文档半修、Demo 显式 register、Android ready backend；未宣称关掉 View 分发与 Android 真机。

## changelog

已见「三包拆 · 清审查 P1/P2 可修项」；请码农确认仍在。

## 待补桶

无。
