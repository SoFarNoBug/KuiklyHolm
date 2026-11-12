# stage0-lite-spike · changelog

## 2026-11-12（Lite 官方五席 + Attr 改名）

- [ADD] scene=`gradient_bg` / `fullscreen_warp`：同源 ShaderSource + `shaders/*.frag.glsl`；`HolmSceneShaders` 路由
- [MODIFY] 废弃公开 Attr `ior`/`warp` → `liquidBoost`/`liquidBend`；新增 `warpAmount`/`scrollSpeed`（互不共用）
- [MODIFY] Android/H5 桥与 `HolmGlPumpSource` uniform 对齐 `u_liquidBoost`/`u_liquidBend`/`u_warpAmount`/`u_scrollSpeed`
- [MODIFY] Demo `holm_spike`：五席芯片 + 按 scene 条件旋钮
- [ADD] ADR 0003；回写 Stage0 / glossary / CONTEXT
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审讯锁定五席与解耦 Attr｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（注释：Android maxFps / onPerf 语义）

- [MODIFY] 注释说明：Android `setFrameRate` 仅为提示；`onPerf`=真实 GL 提交率；CONTINUOUSLY 跳画会闪故不做；真限帧需 WHEN_DIRTY 时钟（本单不做）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：仅注释留痕｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（液态 fresnel 折射 + 封顶 dt）

- [MODIFY] `liquid_approx`：折射/色散改 fresnel 主导（中心不拧）；frag 优先 `highp`
- [MODIFY] Android/H5：`u_time` 改为封顶 dt 累加（≤50ms），减轻掉帧拖影（非热降频）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：中心花斑+拖影最小修｜待批 0（已达标 0）
- 本单无真机验证表（装机目视）

## 2026-11-12（Android 跳画闪烁 + 液态中心缩小花）

- [MODIFY] Android：去掉 `onDrawFrame` 累加器跳画（CONTINUOUSLY 跳画会闪）；限帧只靠 `setFrameRate`；同 fps 不重复申请
- [MODIFY] `liquid_approx`：中心 `n=0` + `centerFade` 压 warp/色散，消除「整 view 缩小版」花斑
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：真机回归最小修｜待批 0（已达标 0）
- 本单无真机验证表（装机目视）

## 2026-11-12（Android 高刷 + 液态中心 NaN）

- [MODIFY] Android `KRHolmSurfaceView`：API 31+ `Surface.setFrameRate` 随 `maxFps`（`0`→120）申请显示时钟
- [MODIFY] `liquid_approx`：中心安全 normalize（防 mediump NaN）；`pow` 高光 22→10
- [MODIFY] Demo：`maxFps` 步进 `0/30/60/120`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：真机三问题最小修｜待批 0（已达标 0）
- 本单无真机验证表（装机目视验收）

## 2026-11-12（HolmSurface maxFps 限帧）

- [ADD] Attr/`HolmSurface` Compose：`maxFps`（默认 60；`0`=不限；`<0` 按 0）
- [MODIFY] H5 `HolmGlPumpSource`：vsync rAF + `nextDue` 累加器跳过 GL 提交；`onPerf` 只计真实 draw
- [MODIFY] H5/Android `KRHolmSurfaceView`：`PROP_MAX_FPS` 脏更新；Android `onDrawFrame` 同语义早退
- [MODIFY] Demo：`maxFps` 步进 `0/60/120`；状态行 `maxFps=`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：限帧 API 对齐尖刺｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（H5 Worker P2：remount + init 回退）

- [MODIFY] H5 `KRHolmSurfaceView`：transfer 失败则换新 canvas 并回退主线程泵；destroy/remount 换新 canvas 可再次 Worker
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查 P2 最小修｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（H5 OffscreenCanvas Worker 泵帧）

