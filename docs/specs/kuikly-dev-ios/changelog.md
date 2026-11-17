# changelog · kuikly-dev-ios

## 2026-11-17（ADD · iOS headless Compute 回读）

- [ADD] C ABI：`ios_c_api` 导出 `holm_runtime_headless_*`；`HeadlessCompute` 仅 Metal
- [ADD] `kmp-wgpu` / Demo：`ComputeReadback.ios`（串行队列）+ `ComputeReadbackBridge.ios`
- 自测：`./build-ios.sh`；`:holmDemo:linkPodDebugFrameworkIosSimulatorArm64`；**iPhone Duo** `kmp_wgpu_compute` → 用户验过 OK
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改 iOS Compute｜待批 0

## 2026-11-16（Lite 冷启 defer FRAGMENT_REQUIRED + Duo 出画）

- [FIX] `KRHolmSurface` Lite：`didMoveToWindow` 早于 Compose attr 时不抢报 `FRAGMENT_REQUIRED`；晚到 `setCss_fragmentShader` 成功后补 `onReady`
- [MODIFY] Demo 默认 `executor=RUNTIME`（验收临时 Lite 已还原）
- 自测：iPhone Duo Sim 冷启 Lite=`gles2` 隧道、Runtime=`wgpu-m0` fps≈58；截图进 device-check-list
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：速速改 Duo Lite 冷启｜待批 0

## 2026-11-16（审查整改 · Holm set* 迁出 kmp-wgpu）

- [FIX] P2-kmp-wgpu-holm-setters：`IosWgpuPlatform` 去掉 set*；`HolmSessionBindings` + `HolmIosNative` 转发（renderer 侧自有 cinterop 未进 classpath，改同模块独立 object）
- [FIX] holmDemo cocoapods `export(:holm-renderer-gl)`：修 ObjC 不见 `HolmShadertoyShell` / `HolmBuiltinShaders`
- [NOTE] 还原工作区 `ohosApp/build-profile.json5` 签名噪声（勿提交）
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：速速改审查项｜待批 0

## 2026-11-16（iOS 架构对齐 Android · 三包 + kmp-wgpu Present）

- [ADD] kmp-wgpu / surface / renderer ios；`HolmRuntimeIosBridge`；Core umbrella ios `api` 两 renderer
- [MODIFY] `KRHolmSurface.m`：Present 经 `HolmDemoHolmRuntimeIosBridge`；停直调 C Session；Provider 闸
- [MODIFY] ADR 0008/0009 + runbook；feature changelog 见 dualtrack
- 自测：sim+arm64 `linkPodDebugFramework*` 通过；无 `holm_runtime_session_*` 于 ObjC
- 真机验证表：待审核员 `docs/reviews/device-check-list/holm-ios-android-align.md`
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（Runtime create_metal device + 错误透传）

- [MODIFY] `KRHolmMetalHostView`：`CAMetalLayer.device = MTLCreateSystemDefaultDevice()`（wgpu CoreAnimationLayer 硬依赖）
- [MODIFY] `ios_c_api`：`create_metal` 失败写入 `holm_runtime_last_create_error`；宿主 `onError` 带真实原因
- [MODIFY] `KRHolmSurface`：device nil / create 失败均透出细节，仍 `BLOCKED` 不冒充 GLES
- [MODIFY] `session_core`：`required_limits.or_worse_values_from(adapter)`（Duo sim inter_stage 15&lt;16）
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：速速改 Duo Runtime BLOCKED｜待批 0

## 2026-11-16（renderScale）

- [ADD] `setCss_renderScale:`；Lite `contentScaleFactor=renderScale`（基线仍 1× 点）；Runtime `drawableSize=points×nativeScale×renderScale`
- 默认 `1` 保持改前像素；本单无真机验证
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（onShaderCompiled）

- [ADD] Lite/Runtime 装载结束 `onShaderCompiled`（wrapMs/compileMs；ADR 0012）
- 本单无真机验证
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-14（关 P2-objc-shadertoy-drift）

- [MODIFY] `KRHolmGlesHostView`：wrap/vert 改调 `HolmShadertoyShell` / `HolmBuiltinShaders`（holmDemo）；删 ObjC 内嵌信封
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关审查 P2｜待批 0
- 自测：Lite 隧道仍出画

## 2026-11-14（修 Lite ~1fps / fps=0）

- [MODIFY] `KRHolmGlesHostView`：缓冲改 1x（不乘 nativeScale）；尺寸未变跳过 `renderbufferStorage`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改 Lite 主线程堵帧｜待批 0
- 自测：Lite 隧道流畅；状态行 fps>0

## 2026-11-14（iOS Lite GLES）

- [ADD] `KRHolmGlesHostView`：CAEAGLLayer + Shadertoy wrap（对齐 HolmShadertoyShell）
- [MODIFY] `KRHolmSurface`：Lite 出画 / `gles2-ios`；与 Runtime 互斥 teardown；去 `LITE_PENDING`
- [MODIFY] `pbxproj`：链 OpenGLES；`runbook` / ADR 0009 Consequences / CONTEXT
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 落地 iOS Lite｜待批 0
- 自测：Simulator Runtime↔Lite 隧道；空片元 `FRAGMENT_REQUIRED`

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
