# 待沉淀池（审查 skill 自有）

> 候选只进**本文件**。不得自动写入码农的 `_backlog`（ADR 0036）。
> `project.yaml` 的 `peer_pools` 仅提示可人工抄录的路径。
> 候选只保留与本仓无关业务细节的通用判据；晋升或作废走人工评审。

**条目格式（候选）**：`- [ ] {规则一句话} ｜{日期} ｜自评：{已达标 / 未达标（缺什么）}`

## 候选

- [ ] Content Registry 过 NVI：View 销毁/卸岛必须 unregister 当前 contentId，避免 map 泄漏｜2026-11-18｜自评：未达标（holm-content-gltf P2）
- [ ] Gradle 物理拆后若 umbrella 自动 `register`+`setDefault`，ADR/产品文须写明「兼容例外」，否则与「禁止仅靠 classpath 静默默选」冲突｜2026-11-16｜自评：未达标（holm-three-package-split P1-doc-drift）
- [ ] 跨 FFI 包装 `Result`/throw 时禁止再用 sticky `lastError*` 二次否决成功路径；`drain` 若不清 last，宿主不得把 last 当帧成败｜2026-11-16｜自评：未达标（kmp-wgpu-extract-v1 P0）
- [ ] DualTrack 性能对比须标注 `renderScale` 与端基线（物理/点/CSS），避免跨端数字误读｜2026-11-16｜自评：未达标（renderscale device-verify）
- [ ] 原生桥 `onError`/`onReady`：若 Attr 侧可能在回调注册前 fire，须像 onReady 一样支持「已发生则重放」，避免 Compose Attr→Event 顺序丢错 ｜2026-11-12｜自评：未达标（dualtrack P1-concurrency）
- [ ] DualTrack/expand View：`executor` 热切后须按当前轨重发 `onReady`（含 backend），不得依赖「Surface 已创建则不再 onSurfaceCreated」｜2026-11-14｜自评：未达标（android-runtime-dualtrack P1-doc-drift）
- [ ] 持 native Surface 的急拆：await 超时也必须停泵并丢弃句柄，禁止超时后继续 tick｜2026-11-14｜自评：未达标（android-runtime-dualtrack P1-concurrency）
- [ ] 删除/改写 kmp-wgpu-native 着色器源后，须同单或明确门禁更新入仓 `.so`/wasm，并对照产物字符串或行为验收；禁止只改 Rust 源却宣称仓库无官方席｜2026-11-14｜自评：未达标（delete-five-scenes P1-plan-soft-drift）
- [ ] 同窗 DualTrack（wgpu↔EGL）落地后，README/runbook 不得残留 `LITE_PENDING` 当现行；切轨占窗释放须真机证｜2026-11-15｜自评：未达标（ohos-lite-adr0011 P1-doc-drift + device-verify）
- [ ] Demo 仅换 fragmentShader 时不应清空 onReady/backend 展示；切 executor 与切片元语义不同｜2026-11-15｜自评：未达标（demo-acd-samples P2）
- [ ] Runtime 错误桥：`emit`+`Result`/`throw` 与宿主 `drain` 只能选一侧 fire，避免同一次失败双发 onError｜2026-11-15｜自评：未达标（wgpu-error-to-business P2-duplicate-onerror）
- [ ] H5 Runtime 主线程回退须与 Worker 同契约透传 tick/lastError/drain，禁止只接 Worker 路径｜2026-11-15｜自评：未达标（wgpu-error-to-business P1-plan-soft-drift）

## 已晋升

- [x] 鸿蒙 NAPI 新导出必须同步 `types/lib*/index.d.ts`｜2026-11-16｜→ `review_heuristics` 卡 F §6
- [x] Kotlin/JS 禁止 `js("ident").asDynamic()` 赋浏览器全局｜2026-11-12｜→ 卡 F §7
- [x] expand View `executor` 切换须对称 teardown（含 Worker/Offscreen）｜2026-11-13｜→ 卡 F §1
- [x] Offscreen Worker 异步 init 失败须与主线程同契约｜2026-11-13｜→ 卡 F §2
- [x] 先 fragment 再 executor：拒非本轨方言；FFI `catch_unwind`｜2026-11-14｜→ 卡 F §3
- [x] DualTrack 性能对比须标注端与分辨率策略；模拟器 fps 不作真机结论｜2026-11-14｜→ 卡 F §4（`renderScale` 细项仍见候选未达标）
- [x] C++ Shadertoy wrap 与 Kotlin SoT 双源须 changelog 同步义务｜2026-11-15｜→ 卡 F §5
