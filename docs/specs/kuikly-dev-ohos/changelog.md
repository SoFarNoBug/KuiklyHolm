# changelog · kuikly-dev-ohos

## 2026-11-14

- ADR 0010 落地：`holm_runtime_session_create_ohos` + `build-ohos.sh` → `libholm_runtime.so`；`libholm_xc.so`（XComponent OnFrame 外泵）+ `KRHolmSurface.ets` 注册；Lite=`LITE_PENDING`。
- runbook 补齐解锁 / 验收步骤。
- **坑沉淀**：`AppScope/app.json5` UTF-8 BOM 导致 DevEco 自动签名假报 bundleName 非法；已改为无 BOM；runbook 增排查/修复。此前误判「三段包名」作废。
- 规范：按计划行事｜命中 platform_ohos / lang_arkts / lang_cpp / lang_c / new_code_file / holm_api｜门禁跳过｜真机验证待勾｜搬运无

## 2026-11-12

- 工作流迁入 kuikly-holm；`project_id` = `kuikly-holm`；L3 = `holm_api`。
