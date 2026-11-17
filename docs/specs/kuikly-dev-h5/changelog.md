# changelog · kuikly-dev-h5

## 2026-11-17（ADR 0014 · 消灭 holm-runtime 命名）

- [MODIFY] native / C ABI / 产物 / Bridge·Demo 名对齐 `kmp-wgpu-native` / `kmp_wgpu_*` / `HolmWgpu*` / `holm_dualtrack`（详见 dualtrack changelog + ADR 0014）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（renderScale）

- [ADD] `PROP_RENDER_SCALE`：canvas backing = CSS×scale（Lite+Runtime）；`createSession(width,height)`
- 默认 `1`=CSS 基线（仍不乘 DPR）；本单无真机验证
- 轻量治理：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（onShaderCompiled）

- [ADD] Lite pump / Runtime `setCustomFs` 上报 `onShaderCompiled`（ADR 0012）
- 本单无真机验证
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-12（H5 双端 + runbook）

- 双端开发口：`8083`（`nativevue2.js`）+ `8080`（`jsBrowserDevelopmentRun`）；样页 `home` / `holm_spike`
- 新增 [runbook.md](./runbook.md)
- 工作流迁入 kuikly-holm；`project_id` = `kuikly-holm`；L3 = `holm_api`
