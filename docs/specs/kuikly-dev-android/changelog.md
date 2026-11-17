# changelog · kuikly-dev-android

## 2026-11-17（ADR 0014 · 消灭 holm-runtime 命名）

- [MODIFY] native / C ABI / 产物 / Bridge·Demo 名对齐 `kmp-wgpu-native` / `kmp_wgpu_*` / `HolmWgpu*` / `holm_dualtrack`（详见 dualtrack changelog + ADR 0014）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（renderScale）

- [ADD] `PROP_RENDER_SCALE`：`setFixedSize` / `setSizeFromLayout`（Lite GLSurfaceView + Runtime SurfaceView）
- 默认 `1`=layout 物理基线；本单无真机验证
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（onShaderCompiled）

- [ADD] Lite `rebuildProgram` / Runtime `setCustomFs` 上报 `onShaderCompiled`（ADR 0012）
- 本单无真机验证
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-12（runbook 沉淀）

- [ADD] `runbook.md`：JDK17 / installDebug / 拉起 Activity / 拒权限与 jar 锁排障 / holm_spike 与 maxFps·gles2 语义
- [MODIFY] `README.md` 链到 runbook

## 2026-11-12

- 工作流迁入 kuikly-holm；`project_id` = `kuikly-holm`；L3 = `holm_api`。
