# 待沉淀池（审查 skill 自有）

> 候选只进**本文件**。不得自动写入码农的 `_backlog`（ADR 0036）。
> `project.yaml` 的 `peer_pools` 仅提示可人工抄录的路径。
> 候选只保留与本仓无关业务细节的通用判据；晋升或作废走人工评审。

**条目格式（候选）**：`- [ ] {规则一句话} ｜{日期} ｜自评：{已达标 / 未达标（缺什么）}`

## 候选

- [ ] Kotlin/JS 禁止 `js("ident").asDynamic()` 赋浏览器全局（会编成 `ident.asDynamic()` 运行时炸）；改用 `js("(function(fn){ self.x = fn; })")(fn)` 或等价 IIFE ｜2026-11-12｜自评：已达标（h5-worker-gl-pump 已规避）
- [ ] 原生桥 `onError`/`onReady`：若 Attr 侧可能在回调注册前 fire，须像 onReady 一样支持「已发生则重放」，避免 Compose Attr→Event 顺序丢错 ｜2026-11-12｜自评：未达标（dualtrack P1-concurrency）
- [ ] Kuikly expand View 模式/`executor` 切换须对称 teardown（含 Worker/Offscreen），禁止只清半套 Session 标志 ｜2026-11-13｜自评：已达标（dualtrack-r4 已关对应 P0；待批晋升）
- [ ] Offscreen Worker 异步 init 失败须与主线程失败同契约（BLOCKED + ready + 可回退/看门狗）｜2026-11-13｜自评：已达标（dualtrack-r4 已关对应 P0；待批晋升）
- [ ] DualTrack/expand View：`executor` 热切后须按当前轨重发 `onReady`（含 backend），不得依赖「Surface 已创建则不再 onSurfaceCreated」｜2026-11-14｜自评：未达标（android-runtime-dualtrack P1-doc-drift）
- [ ] 持 native Surface 的急拆：await 超时也必须停泵并丢弃句柄，禁止超时后继续 tick｜2026-11-14｜自评：未达标（android-runtime-dualtrack P1-concurrency）
- [ ] 删除/改写 holm-runtime 着色器源后，须同单或明确门禁更新入仓 `.so`/wasm，并对照产物字符串或行为验收；禁止只改 Rust 源却宣称仓库无官方席｜2026-11-14｜自评：未达标（delete-five-scenes P1-plan-soft-drift）
- [ ] DualTrack/expand View：Compose 先写 fragmentShader 再写 executor 时，仍持 Runtime Session 必须拒绝非本轨方言；跨 FFI native 入口须 catch_unwind（或等价）防 panic abort｜2026-11-14｜自评：已达标（ios-runtime-adr0009 热修；待批晋升）
- [ ] DualTrack 性能对比须标注端与分辨率策略；iOS Lite 1x 点像素；模拟器 GLES raymarch fps 不作真机结论｜2026-11-14｜自评：已达标（ios-lite-gles 审查观察；待批晋升）

## 已晋升


（暂无）
