#import "KRHolmMetalHostView.h"
#import <Metal/Metal.h>
#import <Metal/Metal.h>

/**
 * @CreateDate: 2026/11/14
 * @Author: jlj
 * @Description: CAMetalLayer 宿主子 View（ADR 0009 Runtime 呈现面）。
 */

@implementation KRHolmMetalHostView

+ (Class)layerClass {
    return [CAMetalLayer class];
}

- (CAMetalLayer *)metalLayer {
    return (CAMetalLayer *)self.layer;
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.opaque = YES;
        self.contentScaleFactor = UIScreen.mainScreen.nativeScale;
        CAMetalLayer *layer = self.metalLayer;
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

- (void)holm_updateDrawableSize {
    CGFloat scale = self.contentScaleFactor > 0 ? self.contentScaleFactor : UIScreen.mainScreen.nativeScale;
    CGSize points = self.bounds.size;
    CGSize pixels = CGSizeMake(MAX(1.0, floor(points.width * scale)),
                               MAX(1.0, floor(points.height * scale)));
    CAMetalLayer *layer = self.metalLayer;
    if (!CGSizeEqualToSize(layer.drawableSize, pixels)) {
        layer.drawableSize = pixels;
    }
}

@end
