# changelog · kuikly-dev-ios

## 2026-11-14（关审查 P2 DisplayLink/resize）

- [MODIFY] `KRHolmSurface`：`CADisplayLink` 经弱持有 proxy；`layout` 像素尺寸未变跳过 `session_resize`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关 review P2｜待批 0
- 自测：Runtime 通面；旋转/进出页不崩

## 2026-11-14（修切 Lite 崩溃）

- [MODIFY] `KRHolmSurface`：`fragmentShader` 热切时非 WGSL 跳过（对齐 H5）；勿把 GLSL 喂 `set_custom_fs`
- [MODIFY] `ios_c_api`：C 入口 `catch_unwind`，panic 不穿 FFI abort
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：双轨切轨崩溃热修｜待批 0
- 自测：Runtime→Lite 不崩；Lite=`LITE_PENDING`

## 2026-11-14（ADR 0009 iOS Runtime）

- [MODIFY] `KRHolmSurface`：容器 + `KRHolmMetalHostView`；Runtime C Session 外泵；Lite=`LITE_PENDING`
- [ADD] `KRHolmMetalHostView`；`pbxproj` 链 `HolmRuntime.xcframework` + Metal/QuartzCore
- [MODIFY] `runbook.md`：先 `build-ios.sh` 再 Run；验收改隧道 Runtime
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 落地｜待批 0
- 本单无真机验证表文件（交审核员）；自测见 dualtrack changelog

## 2026-11-12（错误语义）

- [MODIFY] `onError` pending 回放；场景改为 frost 骨架 / liquid 非强制同源
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：dualtrack 错误语义｜待批 0
- 本机验证：待 Mac

## 2026-11-12（DualTrack A）

- [ADD] `iosApp/.../KRHolmSurface.{h,m}` OpenGLES2（scene=`frost`/`liquid_approx`）Attr + onReady/onError
- [MODIFY] `iosApp.xcodeproj`：Placeholder → KRHolmSurface
- [ADD] `docs/specs/kuikly-dev-ios/runbook.md` 起服与验收勾选
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：双轨 A 落壳｜待批 0
- 本机验证：待 Mac

## 2026-11-12

- 技能迁移 kuikly-holm：`project_id` = `kuikly-holm`；L3 = `holm_api`
