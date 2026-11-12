# 02 · Kuikly 集成指南（HolmSurface）

> **D12**：第一期唯一宿主 = Kuikly；本文件是业务接入主路径。  
> Lite / Standard 共用同一套 `HolmSurface` / `KRHolmSurface` API；差别在原生壳背后是轻量执行器还是 Runtime。  
> 证据：`D:\code_space\Kuikly\docs\DevGuide\expand-native-ui.md`；Compose：`extend-kuikly-dsl-ui.md`。

## 1. 为何走自定义 View（证据）

Kuikly 官方模型：common `DeclarativeBaseView` + 四端 Export/注册。  
**结论**：Holm 做成 **原子自定义组件**，不改 Kuikly Core。  
系统 Liquid Glass / Blur 是平行能力；Holm **不替换**它们（D3）。

## 2. 组件形态

| 选项 | 选择 |
| --- | --- |
| 原子 `DeclarativeBaseView` | **采用** |
| `ViewContainer` 强行挂子节点进 GPU | 不采用；岛上控件用兄弟叠层 |
| `ComposeView` 子类 + `MakeKuiklyComposeNode` | **禁止**（文档：会不可见） |

## 3. API 草案

### viewName

```text
KRHolmSurface
```

### Attr

| Key | 类型 | 含义 | 默认 |
| --- | --- | --- | --- |
| `scene` | string | 场景 ID | 必填 |
| `playing` | bool | 连续渲染 | true |
| `quality` | string | `auto\|high\|standard\|reduced` | auto |
| `propsJson` | string | 场景参数 JSON | `{}` |
| `clearColor` | string | `#AARRGGBB` | M0 用 |
| `seed` | int | 噪声种子 | 0 |

### Event

| Event | 载荷 |
| --- | --- |
| `onReady` | `{ backend, quality }` |
| `onError` | `{ code, message }` |
| `onFrame` | `{ fps, frameIndex }`（可选 Debug） |

### Method

| Method | 参数 | 含义 |
| --- | --- | --- |
| `seek` | `{ t }` | 场景时间（秒） |
| `capture` | 可选 | M3 截图 |
| `pause` / `resume` | — | 可与 playing 合并 |

### Kotlin 草图

```kotlin
class HolmSurfaceView : DeclarativeBaseView<HolmSurfaceAttr, HolmSurfaceEvent>() {
    override fun createAttr() = HolmSurfaceAttr()
    override fun createEvent() = HolmSurfaceEvent()
    override fun viewName() = "KRHolmSurface"

    fun seek(t: Float) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("seek", """{"t":$t}""")
        }
    }
}

fun ViewContainer<*, *>.HolmSurface(init: HolmSurfaceView.() -> Unit) {
    addChild(HolmSurfaceView(), init)
}
```

Compose：

```kotlin
@Composable
fun HolmSurface(
    scene: String,
    modifier: Modifier = Modifier,
    quality: String = "auto",
    propsJson: String = "{}",
    playing: Boolean = true,
) {
    MakeKuiklyComposeNode<HolmSurfaceView>(
        factory = { HolmSurfaceView() },
        modifier = modifier,
        viewUpdate = { /* 同步 attr */ }
    )
}
```

## 4. 四端 Bridge

### 共同职责

1. 创建原生 Surface / Layer / Canvas  
2. resize  
3. 同步 attr（scene / quality / props / playing）  
4. detach 释放 GPU  
5. 触摸可选转发（特效进阶）  

**Lite**：壳内直接跑预编译 shader / 轻量后端。  
**Standard**：壳内 `holm_create` / `holm_resize` / `holm_destroy` → Runtime。

### Android

- 宿主：TextureView 或 SurfaceView  
- `IKuiklyRenderViewExport` + 注册  
- Lite：GLES 执行器即可；Standard：Vulkan 优先 / GLES 回退  

### iOS

- `CAMetalLayer` / MTKView  
- `KuiklyRenderViewExportProtocol`  
- **禁止**用 `UIGlassEffect` 冒充 Holm 输出  

### 鸿蒙

- `XComponent`（SURFACE/TEXTURE）  
- registerMap / V2  
- Lite：Native Drawing / GLES 尖刺；Standard：NAPI → Runtime（wgpu OH 弱 → D9 子集）  

### Web

- `<canvas>`；Lite 可用 WebGL2；Standard 可 wasm + WebGPU→WebGL2  
- 禁止用 WebView+Three 充验收  

## 5. Demo 页

```text
HolmDemoPage
  - 展示 backend / quality（onReady）
  - 中部 HolmSurface
  - playing / seek / quality 控件
```

## 6. 与 Blur / 系统玻璃共存

| API | 态度 |
| --- | --- |
| `Blur` | 非 Holm 页可用；Holm 失败时业务降级 |
| `glassEffectIOS` / `LiquidGlass` | 四端一致需求页 **不要用** |
| `HolmSurface` | 四端一致特效/3D 的推荐入口 |

## 7. 集成验收

- [ ] 至少 Android+iOS：`HolmSurface` 上屏 + onReady  
- [ ] resize / 二次进入不崩  
- [ ] Compose + DSL 各一页  
- [ ] viewName / prop 与文档一致  

## 8. 必读 Kuikly 路径

```text
docs/DevGuide/expand-native-ui.md
docs/Compose/extend-kuikly-dsl-ui.md
demo/.../LiquidGlassDemoPage.kt   # 仅 Demo 结构参考
core-render-android/.../blur/     # 组件形态参考
```

---

**上一篇**：[01-architecture.md](./01-architecture.md)  
**下一篇**：[03-rhi-and-runtime.md](./03-rhi-and-runtime.md)
