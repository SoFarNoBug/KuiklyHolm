#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

typedef void (^KRHolmGlesErrorBlock)(NSString *code, NSString *message);
typedef void (^KRHolmGlesCompiledBlock)(BOOL ok, float wrapMs, float compileMs);

/**
 * Lite 子 View：layerClass = CAEAGLLayer；GLES2 片元外泵。
 * Shadertoy wrap / 全屏 vert 调用 holmDemo（HolmShadertoyShell / HolmBuiltinShaders）。
 */
@interface KRHolmGlesHostView : UIView

@property (nonatomic, copy, nullable) KRHolmGlesErrorBlock onError;
@property (nonatomic, copy, nullable) KRHolmGlesCompiledBlock onShaderCompiled;
@property (nonatomic, readonly) BOOL hasProgram;
@property (nonatomic, readonly) CGSize drawablePixelSize;

- (BOOL)holm_setupGL;
- (void)holm_teardownGL;
- (void)holm_resizeBuffersIfNeeded;
/** 相对点像素基线；(0,1]，默认 1。 */
- (void)holm_setRenderScale:(CGFloat)scale;
/** 空正文返回 NO（调用方发 FRAGMENT_REQUIRED）；失败保留上一成功 program。 */
- (BOOL)holm_setFragmentBody:(NSString *)body;
- (void)holm_drawFrame:(float)dt;

@end

NS_ASSUME_NONNULL_END
