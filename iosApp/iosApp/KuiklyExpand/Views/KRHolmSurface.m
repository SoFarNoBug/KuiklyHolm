#import "KRHolmSurface.h"
#import <OpenKuiklyIOSRender/KRComponentDefine.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import <QuartzCore/QuartzCore.h>

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: DualTrack A — iOS GLES2 Holm 岛；对齐 Android/H5 同源 shader 字符串。
 */

static NSString *const kHolmVert = @"attribute vec2 a_position;\n"
"varying vec2 v_uv;\n"
"void main() {\n"
"  v_uv = a_position * 0.5 + 0.5;\n"
"  gl_Position = vec4(a_position, 0.0, 1.0);\n"
"}\n";

/* 与 FrostShaderSource.gles2Fragment 对齐（精简同步；改 common 时请同步本串） */
static NSString *const kHolmFrostFrag = @"precision mediump float;\n"
"varying vec2 v_uv;\n"
"uniform float u_time;\n"
"float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}\n"
"vec3 sceneColor(vec2 uv){\n"
"  vec2 p=uv+vec2(u_time*0.04,u_time*0.02);\n"
"  float stripe=step(0.5,fract(p.x*6.0+p.y*1.5));\n"
"  vec3 a=vec3(0.15,0.45,0.95); vec3 b=vec3(0.95,0.35,0.25);\n"
"  vec3 col=mix(a,b,stripe);\n"
"  float d=length(p-vec2(0.35+0.1*sin(u_time*0.7),0.55));\n"
"  col=mix(col,vec3(0.2,0.9,0.55),1.0-smoothstep(0.12,0.22,d));\n"
"  float d2=length(p-vec2(0.72,0.32+0.08*cos(u_time*0.5)));\n"
"  col=mix(col,vec3(0.95,0.85,0.2),1.0-smoothstep(0.08,0.16,d2));\n"
"  return col;\n"
"}\n"
"vec3 blurScene(vec2 uv){\n"
"  vec2 r=vec2(0.012,0.012); vec3 s=sceneColor(uv);\n"
"  s+=sceneColor(uv+vec2(r.x,0.0)); s+=sceneColor(uv+vec2(-r.x,0.0));\n"
"  s+=sceneColor(uv+vec2(0.0,r.y)); s+=sceneColor(uv+vec2(0.0,-r.y));\n"
"  return s*0.2;\n"
"}\n"
"void main(){\n"
"  vec2 uv=v_uv; vec3 col=blurScene(uv);\n"
"  float n=hash(uv*vec2(240.0)+u_time)*0.035;\n"
"  col+=vec3(n); col=mix(col,vec3(0.92,0.95,1.0),0.18);\n"
"  float edge=smoothstep(1.15,0.25,length(uv-0.5));\n"
"  col*=0.75+0.35*edge; gl_FragColor=vec4(col,1.0);\n"
"}\n";

/* liquid_approx：骨架同源（UV/fresnel/boost/bend）；端侧允许精简，非逐字同步 common */
static NSString *const kHolmLiquidFrag = @"precision mediump float;\n"
"varying vec2 v_uv;\n"
"uniform float u_time;\n"
"uniform float u_liquidBoost;\n"
"uniform float u_liquidBend;\n"
"float sdRoundBox(vec2 p,vec2 b,float r){vec2 q=abs(p)-b+r;return length(max(q,0.0))+min(max(q.x,q.y),0.0)-r;}\n"
"vec3 sceneColor(vec2 uv){\n"
"  vec2 p=uv+vec2(u_time*0.035,u_time*0.018);\n"
"  float stripe=smoothstep(0.35,0.65,fract(p.x*5.5+p.y*1.4));\n"
"  vec3 col=mix(vec3(0.12,0.42,0.95),vec3(0.95,0.32,0.22),stripe);\n"
"  float d=length(p-vec2(0.35+0.1*sin(u_time*0.55),0.55));\n"
"  col=mix(col,vec3(0.15,0.92,0.55),1.0-smoothstep(0.12,0.22,d));\n"
"  return col;\n"
"}\n"
"vec3 softBlur(vec2 uv){\n"
"  vec2 r=vec2(0.010,0.024); vec3 s=sceneColor(uv);\n"
"  s+=sceneColor(uv+vec2(r.x,0.0)); s+=sceneColor(uv+vec2(-r.x,0.0));\n"
"  s+=sceneColor(uv+vec2(0.0,r.y)); s+=sceneColor(uv+vec2(0.0,-r.y));\n"
"  return s*0.2;\n"
"}\n"
"void main(){\n"
"  vec2 uv=v_uv; vec2 p=uv-0.5;\n"
"  float glass=sdRoundBox(p,vec2(0.492,0.472),0.055);\n"
"  float inside=1.0-smoothstep(-0.008,0.012,glass);\n"
"  float rim=1.0-smoothstep(0.0,0.055,abs(glass));\n"
"  float fresnel=pow(clamp(rim,0.0,1.0),1.6);\n"
"  float boostK=max(u_liquidBoost-1.0,0.0);\n"
"  float bendAmt=u_liquidBend*fresnel*(0.85+0.65*boostK);\n"
"  vec2 nDir=vec2(p.x*0.42,p.y); float nLen=length(nDir);\n"
"  vec2 n=nLen>0.001?(nDir/nLen):vec2(0.0);\n"
"  float wave=sin(u_time*0.85+p.x*7.0+p.y*9.0)*0.3+sin(u_time*0.45+p.y*10.0)*0.22;\n"
"  vec2 offset=n*bendAmt*(0.5+0.5*wave); offset.x*=0.45;\n"
"  vec2 sampleUv=uv+offset*inside;\n"
"  vec3 col=softBlur(sampleUv);\n"
"  col=mix(col,vec3(0.88,0.92,0.98),0.08*inside+0.05*fresnel);\n"
"  col+=vec3(0.92,0.96,1.0)*pow(fresnel,2.4)*(0.18+0.22*boostK);\n"
"  vec3 outside=sceneColor(uv)*0.88;\n"
"  col=mix(outside,col,inside); gl_FragColor=vec4(col,1.0);\n"
"}\n";

