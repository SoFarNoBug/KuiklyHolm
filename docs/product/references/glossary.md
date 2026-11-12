# 术语表（Holm）

| 术语 | 含义 |
| --- | --- |
| **Holm** | 产品名；Kuikly 局部 GPU 特效增强（业务优先，非第一期通用开源引擎） |
| **Stage 0** | Lite(A) vs Standard(B) 选型门禁 |
| **Lite / 路径 A** | 同源 Shader + 轻量执行；可无大 Runtime；默认优先 |
| **Standard / 路径 B** | 可选统一 Runtime/RHI |
| **Full** | Standard + 简单 3D |
| **D12** | 身份锁定：Kuikly 业务优先 |
| **HolmSurface** | Kotlin / Compose API 组件名 |
| **KRHolmSurface** | 四端原生 viewName |
| **Holm Runtime / holm-runtime** | 共享 Scene/Pass/Shader 内核 |
| **holm_*** | C ABI 前缀 |
| **岛 / Island** | 架构比喻；即 HolmSurface 占据的区域 |
| **RHI** | Rendering Hardware Interface |
| **Bridge** | 各端原生壳 + FFI |
| **Scene** | 场景描述 |
| **Pass / PassGraph** | 有序渲染步骤 |
| **Caps** | 设备能力 |
| **Quality** | high / standard / reduced / auto |
| **Backdrop** | 折射/模糊采样的背景来源 |
| **黄金图** | 固定参数参考截图 |
| **SSoT** | 单一真相源 |
| **系统 Liquid Glass** | Apple iOS/macOS 26+ 系统材质（Holm 不依赖） |
| **Kuikly Blur** | 框架毛玻璃组件（非 Holm） |
| **wgpu** | WebGPU 语义跨平台图形库 |
| **XComponent** | 鸿蒙自定义绘制宿主 |
| **观感一致** | 容差内一致，非逐像素 |
| **GearUI** | Kuikly 上的 UI 组件库，非 GPU 层 |
| **liquid_approx** | Lite scene 键；对应产品 `liquid_approx_v1` 的单 Pass 近似（非多 Pass / 非真 Backdrop） |
| **ior / warp** | Lite Attr 强度旋钮（非光学物理 IOR；非 Scene `propsJson`）；仅 `liquid_approx` 使用 |

更多决策见 `../00-context-and-decisions.md`。
