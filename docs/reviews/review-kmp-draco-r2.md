# Review · kmp-draco · r2

- 结论：有条件通过
- 依据：对照 [`review-kmp-draco.md`](review-kmp-draco.md)；Plan `.cursor/plans/2026-11-18-kmp-draco.md`；ADR 0017；changelog 已含「审查 kmp-draco r1」
- 日期：2026-11-18
- diff（相对 r1）：`settings.gradle.kts` 旁路默认 includeBuild；`publish-maven.sh` + mavenLocal；`DracoDecodeInstrumentedTest`；`DecodedMesh`/`README`/ADR/changelog

## 对照上轮

| 桶 | 状态 | 说明 |
|---|---|---|
| P0-build-break | **已关** | 旁路仓存在则默认 includeBuild（[`settings.gradle.kts`](../../settings.gradle.kts) L55–78）；`publish-maven.sh local` 已产出 mavenLocal `0.1.0`；Central 仍 404，但 `kmp.draco.local=false` 可走 mavenLocal，不再因缺 gitignore 属性而默认断解析 |
| P1-device-verify | **未关** | 真机表未勾；Demo Tokyo 出画仍代码审不了 |
| P1-plan-soft-drift | **已关** | 已补 [`DracoDecodeInstrumentedTest`](../../../../kmp-draco/kmp-draco/src/androidInstrumentedTest/kotlin/com/jlj/kmp/draco/DracoDecodeInstrumentedTest.kt)（Tokyo 切片 + empty 抛错）；跑通属真机/联调，不挡关本桶 |
| P2-arm64-only-jni | **已关** | README 标明仅 arm64 |
| P2-skinning-attrs-ignored | **已关** | changelog 已知遗留登记；非本波 |
| P2-decodedmesh-texcoord-ambiguous | **已关** | `texCoords` KDoc 标明首个 + 推荐 uniqueId |
| （新开） | 无 | — |

## P0

无。

## P1

### P1-device-verify Tokyo / KHR_draco 真机出画
- 证据：可执行档仍在；无「步骤结果 OK」勾选；instrumented 未代替整包 Demo
- 影响：放行条件 — 接线已齐，运行时未闭环
- 处置：必修 → [`device-check-list/kmp-draco.md`](device-check-list/kmp-draco.md)
- 代码审不了：Tokyo 可见可拖；gl/wgpu；缺 so fail-loud；进出稳定

## P2

无。

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/kmp-draco.md`](device-check-list/kmp-draco.md)（r1 已产，本轮沿用）。

## changelog

已见 r1 回执行条目；真机勾表后请再 append。审核员不代写。

## 待补桶

无。