@interface KRHolmSurface ()
@property (nonatomic, strong) CAEAGLLayer *eaglLayer;
@property (nonatomic, strong) EAGLContext *glContext;
@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, assign) GLuint framebuffer;
@property (nonatomic, assign) GLuint renderbuffer;
@property (nonatomic, assign) GLuint program;
@property (nonatomic, assign) GLint timeLoc;
@property (nonatomic, assign) GLint boostLoc;
@property (nonatomic, assign) GLint bendLoc;
@property (nonatomic, assign) GLint posLoc;
@property (nonatomic, assign) GLuint vbo;
@property (nonatomic, copy) NSString *scene;
@property (nonatomic, assign) BOOL playing;
@property (nonatomic, assign) float liquidBoost;
@property (nonatomic, assign) float liquidBend;
@property (nonatomic, assign) CFTimeInterval animTime;
@property (nonatomic, assign) CFTimeInterval lastTs;
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
        _scene = @"frost";
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
    NSString *next = css_scene.length ? css_scene : @"frost";
    if ([next isEqualToString:_scene] && _program != 0) {
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

    GLfloat verts[] = { -1.f, -1.f, 1.f, -1.f, -1.f, 1.f, 1.f, 1.f };
    glGenBuffers(1, &_vbo);
    glBindBuffer(GL_ARRAY_BUFFER, _vbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(verts), verts, GL_STATIC_DRAW);

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
        if (_vbo) {
            glDeleteBuffers(1, &_vbo);
            _vbo = 0;
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

- (GLuint)compileShader:(GLenum)type source:(NSString *)source {
    GLuint shader = glCreateShader(type);
    const char *c = source.UTF8String;
    glShaderSource(shader, 1, &c, NULL);
    glCompileShader(shader);
    GLint ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        GLint len = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &len);
        NSMutableData *data = [NSMutableData dataWithLength:MAX(len, 1)];
        glGetShaderInfoLog(shader, len, NULL, data.mutableBytes);
        NSString *log = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] ?: @"compile failed";
        [self fireError:@"SHADER_COMPILE" message:log];
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

- (void)rebuildProgram {
    if (!_glContext) {
        return;
    }
    [EAGLContext setCurrentContext:_glContext];
    NSString *frag = [self.scene isEqualToString:@"liquid_approx"] ? kHolmLiquidFrag : kHolmFrostFrag;
    GLuint vs = [self compileShader:GL_VERTEX_SHADER source:kHolmVert];
    GLuint fs = [self compileShader:GL_FRAGMENT_SHADER source:frag];
    if (!vs || !fs) {
        return;
    }
    GLuint prog = glCreateProgram();
    glAttachShader(prog, vs);
    glAttachShader(prog, fs);
    glLinkProgram(prog);
    GLint linked = 0;
    glGetProgramiv(prog, GL_LINK_STATUS, &linked);
    glDeleteShader(vs);
    glDeleteShader(fs);
    if (!linked) {
        [self fireError:@"SHADER_LINK" message:@"link failed"];
        glDeleteProgram(prog);
        return;
    }
    if (_program) {
        glDeleteProgram(_program);
    }
    _program = prog;
    _posLoc = glGetAttribLocation(_program, "a_position");
    _timeLoc = glGetUniformLocation(_program, "u_time");
    _boostLoc = glGetUniformLocation(_program, "u_liquidBoost");
    _bendLoc = glGetUniformLocation(_program, "u_liquidBend");
}

- (void)startDisplayLink {
    if (_displayLink || !_playing || !_glContext) {
        return;
    }
    _displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(onTick:)];
    [_displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
    _lastTs = 0;
}

- (void)stopDisplayLink {
    [_displayLink invalidate];
    _displayLink = nil;
}

- (void)onTick:(CADisplayLink *)link {
    if (!_program || !_glContext) {
        return;
    }
    CFTimeInterval now = CACurrentMediaTime();
    if (_lastTs > 0) {
        float dt = (float)(now - _lastTs);
        if (dt > 0.05f) {
            dt = 0.05f;
        }
        if (dt > 0) {
            _animTime += dt;
        }
    }
    _lastTs = now;
    [EAGLContext setCurrentContext:_glContext];
    glBindFramebuffer(GL_FRAMEBUFFER, _framebuffer);
    glClearColor(0.05f, 0.07f, 0.1f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    glUseProgram(_program);
    if (_timeLoc >= 0) {
        glUniform1f(_timeLoc, (GLfloat)_animTime);
    }
    if (_boostLoc >= 0) {
        glUniform1f(_boostLoc, _liquidBoost);
    }
    if (_bendLoc >= 0) {
        glUniform1f(_bendLoc, _liquidBend);
    }
    glBindBuffer(GL_ARRAY_BUFFER, _vbo);
    glEnableVertexAttribArray(_posLoc);
    glVertexAttribPointer(_posLoc, 2, GL_FLOAT, GL_FALSE, 0, NULL);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableVertexAttribArray(_posLoc);
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
        @"scene": _scene ?: @"frost"
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
