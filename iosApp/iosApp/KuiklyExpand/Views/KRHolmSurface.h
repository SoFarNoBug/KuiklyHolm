#import <UIKit/UIKit.h>
#import <OpenKuiklyIOSRender/KuiklyRenderViewExportProtocol.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * KRHolmSurface — 容器岛（ADR 0009）。
 * Runtime：子 View + CAMetalLayer → holm_runtime_session_*（主线程 CADisplayLink 外泵）。
 * Lite：本波诚实 onError（LITE_PENDING），不 GLES 出画。
 * 类名必须与 viewName=KRHolmSurface 一致。
 */
@interface KRHolmSurface : UIView <KuiklyRenderViewExportProtocol>

@end

NS_ASSUME_NONNULL_END