- [ADD] `HolmGlPumpSource`：纯 JS WebGL 泵（antialias/depth 关闭；ior/warp 脏上传；RAF 不经 Kotlin）
- [MODIFY] H5 `KRHolmSurfaceView`：主路径 `transferControlToOffscreen` + Blob Worker；回退主线程同泵；ResizeObserver + visibility 停泵
- [MODIFY] 泵消息桥：`self.__holmPumpDispatch` 用 IIFE 赋值（避免 `js("self").asDynamic()` 编译成运行时炸）；postMessage 用 JS number；Map 键 toInt
- Android 执行器未改
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：H5 主线程降载、不改公开 Attr｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（liquid_approx 宽画布观感）

- [MODIFY] `liquid_approx`：近满幅玻璃板（去掉浮岛+暗框）；各向异性模糊/折射；色散改为轻 mix；压低 sheen
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：同 scene 观感微调｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（liquid_approx 观感+FPS）

- [MODIFY] `liquid_approx`：圆角玻璃 SDF + 5-tap 模糊 + 边缘折射/色散/高光（仍策略 A；约 7 sample/px）
- [MODIFY] 默认 `ior=1.35` / `warp=0.05`；Demo 步进加宽
- [ADD] Attr Event `onPerf`：Android/H5 GL 约 1Hz 上报 fps/frameMs；Demo 状态行显示
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：观感与性能目视最小修｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（第三特效 liquid_approx）

- [ADD] `LiquidApproxShaderSource` + `shaders/liquid_approx.frag.glsl`；`HolmSceneShaders` 路由 liquid_approx
- [ADD] Attr/`HolmSurface` Compose：`ior` / `warp`（默认 1.2 / 0.03；frost/glow 忽略）
- [MODIFY] Android/H5 `KRHolmSurfaceView`：uniform 热更新，不重建 program
- [MODIFY] `HolmSpikePager`：液态芯片 + ior/warp Lo/默认/Hi 步进；状态行显示两值
- [MODIFY] ADR 0002 / Stage0 / glossary / CONTEXT：第 3 scene Lite 扩容
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：特效扩容沿用 glow 形状｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（P2 收口 · glow review）

- [MODIFY] Demo：状态行常显 `scene=`（切换即刷新，不依赖二次 onReady）
- [MODIFY] `ReadyParams`/`ErrorParams`：decode 兼容 Map/JSONObject/JSON 字符串；H5 `fireReady` 改 `mapOf`
- [MODIFY] Android `onDetachedFromWindow`：`queueEvent { release() }` 再 `onPause`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查 P2 最小修｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（第二特效 glow）

- [ADD] `GlowShaderSource` + `shaders/glow.frag.glsl`；`HolmSceneShaders` 路由 frost/glow
- [MODIFY] Android/H5 `KRHolmSurfaceView`：`scene` 热换 program
- [MODIFY] `HolmSpikePager`：霜化 / 柔光切换
- [MODIFY] ADR 0002 / Stage0：验证第 2 scene 热换仍 Lite(A)
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：特效扩容无新规范冲突｜待批 0（已达标 0）
- 本单无真机验证

## 2026-11-12（毛玻璃观感）

- [MODIFY] `shaders/frost.frag.glsl` + `FrostShaderSource`：岛内模拟底图 + 13 点邻域模糊 + 白雾（策略 A）
- [MODIFY] `HolmSpikePager`：毛玻璃文案 + 页内高对比对照色条
- [MODIFY] ADR 0002 Consequences：观感升级仍 H1=A
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：纯观感迭代｜待批 0（已达标 0）

## 2026-11-12

- [ADD] `HolmSurface` / `KRHolmSurface` 薄壳（最小 Attr/Event）于 `:KuiklyHolmCore`
- [ADD] `shaders/frost` + `FrostShaderSource`；Android GLES2 / H5 WebGL1 霜化执行器（策略 A）
- [ADD] Demo `holm_spike` + Home 入口；androidApp / h5App 注册
- [ADD] Stage0 结论 Lite(A)；`00` O0 关闭；ADR 0002
- 编译：`:androidApp:compileDebugKotlin`、`:KuiklyHolmCore`/`:holmDemo`/`:h5App` `compileKotlinJs` 通过
- 本单无真机验证（编译门禁）；真机目视霜化 / onReady 可后续补表
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：纯业务尖刺，无新规范/反例/冲突写法｜待批 0（已达标 0）
