# changelog · kuikly-dev-ohos

## 2026-11-16（renderScale）

- [ADD] Attr `renderScale` → NAPI `setRenderScale`；`GetSurfaceSize`×scale + `SET_BUFFER_GEOMETRY`；Lite/Runtime resize
- 默认 `1`=物理基线；几何失败仍用逻辑尺寸
- 本单无真机验证
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（onShaderCompiled）

- [ADD] `FireShaderCompiled` + ETS 回调；Lite/Runtime 双耗时（ADR 0012）
- 本单无真机验证
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-15（审查整改 ohos-lite-adr0011）

- [MODIFY] `holm-runtime/README`：去掉鸿蒙 Lite=`LITE_PENDING`；对齐 ADR 0011 / `gles2-ohos`
- [MODIFY] Lite destroy：不 `eglTerminate(DEFAULT_DISPLAY)`（防误伤其它 EGL）
- [MODIFY] 切轨途中 WGSL 暂存不报 `SHADER_DIALECT`；显式 `setFragmentShader` 仍报
- 已知遗留：`P1-device-verify` 真机表待勾｜不修理由：须真机
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关审查可改项｜待批 0
- 自测：README 口径；切 Runtime→Lite 不应先闪 SHADER_DIALECT（待真机勾）

## 2026-11-15（ADR 0011 鸿蒙 Lite EGL）

- [ADD] ADR 0011：同 XComponent + EGL/GLES Shadertoy；`onReady.backend=gles2-ohos`
- [ADD] `holm_xc_lite_gl.*` + `holm_xc_shadertoy.h`（wrap/vert 对齐 Kotlin SoT）；CMake 链 `libEGL`/`libGLESv2`
- [MODIFY] `holm_xc_plugin`：去掉 `LITE_PENDING`；Lite/Runtime 互斥切轨；共用 OnFrame
- [MODIFY] `KRHolmSurface.ets` 注释；runbook 验收改双轨通面
- **同步义务**：改 `HolmShadertoyShell` / `HolmBuiltinShaders` 须同步 `holm_xc_shadertoy.h`
- 真机验证表：`docs/reviews/device-check-list/ohos-lite-adr0011.md`（交审核员勾选；码农只登记）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan ADR 0011｜待批 0
- 自测：Hvigor 编 `libholm_xc`；`holm_runtime_m0` Lite 隧道 + 互切（真机待勾）

## 2026-11-14

- ADR 0010 落地：`holm_runtime_session_create_ohos` + `build-ohos.sh` → `libholm_runtime.so`；`libholm_xc.so`（XComponent OnFrame 外泵）+ `KRHolmSurface.ets` 注册；Lite=`LITE_PENDING`。
- runbook 补齐解锁 / 验收步骤。
- **坑沉淀**：`AppScope/app.json5` UTF-8 BOM 导致 DevEco 自动签名假报 bundleName 非法；已改为无 BOM；runbook 增排查/修复。此前误判「三段包名」作废。
- 规范：按计划行事｜命中 platform_ohos / lang_arkts / lang_cpp / lang_c / new_code_file / holm_api｜门禁跳过｜真机验证待勾｜搬运无

## 2026-11-12

- 工作流迁入 kuikly-holm；`project_id` = `kuikly-holm`；L3 = `holm_api`。
