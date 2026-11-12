# Review · second-effect-glow · r2

- 结论：有条件通过
- 依据：基线 `review-second-effect-glow.md`；本轮核对 P2 收口 diff
- 日期：2026-11-12

## 对照上轮

| 桶 / 项 | 状态 | 说明 |
|---|---|---|
| P0 | 已关（无） | 仍无 P0 |
| P1-device-verify | 未关 | Android GLES 热换仍须真机；可执行档沿用基线 |
| P2-onready-stale-scene | 已关 | `HolmSpikePager` 状态行 `scene=$scene \| …` 随 Compose 状态刷新 |
| P2-h5-ready-payload | 已关 | H5 `mapOf` + `toHolmJson()` 兼容 Map/JSONObject/String |
| P2-android-release-thread | 已关 | `queueEvent { release() }` 后 `onPause` |
| 新开 | 无 | — |

## P0

无。

## P1

### P1-device-verify（未关）Android GLES scene 热换须真机确认
- 证据：热换逻辑仍在 `queueEvent`→`rebuildProgram`；代码审无法证真机观感
- 影响：放行条件不变
- 处置：必修；可执行档 → [device-check-list/second-effect-glow.md](./device-check-list/second-effect-glow.md)

## P2

无。（上轮三项均已关）

## 规范候选观察

无。

## 真机

同基线；可执行档未改。

## changelog

`docs/specs/stage0-lite-spike/changelog.md`「P2 收口 · glow review」已 append（审核员不代写）。

## 待补桶

无。
