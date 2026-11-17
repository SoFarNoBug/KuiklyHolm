# DualTrack · Lite vs Standard（限时尖刺）

> **Attr 命名（2026-11-17）**：对外执行器已改为 `executor=renderer-gl` / `renderer-wgpu`（废 `lite`/`runtime`）。下文历史「Lite/Standard」仅保留尖刺史实，勿再当 UI/Attr 用词。

> **状态**：已收口（2026-11-12）→ ADR 0004 **Keep Lite**；**O0 仍为 Lite(A)**（未改选）。  
> **动机**：多端对齐要证据；真模型英雄区只能走 Standard，先用最小通面估账单。

## 1. 目标

用可量化证据决定：

1. **Keep Lite** — 继续 Lite 五席 + 补齐端缺口  
2. **Switch Standard** — 改选 O0=Standard，开 M0→M2 真模型  
3. **Lite 业务 + Standard 仅 3D** — 长期双栈（仅当表证明必要）

## 2. 范围

| 轨 | 内容 | 端 | 非目标 |
| --- | --- | --- | --- |
| **A · Lite** | `KRHolmSurface` iOS；`frost` 逐字同源 + `liquid_approx` 骨架同源 | iOS（Android/H5 已有） | 本波不强求五席全上 iOS；liquid 不强制逐字同步 |
| **B · Standard** | `:kmp-wgpu-native` wgpu 清屏/三角 + 旁路桥；Demo 可切换，**默认 Lite** | Android + H5 | glTF / M1 / M2 / 鸿蒙 |

## 3. 非目标（硬）

- 本波不上真模型 `.glb`
- 不删除 Lite 官方五席（ADR 0003）
- 对比完成前不把 O0 改成 Standard
- 不静默把 Runtime 换成非 wgpu 栈（阻塞则表内记 **BLOCKED**）

## 4. 验收

### Track A

- [x] 代码：`KRHolmSurface` iOS GLES2 + `frost`/`liquid_approx`（工程已挂 pbxproj）  
- [x] `onReady.backend` 约定 `gles2-ios`  
- [x] [`docs/specs/kuikly-dev-ios/runbook.md`](../../specs/kuikly-dev-ios/runbook.md) 起服命令可跟  
- [ ] Mac 模拟器目视（本机 Windows → PARTIAL）  

### Track B

- [x] `kmp-wgpu-native` Rust+wgpu 骨架 + `executor=renderer-wgpu` 旁路  
- [x] 缺 native 时 `onError(BLOCKED)` + `backend=wgpu-blocked`（不静默 GLES）  
- [x] Demo `holm_dualtrack`；默认仍 Lite 五席  
- [ ] Android/H5 可见清屏/三角（**BLOCKED**：无 rustc）  
- [x] 包体增量粗估行（见下表：未链 native ≈0；链上后另测）  

## 5. 对比表（收口必填）

| 项 | Track A · Lite | Track B · Standard M0 |
| --- | --- | --- |
| 人日（粗估） | ~1～1.5（ObjC GLES 薄壳 + 两 scene + runbook） | ~0.5 骨架 + **≥3～5** 待 rustc/NDK/wasm 通面（未发生） |
| 包体增量 | 小（两份 GLSL 串 + View；无 Runtime） | 本波未链 .so/wasm ≈ **+0**；通面后粗估 **数 MB 级**（wgpu+依赖，待测） |
| 多端对齐难度 | 中：同源 GLSL 可吃；iOS 须 Mac 验；五席全上仍要补 | 高：Rust 工具链 + 双端 Surface/wasm；本波未通 |
| 加下一能力边际成本 | 换 scene/shader | Pass/Runtime |
| 真模型可行性 | 否（假 3D 可硬拧） | 通面后可达 M2（本波未通面） |
| 风险 / BLOCKED | Mac 未验；shader 串双份同步 | **BLOCKED：本机无 rustc/cargo**；禁止静默换栈 |
| 本机验证 | PARTIAL（代码+工程；无 Mac 目视） | BLOCKED（旁路可演示错误态；无清屏/三角） |

## 6. 收口

1. [x] 填满上表  
2. [x] 证据收口 → **1 · Keep Lite**（B BLOCKED，不足以改选 Standard / 双栈）  
3. [x] [`docs/adr/0004-lite-vs-standard-dualtrack.md`](../../adr/0004-lite-vs-standard-dualtrack.md)  
4. [x] **未**改 O0（仍 Lite(A)）；O1 尖刺默认记 wgpu  

## 7. 关联

- Stage0 / ADR 0002 / ADR 0003  
- M0.md（B 轨最小子集）  
- CONTEXT「当前状态」
