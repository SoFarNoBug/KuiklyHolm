#import "KRHolmGlesHostView.h"
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import <OpenGLES/EAGL.h>
#import <holmDemo/holmDemo.h>

/**
 * @CreateDate: 2026/11/14
 * @Author: jlj
 * @Description: CAEAGLLayer Lite 宿主；wrap/vert 走 holmDemo HolmShadertoyShell / HolmBuiltinShaders（单源）。
 * 缓冲用 1x（点像素），对齐 DualTrack「不默认乘 DPR」。
 */

@interface KRHolmGlesHostView ()
@property (nonatomic, strong, nullable) EAGLContext *glContext;
@property (nonatomic, assign) GLuint framebuffer;
@property (nonatomic, assign) GLuint renderbuffer;
@property (nonatomic, assign) GLuint program;
@property (nonatomic, assign) GLint timeLoc;
@property (nonatomic, assign) GLint resolutionLoc;
@property (nonatomic, assign) GLint liquidBoostLoc;
@property (nonatomic, assign) GLint liquidBendLoc;
@property (nonatomic, assign) GLint posLoc;
@property (nonatomic, assign) float animTimeSec;
@property (nonatomic, assign) float liquidBoost;
@property (nonatomic, assign) float liquidBend;
@property (nonatomic, assign) GLint viewW;
@property (nonatomic, assign) GLint viewH;
@property (nonatomic, copy, nullable) NSString *fragmentBody;
@end

@implementation KRHolmGlesHostView

+ (Class)layerClass {
    return [CAEAGLLayer class];
}

- (CAEAGLLayer *)eaglLayer {
    return (CAEAGLLayer *)self.layer;
}

- (BOOL)hasProgram {
    return _program != 0;
}

- (CGSize)drawablePixelSize {
    return CGSizeMake(MAX(1, _viewW), MAX(1, _viewH));
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _liquidBoost = 1.35f;
        _liquidBend = 0.05f;
        _timeLoc = -1;
        _resolutionLoc = -1;
        _liquidBoostLoc = -1;
        _liquidBendLoc = -1;
        _posLoc = -1;
        _viewW = 1;
        _viewH = 1;
        self.opaque = YES;
        // 1x：与 H5 Lite/旧 iOS 壳一致；nativeScale 下 raymarch 易堵主线程 → fps≈0
        self.contentScaleFactor = 1.0;
        self.backgroundColor = [UIColor colorWithRed:0.02 green:0.08 blue:0.28 alpha:1];
        CAEAGLLayer *layer = self.eaglLayer;
        layer.opaque = YES;
        layer.contentsScale = 1.0;
        layer.drawableProperties = @{
            kEAGLDrawablePropertyRetainedBacking: @NO,
            kEAGLDrawablePropertyColorFormat: kEAGLColorFormatRGBA8,
        };
    }
    return self;
}

- (void)dealloc {
    [self holm_teardownGL];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [self holm_resizeBuffersIfNeeded];
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        self.contentScaleFactor = 1.0;
        self.eaglLayer.contentsScale = 1.0;
        [self holm_resizeBuffersIfNeeded];
    }
}

- (BOOL)holm_setupGL {
    if (_glContext) {
        return YES;
    }
    _glContext = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    if (!_glContext || ![EAGLContext setCurrentContext:_glContext]) {
        [self holm_emitError:@"NO_GLES" message:@"OpenGLES2 unavailable"];
        _glContext = nil;
        return NO;
    }
    glGenFramebuffers(1, &_framebuffer);
    glGenRenderbuffers(1, &_renderbuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, _framebuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, _renderbuffer);
    [_glContext renderbufferStorage:GL_RENDERBUFFER fromDrawable:self.eaglLayer];
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, _renderbuffer);
    [self holm_resizeBuffersIfNeeded];
    return YES;
}

- (void)holm_teardownGL {
    if (!_glContext) {
        return;
    }
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
    _fragmentBody = nil;
    _animTimeSec = 0;
}

