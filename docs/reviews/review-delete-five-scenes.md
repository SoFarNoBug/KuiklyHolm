# Review · delete-five-scenes

- 结论：有条件通过
- 依据：Plan [`.cursor/plans/2026-11-14-delete-five-scenes.md`](../../.cursor/plans/2026-11-14-delete-five-scenes.md)；changelog [`docs/specs/holm-render-substrate/changelog.md`](../specs/holm-render-substrate/changelog.md)（2026-11-14 删除官方五席）
- 日期：2026-11-14
- 范围：Core 五席源删除 + Resolve/View；holm-runtime SceneKind/WGSL；iOS 壳；Demo 示例1；根 shaders；轻文档。不含 Gradle 三包拆分 / `holm_api` 全文（计划非目标）

## 对照上轮

无。（新 task-id）

## P0

无。

## P1

### P1-plan-soft-drift（未关）Runtime 产物未重编，仓库内仍可经旧二进制出五席
- 证据：
  - 源码已删：`holm-runtime/src/shaders.rs` / `session_core.rs` 仅 ClearTri+Custom
  - 入仓产物仍含席位标签：`androidApp/.../libholm_runtime.so` 与 `h5App/.../holm_runtime_bg.wasm` 字符串检索 `holm-frost` / `frost_uv` / `holm-glow` / `holm-liquid` = **True**
  - changelog 已记「本单未强制重编 so」；Plan B「若本机可编」未做成硬门，但验收句「仓库内无五席…可被 scene= 选中出画」对 Runtime 二进制未兑现
- 影响：装旧 so/wasm 时，无 custom 且 `setScene(frost|glow|liquid)` 仍可能画出官方席；Demo 主路径（始终喂隧道 custom）不受影响
- 处置：必修 — `build-android.ps1` + wasm 构建后更新 jniLibs / `h5App/.../holm-runtime/`，再验无 `holm-frost` 字符串或 Runtime 无 custom 时不再出霜化

### P1-doc-drift（未关）runbook / CONTEXT 状态句未跟上删席
- 证据：
  - [`docs/specs/kuikly-dev-android/runbook.md`](../specs/kuikly-dev-android/runbook.md) 仍有「## Holm 尖刺目视（holm_spike）」与霜化/柔光/液态（约 L97–102）；同文件 DualTrack 表已改隧道
  - [`CONTEXT.md`](../../CONTEXT.md)「当前状态」仍写「五席入口标废弃」「后单：…删五席源码」（约 L64–65），与本单已删矛盾
- 影响：接手/真机指引可误开已删页或以为源码还在
- 处置：必修 — 删/改尖刺节；CONTEXT 状态改为「五席源码已删；Demo 仅示例1」

### P1-device-verify（未关）删席后隧道通面 / 空片元无霜化 / 重编产物
- 证据：Plan 验证 2–4；changelog「本单无真机验证」；二进制未重建
- 影响：放行条件
- 处置：必修 → 可执行档 [`device-check-list/delete-five-scenes.md`](device-check-list/delete-five-scenes.md)
- 代码审不了：Home 仅示例1入口；Lite/Runtime 隧道观感；空 `fragmentShader` 是否深蓝+error 且无霜化；重编 so/wasm 后无五席出画；iOS 清屏+`FRAGMENT_REQUIRED`

## P2

### P2-ios-dead-vert-constant
- 证据：`KRHolmSurface.m` 保留 `kHolmVert` 仅 `(void)kHolmVert`；`timeLoc`/`boostLoc`/`bendLoc`/`posLoc` 等未再使用
- 影响：无功能；噪音
- 处置：可选 — 下单接 `fragmentShader` Attr 时一并清或启用

### P2-product-adr-stale-delete-later
- 证据：`docs/product/00` / ADR 0008 仍写「五席源码可暂留 / 删除另单」（Plan 允许不大改历史 ADR 正文）
- 影响：产品 SoT 与仓库现状短暂双真源；ADR 0003 文首已注明已删
- 处置：建议 — 后单轻改 00/0008 一句「源码已删」

## 规范候选观察

1. 删除/改写 holm-runtime（或其它）着色器源后，须同单或明确门禁更新入仓 `.so` / wasm，并在验收对照产物字符串或行为；禁止只改 Rust/源而宣称「仓库无官方席」。

（已入本 skill `_backlog`；可人工抄 `peer_pools` → kuikly-android-coding）

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/delete-five-scenes.md`](device-check-list/delete-five-scenes.md)。

## changelog

请确认 [`docs/specs/holm-render-substrate/changelog.md`](../specs/holm-render-substrate/changelog.md) 已含「删除官方五席源码 · Demo 只留示例1」（审核员不代写）。已见 append。

## 待补桶

无。
