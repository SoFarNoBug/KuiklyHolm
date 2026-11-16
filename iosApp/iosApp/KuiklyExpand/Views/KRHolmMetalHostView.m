#import "KRHolmMetalHostView.h"
#import <Metal/Metal.h>

/**
 * @CreateDate: 2026/11/14
 * @Author: jlj
 * @Description: CAMetalLayer 宿主子 View（ADR 0009 Runtime 呈现面）。
 */

@interface KRHolmMetalHostView ()
@property (nonatomic, assign) CGFloat renderScale;
@end

@implementation KRHolmMetalHostView

+ (Class)layerClass {
    return [CAMetalLayer class];
}

- (CAMetalLayer *)metalLayer {
    return (CAMetalLayer *)self.layer;
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _renderScale = 1.0;
        self.opaque = YES;
        self.contentScaleFactor = UIScreen.mainScreen.nativeScale;
        CAMetalLayer *layer = self.metalLayer;
        // wgpu CoreAnimationLayer 要求 layer.device 非空；缺省 device 会导致 create_metal 失败。
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();
        if (device) {
            layer.device = device;
        }
        layer.pixelFormat = MTLPixelFormatBGRA8Unorm;
        layer.framebufferOnly = YES;
        layer.contentsScale = self.contentScaleFactor;
        [self holm_updateDrawableSize];
    }
    return self;
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [self holm_updateDrawableSize];
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        self.contentScaleFactor = self.window.screen.nativeScale;
        self.metalLayer.contentsScale = self.contentScaleFactor;
        [self holm_updateDrawableSize];
    }
}

- (void)holm_setRenderScale:(CGFloat)scale {
    CGFloat next = scale;
    if (!(next > 0.0) || !isfinite(next)) {
        next = 1.0;
    } else if (next > 1.0) {
        next = 1.0;
    }
    if (fabs(next - _renderScale) < 1e-6) {
        return;
    }
    _renderScale = next;
    [self holm_updateDrawableSize];
}

- (void)holm_updateDrawableSize {
    CGFloat native = self.contentScaleFactor > 0 ? self.contentScaleFactor : UIScreen.mainScreen.nativeScale;
    CGFloat scale = native * (_renderScale > 0 ? _renderScale : 1.0);
    CGSize points = self.bounds.size;
    CGSize pixels = CGSizeMake(MAX(1.0, floor(points.width * scale)),
                               MAX(1.0, floor(points.height * scale)));
    CAMetalLayer *layer = self.metalLayer;
    if (!CGSizeEqualToSize(layer.drawableSize, pixels)) {
        layer.drawableSize = pixels;
    }
}

@end
