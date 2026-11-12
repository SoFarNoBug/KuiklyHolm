# Cursor Agent 协作指南（kuikly-holm / Holm）

配置与流程在 **`.cursor/`**。产品 SoT 在 **`docs/product/`**。

## 目录

| 路径 | 用途 |
|---|---|
| `.cursor/rules/*.mdc` | 角色 / 闸门；`alwaysApply` 或 description 触发 |
| `.cursor/skills/*/SKILL.md` | 技能流程包 |
| `.cursor/hooks.json` + `.cursor/hooks/` | 编码前确认闸 + 编码 Skill 硬闸 |
| `docs/skill-references/` | 规范全文 / 自检清单 |
| `docs/product/` | 产品决策、架构、阶段（SoT） |

## 角色路由

| 意图 | Rule | Skill |
|---|---|---|
| Android / 通用 KMP 编码 / 修 Bug / 按计划 / 速速改 | `kuikly-dev-android.mdc` | `kuikly-android-coding` |
| 鸿蒙（ohosApp / ets / NAPI / ohosArm64）编码 | `kuikly-dev-ohos.mdc` | `kuikly-ohos-coding` |
| iOS（iosApp / Swift / ObjC / iosMain）编码 | `kuikly-dev-ios.mdc` | `kuikly-ios-coding` |
| 审查 / review | `kuikly-dev-review.mdc` | `kuikly-code-review` |
| 烤一下 / grill一下 / 审讯一下 | `grill-with-docs.mdc` | `grill-with-docs` → `grilling` + `domain-modeling` |
| Kuikly Compose DSL | `kuiklyComposeDSL.mdc` | — |
| 任何落盘改码前 | `coding-confirm-gate.mdc`（always） | + coding-gate hook |
| 确认 Plan 后执行 | `plan-exec-coding-skill.mdc`（always） | **必须先 Read** 对应码农 Skill；+ skill-gate hook |

## 双闸（硬拦）

| 闸 | Hook | 拦什么 | 如何放行 |
|---|---|---|---|
| 编码前确认闸 | `coding-gate-ask.mjs` | 未选 1/2 写非免闸文件 | 对话选 1/2，或 `gate-unlock.mjs on` |
| 编码 Skill 硬闸 | `coding-skill-gate.mjs` | 未 Read 码农 SKILL 写 `.kt`/`.ets`/`.swift`/`.m`/`.mm`/`.cpp` 等 | **Read** `kuikly-android-coding` / `kuikly-ohos-coding` / `kuikly-ios-coding` 的 `SKILL.md`；或说「速速改」；或 `skill-gate-unlock.mjs on` |

说明：确认闸放行 **≠** Skill 硬闸武装。Implement Plan 时两者都要过。  
Skill 硬闸文档：`docs/context/coding-skill-gate.md`；确认闸：`docs/context/coding-gate.md`。

## 阅读顺序（新人 / Agent）

1. `AGENTS.md` → `CONTEXT.md`
2. `docs/product/README.md` → `docs/product/00-context-and-decisions.md`
3. `docs/product/07-p0-hard-problems.md`
4. `docs/product/phases/Stage0.md`
5. 本文件 → 按意图 Read 对应 Skill

## Plan 与 grill-with-docs

- 设计空间大且用户说「烤一下 / grill一下 / 审讯一下」→ grill，再写 CONTEXT/ADR / 计划。
- 用户已确认计划并说执行 → **先 Read** 对应码农 Skill（Android / 鸿蒙 / iOS），再改业务源码（hook 会拦）。

## 里程碑

业务阶段以 `docs/product/phases/` 为准（Stage0 → Lite 特效 或 Standard M0–M3）。  
决策 SoT：`docs/product/00-context-and-decisions.md`。
