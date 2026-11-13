#import <UIKit/UIKit.h>
#import <OpenKuiklyIOSRender/KuiklyRenderViewExportProtocol.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Lite iOS KRHolmSurface — OpenGLES2；frost 逐字同源，liquid_approx 骨架同源（允许端侧精简）。
 * 类名必须与 viewName=KRHolmSurface 一致。
 */
@interface KRHolmSurface : UIView <KuiklyRenderViewExportProtocol>

@end

NS_ASSUME_NONNULL_END
