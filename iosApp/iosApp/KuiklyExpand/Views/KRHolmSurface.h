#import <UIKit/UIKit.h>
#import <OpenKuiklyIOSRender/KuiklyRenderViewExportProtocol.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Lite iOS KRHolmSurface — OpenGLES2。
 * 官方五席已移除；本波未接宿主 fragmentShader Attr，无片元时清屏 + onError。
 * 类名必须与 viewName=KRHolmSurface 一致。
 */
@interface KRHolmSurface : UIView <KuiklyRenderViewExportProtocol>

@end

NS_ASSUME_NONNULL_END
