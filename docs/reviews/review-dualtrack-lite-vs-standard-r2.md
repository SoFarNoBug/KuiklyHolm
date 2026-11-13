# Review · dualtrack-lite-vs-standard · r2

- 结论：有条件通过
- 依据：基线 `review-dualtrack-lite-vs-standard.md`；整改 changelog「审查整改：onError 重放 + Runtime 假通面」
- 日期：2026-11-12

## 对照上轮

| 桶 / 项 | 状态 | 说明 |
|---|---|---|
| P0 | 已关（无） | 仍无 P0 |
| P1-concurrency | 已关 | Android/H5 `fireError` 写 pending + `ON_ERROR`→`replayPendingError`；iOS `pendingError*` + `setCss_onError` 重放 |
| P1-doc-drift | 已关 | DualTrack / runbook / `.h`/`.m` 统一「frost 逐字 / liquid 骨架同源」 |
| P1-device-verify | 未关 | 代码仍审不了 iOS 目视与 Runtime 旁路 UX；可执行档沿用基线 |
| P2-runtime-ready-without-draw | 已关 | `isAvailable` = `isLibraryLoaded` ∧ `isM0Presentable`（本波 presentable=false） |
| P2-playing-ignores-executor | 已关 | `applyPlayingRenderMode()`：runtime 强制 WHEN_DIRTY |
| 新开 | 无 | — |

## P0

无。

## P1

### P1-device-verify（未关）iOS Lite 目视 + Android/H5 Runtime 旁路态
- 证据：整改后仍无 Mac/装机闭环；表步骤 #5 现应对得上 onError 重放，但仍须设备勾
- 影响：放行条件不变
- 处置：必修；可执行档 → [`device-check-list/dualtrack-lite-vs-standard.md`](device-check-list/dualtrack-lite-vs-standard.md)
- 代码审不了：frost/liquid 观感；Runtime 页 `BLOCKED` 文案是否出现；Lite 默认入口

## P2

无。（上轮两项均已关）

## 规范候选观察

无。（上轮候选仍在本 skill `_backlog`，本轮无新增）

## 真机

同基线；可执行档未改。建议优先跑表步骤 #3–#5 验证 onError 重放。

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md`「审查整改」条目仍在（审核员不代写）。

## 待补桶

无。
