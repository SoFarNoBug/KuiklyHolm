#import <UIKit/UIKit.h>
#import <OpenKuiklyIOSRender/KuiklyRenderViewExportProtocol.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * KRHolmSurface — 容器岛（ADR 0009 + Lite GLES）。
 * Runtime：子 View + CAMetalLayer → HolmWgpuIosBridge（kmp-wgpu Present）；主线程 CADisplayLink 外泵。
 * Lite：子 View + CAEAGLLayer GLES2 Shadertoy；onReady.backend=gles2-ios。
 * 类名必须与 viewName=KRHolmSurface 一致。
 */
@interface KRHolmSurface : UIView <KuiklyRenderViewExportProtocol>

@end

NS_ASSUME_NONNULL_END