- (void)holm_resizeBuffersIfNeeded {
    if (!_glContext) {
        return;
    }
    CGFloat scale = self.contentScaleFactor > 0 ? self.contentScaleFactor : 1.0;
    GLint wantW = (GLint)MAX(1.0, floor(self.bounds.size.width * scale));
    GLint wantH = (GLint)MAX(1.0, floor(self.bounds.size.height * scale));
    if (wantW == _viewW && wantH == _viewH && _viewW > 0 && _viewH > 0) {
        return;
    }
    [EAGLContext setCurrentContext:_glContext];
    glBindRenderbuffer(GL_RENDERBUFFER, _renderbuffer);
    [_glContext renderbufferStorage:GL_RENDERBUFFER fromDrawable:self.eaglLayer];
    GLint w = 0, h = 0;
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &w);
    glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &h);
    if (w > 0 && h > 0) {
        _viewW = w;
        _viewH = h;
        glViewport(0, 0, w, h);
    }
}

- (void)holm_setLiquidBoost:(float)boost bend:(float)bend {
    _liquidBoost = boost;
    _liquidBend = bend;
}

- (BOOL)holm_setFragmentBody:(NSString *)body {
    NSString *trimmed = [body stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (trimmed.length == 0) {
        return NO;
    }
    if ([trimmed isEqualToString:_fragmentBody] && _program != 0) {
        return YES;
    }
    if (!_glContext && ![self holm_setupGL]) {
        return NO;
    }
    [EAGLContext setCurrentContext:_glContext];
    NSString *fragSrc = nil;
    NSString *vertSrc = nil;
    CFAbsoluteTime wrapT0 = CFAbsoluteTimeGetCurrent();
    @try {
        fragSrc = [HolmDemoHolmShadertoyShell.shared wrapFragmentBody:trimmed];
        vertSrc = HolmDemoHolmBuiltinShaders.shared.gles2Vertex;
    } @catch (NSException *ex) {
        float wrapMs = (float)((CFAbsoluteTimeGetCurrent() - wrapT0) * 1000.0);
        [self holm_emitError:@"SHADER_WRAP" message:ex.reason ?: @"wrap failed"];
        [self holm_emitCompiled:NO wrapMs:wrapMs compileMs:0.f];
        return NO;
    }
    float wrapMs = (float)((CFAbsoluteTimeGetCurrent() - wrapT0) * 1000.0);
    if (fragSrc.length == 0 || vertSrc.length == 0) {
        [self holm_emitError:@"SHADER_WRAP" message:@"empty wrap/vert from holmDemo"];
        [self holm_emitCompiled:NO wrapMs:wrapMs compileMs:0.f];
        return NO;
    }
    CFAbsoluteTime compileT0 = CFAbsoluteTimeGetCurrent();
    GLuint vert = [self holm_compileShader:GL_VERTEX_SHADER source:vertSrc];
    GLuint frag = [self holm_compileShader:GL_FRAGMENT_SHADER source:fragSrc];
    if (vert == 0 || frag == 0) {
        if (vert) {
            glDeleteShader(vert);
        }
        if (frag) {
            glDeleteShader(frag);
        }
        float compileMs = (float)((CFAbsoluteTimeGetCurrent() - compileT0) * 1000.0);
        [self holm_emitCompiled:NO wrapMs:wrapMs compileMs:compileMs];
        return NO;
    }
    GLuint program = glCreateProgram();
    glAttachShader(program, vert);
    glAttachShader(program, frag);
    glLinkProgram(program);
    glDeleteShader(vert);
    glDeleteShader(frag);
    GLint link = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &link);
    if (!link) {
        GLint logLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &logLen);
        NSString *log = @"link failed";
        if (logLen > 1) {
            NSMutableData *buf = [NSMutableData dataWithLength:(NSUInteger)logLen];
            glGetProgramInfoLog(program, logLen, NULL, buf.mutableBytes);
            log = [[NSString alloc] initWithData:buf encoding:NSUTF8StringEncoding] ?: log;
        }
        glDeleteProgram(program);
        float compileMs = (float)((CFAbsoluteTimeGetCurrent() - compileT0) * 1000.0);
        [self holm_emitError:@"SHADER_LINK" message:log];
        [self holm_emitCompiled:NO wrapMs:wrapMs compileMs:compileMs];
        return NO;
    }
    float compileMs = (float)((CFAbsoluteTimeGetCurrent() - compileT0) * 1000.0);
    if (_program) {
        glDeleteProgram(_program);
    }
    _program = program;
    _fragmentBody = [trimmed copy];
    _posLoc = glGetAttribLocation(_program, "a_position");
    _timeLoc = glGetUniformLocation(_program, "u_time");
    _resolutionLoc = glGetUniformLocation(_program, "u_resolution");
    _liquidBoostLoc = glGetUniformLocation(_program, "u_liquidBoost");
    _liquidBendLoc = glGetUniformLocation(_program, "u_liquidBend");
    [self holm_emitCompiled:YES wrapMs:wrapMs compileMs:compileMs];
    return YES;
}

