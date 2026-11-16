#import <UIKit/UIKit.h>
#import <QuartzCore/CAMetalLayer.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Runtime 子 View：layerClass = CAMetalLayer；维护 drawableSize / nativeScale。
 */
@interface KRHolmMetalHostView : UIView

@property (nonatomic, readonly) CAMetalLayer *metalLayer;

- (void)holm_updateDrawableSize;
/** 相对 nativeScale 基线；(0,1]，默认 1。 */
- (void)holm_setRenderScale:(CGFloat)scale;

@end

NS_ASSUME_NONNULL_END
