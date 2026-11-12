# 05 · 一致性、质量门禁与性能预算（Holm）

> **Standard/Full**：强制执行本节全家桶。  
> **Lite**：同源 shader 前提下可用简化对拍（同场景截图人工比），不强制 SSIM CI；仍禁止系统玻璃冒充。

## 1. 「一致」三级定义（防注水）

| 级别 | 定义 | 目标 |
| --- | --- | --- |
| L-逻辑 | 同 scene+props+seek → 同 Pass 集合（本端 Caps 允许范围内） | Standard 必须；Lite 尽量 |
| L-观感 | 黄金图达 **下列写死阈值** + 产品签字 | Standard 必须；Lite 抽检 |
| L-逐像素 | 哈希相同 | **否** |
| L-系统气质 | 每端都像系统原生控件语言 | **不承诺**（D4） |

跨端统一材质可能在某端显得「外来」——验收时产品须知情，不得事后用「不够原生」否定已达标黄金图，除非改 00 目标。

## 2. 黄金图（写死初值）

### Fixture

| ID | scene | seek | 用途 |
| --- | --- | --- | --- |
| F0 | `clear_red` | 0 | 通面 |
| F1 | `gradient_bg` | 0 | 色空间 |
| F2 | `frost_panel` | 0 | 模糊 |
| F3 | `liquid_approx_v1` | 0 与 1.0 | 可重复 |
| F4 | `model_hero` | 0 | M2 |

### 采集规格（不得口头改）

| 项 | 值 |
| --- | --- |
| 岛像素 | **512×512**（物理像素） |
| quality | `standard` |
| seed | `42` |
| 暖机帧 | 丢弃前 10 帧，取第 11 帧 |
| 格式 | PNG，sRGB |
| 对比区域 | **整岛 bitmap**（不含岛外 Kuikly 控件） |
| Reference 端 | **Android Vulkan**（无则 Metal）；其它端比 reference |

### 通过阈值（初值）

| Fixture | 度量 | 阈值 |
| --- | --- | --- |
| F0 / F1 | SSIM | **≥ 0.98** |
| F2 / F3 | SSIM | **≥ 0.96** |
| 备选 | mean ΔE (CIE76) | 平坦 **&lt; 3.0**；模糊 **&lt; 5.0** |

**改阈值**：必须改本文并发 PR/纪要说明原因与截图；禁止验收现场「调到过」。

### 争议升级

1. 先看是否达上表。  
2. 达线而产品仍不满意 → 改场景/材质或改 00 目标，**不是**悄悄放宽阈值。  
3. 未达线 → 缺陷，不发布。

## 3. 逻辑测试

- Reduced：refraction pass **不进入** encode 计数  
- seek 两次状态一致  
- 未知 scene / 非法 JSON → onError，不崩  

## 4. 性能预算

| 场景 | 设备 | 目标 |
| --- | --- | --- |
| 岛约 1080×600，Standard frost | 中端 Android | 平均 **≥ 50 FPS**，无持续 jank |
| + refraction High | 旗舰 | ≥ 50 FPS |
| Web Standard | 桌面 Chrome | ≥ 50 FPS |
| 不可见/后台 | 全端 | 暂停 tick |

D6（一页少岛、列表默认禁）是使用纪律，不是「已优于 Lite」的证明。Lite vs Standard 性能须同场景实测，不得空口。

## 5. 调试清单

| 症状 | 先查 |
| --- | --- |
| 偏暗 | sRGB |
| 颠倒 | Y 翻转 |
| 一端无折射 | Caps/Quality（鸿蒙子集见 D9） |
| 字不一致 | 岛外 Text——预期 |
| 滚动撕裂 | 07 H2；应用否违规进 List |
| 二次花屏 | destroy / H4 |

## 6. 可访问性

减弱动态效果 → Reduced / 暂停（M3）。

## 7. 回报表

```text
| 项 | Android | iOS | 鸿蒙 | Web | 数值 |
| F0 SSIM |  |  |  |  |  |
| F2 SSIM |  |  |  |  |  |
| 对比区域 | 整岛 512 |  |  |  |  |
| 阈值是否改过 | 否/是+PR |  |  |  |  |
```

---

**上一篇**：[04-scene-effects-3d.md](./04-scene-effects-3d.md)  
**下一篇**：[06-implementation-roadmap.md](./06-implementation-roadmap.md)
