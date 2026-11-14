#import "KRHolmSurface.h"
#import <OpenKuiklyIOSRender/KRComponentDefine.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import <QuartzCore/QuartzCore.h>

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: DualTrack A — iOS GLES2 Holm 岛；五席已删，无宿主片元则清屏+error。
 */

@interface KRHolmSurface ()
@property (nonatomic, strong) CAEAGLLayer *eaglLayer;
@property (nonatomic, strong) EAGLContext *glContext;
@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, assign) GLuint framebuffer;
@property (nonatomic, assign) GLuint renderbuffer;
@property (nonatomic, assign) GLuint program;
@property (nonatomic, copy) NSString *scene;
@property (nonatomic, assign) BOOL playing;
@property (nonatomic, assign) float liquidBoost;
@property (nonatomic, assign) float liquidBend;
@property (nonatomic, assign) BOOL readyFired;
@property (nonatomic, copy, nullable) NSString *pendingErrorCode;
@property (nonatomic, copy, nullable) NSString *pendingErrorMessage;
@property (nonatomic, copy, nullable) KuiklyRenderCallback css_onReady;
@property (nonatomic, copy, nullable) KuiklyRenderCallback css_onError;
@end

@implementation KRHolmSurface

@synthesize hr_rootView;

+ (Class)layerClass {
    return [CAEAGLLayer class];
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _scene = @"";
        _playing = YES;
        _liquidBoost = 1.35f;
        _liquidBend = 0.05f;
        [self setupGL];
    }
    return self;
}

- (void)dealloc {
    [self tearDownGL];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [self resizeBuffers];
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self startDisplayLink];
    } else {
        [self stopDisplayLink];
    }
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)setCss_scene:(NSString *)css_scene {
    NSString *next = css_scene.length ? css_scene : @"";
    if ([next isEqualToString:_scene]) {
        return;
    }
    _scene = [next copy];
    [self rebuildProgram];
}

- (void)setCss_playing:(NSNumber *)css_playing {
    _playing = css_playing.boolValue;
    if (_playing) {
        [self startDisplayLink];
    } else {
        [self stopDisplayLink];
    }
}

- (void)setCss_liquidBoost:(NSNumber *)css_liquidBoost {
    _liquidBoost = css_liquidBoost.floatValue;
}

- (void)setCss_liquidBend:(NSNumber *)css_liquidBend {
    _liquidBend = css_liquidBend.floatValue;
}

- (void)setCss_onReady:(KuiklyRenderCallback)css_onReady {
    _css_onReady = [css_onReady copy];
    if (_readyFired && _css_onReady) {
        [self fireReady];
    }
}

- (void)setCss_onError:(KuiklyRenderCallback)css_onError {
    _css_onError = [css_onError copy];
    if (_pendingErrorCode.length && _css_onError) {
        [self fireError:_pendingErrorCode message:_pendingErrorMessage ?: @""];
    }
}

#pragma mark - GL

- (void)setupGL {
    self.opaque = YES;
    self.backgroundColor = [UIColor colorWithRed:0.05 green:0.07 blue:0.10 alpha:1];
    _eaglLayer = (CAEAGLLayer *)self.layer;
    _eaglLayer.opaque = YES;
    _eaglLayer.drawableProperties = @{
        kEAGLDrawablePropertyRetainedBacking: @NO,
        kEAGLDrawablePropertyColorFormat: kEAGLColorFormatRGBA8
    };
    _glContext = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    if (!_glContext || ![EAGLContext setCurrentContext:_glContext]) {
        [self fireError:@"NO_GLES" message:@"OpenGLES2 unavailable"];
        return;
    }
    glGenFramebuffers(1, &_framebuffer);
    glGenRenderbuffers(1, &_renderbuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, _framebuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, _renderbuffer);
    [_glContext renderbufferStorage:GL_RENDERBUFFER fromDrawable:_eaglLayer];
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, _renderbuffer);

    [self rebuildProgram];
    [self resizeBuffers];
    if (_program != 0 && !_readyFired) {
        _readyFired = YES;
        [self fireReady];
    }
}

- (void)tearDownGL {
    [self stopDisplayLink];
    if (_glContext) {
        [EAGLContext setCurrentContext:_glContext];
        if (_program) {
            glDeleteProgram(_program);
            _program = 0;
        }
        if (_framebuffer) {
            glDeleteFramebuffers(1, &_framebuffer);
            _framebuffer = 0;
        }
        if (_renderbuffer) {
            glDeleteRenderbuffers(1, &_renderbuffer);
            _renderbuffer = 0;
        }
        if ([EAGLContext currentContext] == _glContext) {
            [EAGLContext setCurrentContext:nil];
        }
        _glContext = nil;
    }
}

- (void)resizeBuffers {
    if (!_glContext) {
        return;
    }
    [EAGLContext setCurrentContext:_glContext];
    glBindRenderbuffer(GL_RENDERBUFFER, _renderbuffer);
    [_glContext renderbufferStorage:GL_RENDERBUFFER fromDrawable:_eaglLayer];
    GLint w = 0, h = 0;
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &w);
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &h);
    if (w > 0 && h > 0) {
        glViewport(0, 0, w, h);
    }
}

- (void)rebuildProgram {
    if (!_glContext) {
        return;
    }
    [EAGLContext setCurrentContext:_glContext];
    if (_program) {
        glDeleteProgram(_program);
        _program = 0;
    }
    // 官方五席已删；本波未接 fragmentShader Attr → 诚实失败 + 清屏
    [self fireError:@"FRAGMENT_REQUIRED"
            message:@"fragmentShader required (official scenes removed on iOS)"];
}

- (void)startDisplayLink {
    if (_displayLink || !_playing || !_glContext) {
        return;
    }
    _displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(onTick:)];
    [_displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
}

- (void)stopDisplayLink {
    [_displayLink invalidate];
    _displayLink = nil;
}

- (void)onTick:(CADisplayLink *)link {
    if (!_glContext) {
        return;
    }
    (void)link;
    [EAGLContext setCurrentContext:_glContext];
    glBindFramebuffer(GL_FRAMEBUFFER, _framebuffer);
    glClearColor(0.05f, 0.07f, 0.1f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    glBindRenderbuffer(GL_RENDERBUFFER, _renderbuffer);
    [_glContext presentRenderbuffer:GL_RENDERBUFFER];
}

- (void)fireReady {
    if (!_css_onReady) {
        return;
    }
    NSDictionary *payload = @{
        @"backend": @"gles2-ios",
        @"quality": @"auto",
        @"scene": _scene ?: @""
    };
    _css_onReady(payload);
}

- (void)fireError:(NSString *)code message:(NSString *)message {
    _pendingErrorCode = [code copy] ?: @"";
    _pendingErrorMessage = [message copy] ?: @"";
    if (!_css_onError) {
        return;
    }
    _css_onError(@{ @"code": _pendingErrorCode, @"message": _pendingErrorMessage });
}

@end
