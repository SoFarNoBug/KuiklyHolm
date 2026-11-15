#import <UIKit/UIKit.h>
#import <QuartzCore/CAMetalLayer.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Runtime 子 View：layerClass = CAMetalLayer；维护 drawableSize / nativeScale。
 */
@interface KRHolmMetalHostView : UIView

@property (nonatomic, readonly) CAMetalLayer *metalLayer;

- (void)holm_updateDrawableSize;

@end

NS_ASSUME_NONNULL_END
