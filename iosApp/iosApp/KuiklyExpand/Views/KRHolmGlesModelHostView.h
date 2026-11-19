#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class HolmDemoHolmIosModelSnapshot;

/**
 * GLES2 网格 Model 宿主（CAEAGLLayer + depth24）；对齐 Android HolmGlesModelRenderer。
 */
@interface KRHolmGlesModelHostView : UIView

@property (nonatomic, readonly) BOOL hasMesh;
@property (nonatomic, readonly) CGSize drawablePixelSize;

- (BOOL)holm_setupGL;
- (void)holm_teardownGL;
- (void)holm_resizeBuffersIfNeeded;
- (void)holm_setRenderScale:(CGFloat)scale;
- (void)holm_setOrbitYaw:(float)yaw pitch:(float)pitch enabled:(BOOL)enabled;
- (void)holm_nudgeOrbitDx:(float)dx dy:(float)dy;
- (void)holm_setColorR:(float)r g:(float)g b:(float)b a:(float)a;
/** lit=0 unlit；intensity 仅 lit 时有效。 */
- (void)holm_setLit:(BOOL)lit
          lightDirX:(float)x
                  y:(float)y
                  z:(float)z
          intensity:(float)intensity;
- (void)holm_uploadSnapshot:(HolmDemoHolmIosModelSnapshot *)snapshot;
- (void)holm_clearMesh;
- (void)holm_drawFrame;

@end

NS_ASSUME_NONNULL_END