- (GLuint)holm_compileShader:(GLenum)type source:(NSString *)source {
    GLuint shader = glCreateShader(type);
    const char *cSrc = source.UTF8String;
    GLint len = (GLint)[source lengthOfBytesUsingEncoding:NSUTF8StringEncoding];
    glShaderSource(shader, 1, &cSrc, &len);
    glCompileShader(shader);
    GLint status = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &status);
    if (!status) {
        GLint logLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLen);
        NSString *log = @"compile failed";
        if (logLen > 1) {
            NSMutableData *buf = [NSMutableData dataWithLength:(NSUInteger)logLen];
            glGetShaderInfoLog(shader, logLen, NULL, buf.mutableBytes);
            log = [[NSString alloc] initWithData:buf encoding:NSUTF8StringEncoding] ?: log;
        }
        glDeleteShader(shader);
        [self holm_emitError:@"SHADER_COMPILE" message:log];
        return 0;
    }
    return shader;
}

- (void)holm_drawFrame:(float)dt {
    if (!_glContext) {
        return;
    }
    [EAGLContext setCurrentContext:_glContext];
    glBindFramebuffer(GL_FRAMEBUFFER, _framebuffer);
    float clamped = MAX(0.f, MIN(dt, 0.1f));
    _animTimeSec += clamped;
    if (_program == 0) {
        glClearColor(0.02f, 0.08f, 0.28f, 1.f);
        glClear(GL_COLOR_BUFFER_BIT);
    } else {
        glClearColor(0.05f, 0.07f, 0.1f, 1.f);
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(_program);
        if (_timeLoc >= 0) {
            glUniform1f(_timeLoc, _animTimeSec);
        }
        if (_resolutionLoc >= 0) {
            glUniform2f(_resolutionLoc, (GLfloat)_viewW, (GLfloat)_viewH);
        }
        if (_liquidBoostLoc >= 0) {
            glUniform1f(_liquidBoostLoc, _liquidBoost);
        }
        if (_liquidBendLoc >= 0) {
            glUniform1f(_liquidBendLoc, _liquidBend);
        }
        static const GLfloat kQuad[] = {
            -1.f, -1.f,
            1.f, -1.f,
            -1.f, 1.f,
            1.f, 1.f,
        };
        if (_posLoc >= 0) {
            glEnableVertexAttribArray((GLuint)_posLoc);
            glVertexAttribPointer((GLuint)_posLoc, 2, GL_FLOAT, GL_FALSE, 0, kQuad);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            glDisableVertexAttribArray((GLuint)_posLoc);
        }
    }
    glBindRenderbuffer(GL_RENDERBUFFER, _renderbuffer);
    [_glContext presentRenderbuffer:GL_RENDERBUFFER];
}

- (void)holm_emitError:(NSString *)code message:(NSString *)message {
    if (_onError) {
        _onError(code ?: @"", message ?: @"");
    }
}

- (void)holm_emitCompiled:(BOOL)ok wrapMs:(float)wrapMs compileMs:(float)compileMs {
    if (_onShaderCompiled) {
        _onShaderCompiled(ok, wrapMs, compileMs);
    }
}

@end
