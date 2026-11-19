#import "KRHolmGlesModelHostView.h"
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import <OpenGLES/EAGL.h>
#import <holmDemo/holmDemo.h>
#import <math.h>
#import <stdlib.h>
#import <string.h>

/**
 * @CreateDate: 2026/11/19
 * @Author: jlj
 * @Description: CAEAGLLayer 网格 Present；Lambert + orbit + cull back。
 */

typedef struct {
    GLuint vbo;
    GLuint ibo;
    GLsizei indexCount;
} KRHolmGpuMesh;

@interface KRHolmGlesModelHostView ()
@property (nonatomic, strong, nullable) EAGLContext *glContext;
@property (nonatomic, assign) GLuint framebuffer;
@property (nonatomic, assign) GLuint colorbuffer;
@property (nonatomic, assign) GLuint depthbuffer;
@property (nonatomic, assign) GLuint program;
@property (nonatomic, assign) GLint uMvp;
@property (nonatomic, assign) GLint uColor;
@property (nonatomic, assign) GLint uLightDir;
@property (nonatomic, assign) GLint uLit;
@property (nonatomic, assign) GLint uLightIntensity;
@property (nonatomic, assign) GLint aPos;
@property (nonatomic, assign) GLint aNrm;
@property (nonatomic, assign) GLint viewW;
@property (nonatomic, assign) GLint viewH;
@property (nonatomic, assign) CGFloat renderScale;
@property (nonatomic, assign) float yaw;
@property (nonatomic, assign) float pitch;
@property (nonatomic, assign) BOOL orbitEnabled;
@property (nonatomic, assign) float colorR;
@property (nonatomic, assign) float colorG;
@property (nonatomic, assign) float colorB;
@property (nonatomic, assign) float colorA;
@property (nonatomic, assign) BOOL lit;
@property (nonatomic, assign) float lightX;
@property (nonatomic, assign) float lightY;
@property (nonatomic, assign) float lightZ;
@property (nonatomic, assign) float lightIntensity;
@property (nonatomic, assign) float centerX;
@property (nonatomic, assign) float centerY;
@property (nonatomic, assign) float centerZ;
@property (nonatomic, assign) float radius;
@property (nonatomic, assign) KRHolmGpuMesh *meshes;
@property (nonatomic, assign) NSInteger meshCount;
@end

@implementation KRHolmGlesModelHostView

+ (Class)layerClass {
    return [CAEAGLLayer class];
}

- (CAEAGLLayer *)eaglLayer {
    return (CAEAGLLayer *)self.layer;
}

- (BOOL)hasMesh {
    return _meshCount > 0 && _program != 0;
}

- (CGSize)drawablePixelSize {
    return CGSizeMake(MAX(1, _viewW), MAX(1, _viewH));
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _viewW = 1;
        _viewH = 1;
        _renderScale = 1.0;
        _yaw = 0.4f;
        _pitch = 0.3f;
        _orbitEnabled = YES;
        _colorR = 0.85f;
        _colorG = 0.55f;
        _colorB = 0.25f;
        _colorA = 1.f;
        _lit = NO;
        _lightX = 0.3f;
        _lightY = 0.8f;
        _lightZ = 0.4f;
        _lightIntensity = 1.f;
        _radius = 1.f;
        self.opaque = YES;
        self.contentScaleFactor = _renderScale;
        self.backgroundColor = [UIColor colorWithRed:0.02 green:0.08 blue:0.18 alpha:1];
        CAEAGLLayer *layer = self.eaglLayer;
        layer.opaque = YES;
        layer.contentsScale = _renderScale;
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

- (void)holm_setRenderScale:(CGFloat)scale {
    CGFloat next = scale;
    if (!(next > 0.0) || !isfinite(next)) {
        next = 1.0;
    }
    if (next > 1.0) {
        next = 1.0;
    }
    if (fabs(next - _renderScale) < 1e-6) {
        return;
    }
    _renderScale = next;
    self.contentScaleFactor = _renderScale;
    self.eaglLayer.contentsScale = _renderScale;
    [self holm_resizeBuffersIfNeeded];
}

- (BOOL)holm_setupGL {
    if (_glContext) {
        return YES;
    }
    EAGLContext *ctx = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    if (!ctx || ![EAGLContext setCurrentContext:ctx]) {
        return NO;
    }
    _glContext = ctx;
    glClearColor(0.02f, 0.08f, 0.18f, 1.f);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    glDepthMask(GL_TRUE);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    glFrontFace(GL_CCW);
    _program = [self holm_linkProgram];
    if (_program == 0) {
        return NO;
    }
    _uMvp = glGetUniformLocation(_program, "u_mvp");
    _uColor = glGetUniformLocation(_program, "u_color");
    _uLightDir = glGetUniformLocation(_program, "u_lightDir");
    _uLit = glGetUniformLocation(_program, "u_lit");
    _uLightIntensity = glGetUniformLocation(_program, "u_lightIntensity");
    _aPos = glGetAttribLocation(_program, "a_pos");
    _aNrm = glGetAttribLocation(_program, "a_nrm");
    [self holm_resizeBuffersIfNeeded];
    return YES;
}

- (void)holm_teardownGL {
    if (_glContext) {
        [EAGLContext setCurrentContext:_glContext];
        [self holm_clearMesh];
        if (_program) {
            glDeleteProgram(_program);
            _program = 0;
        }
        if (_framebuffer) {
            glDeleteFramebuffers(1, &_framebuffer);
            _framebuffer = 0;
        }
        if (_colorbuffer) {
            glDeleteRenderbuffers(1, &_colorbuffer);
            _colorbuffer = 0;
        }
        if (_depthbuffer) {
            glDeleteRenderbuffers(1, &_depthbuffer);
            _depthbuffer = 0;
        }
        if ([EAGLContext currentContext] == _glContext) {
            [EAGLContext setCurrentContext:nil];
        }
        _glContext = nil;
    }
}

- (void)holm_resizeBuffersIfNeeded {
    if (!_glContext) {
        return;
    }
    [EAGLContext setCurrentContext:_glContext];
    CGFloat scale = self.contentScaleFactor;
    GLint w = (GLint)MAX(1.0, (double)(self.bounds.size.width * scale));
    GLint h = (GLint)MAX(1.0, (double)(self.bounds.size.height * scale));
    if (w == _viewW && h == _viewH && _framebuffer != 0) {
        return;
    }
    _viewW = w;
    _viewH = h;
    if (_framebuffer) {
        glDeleteFramebuffers(1, &_framebuffer);
        _framebuffer = 0;
    }
    if (_colorbuffer) {
        glDeleteRenderbuffers(1, &_colorbuffer);
        _colorbuffer = 0;
    }
    if (_depthbuffer) {
        glDeleteRenderbuffers(1, &_depthbuffer);
        _depthbuffer = 0;
    }
    glGenFramebuffers(1, &_framebuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, _framebuffer);
    glGenRenderbuffers(1, &_colorbuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, _colorbuffer);
    [_glContext renderbufferStorage:GL_RENDERBUFFER fromDrawable:self.eaglLayer];
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, _colorbuffer);
    glGenRenderbuffers(1, &_depthbuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, _depthbuffer);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24_OES, _viewW, _viewH);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, _depthbuffer);
    glViewport(0, 0, _viewW, _viewH);
}

- (void)holm_setOrbitYaw:(float)yaw pitch:(float)pitch enabled:(BOOL)enabled {
    _yaw = yaw;
    _pitch = fminf(1.2f, fmaxf(-1.2f, pitch));
    _orbitEnabled = enabled;
}

- (void)holm_nudgeOrbitDx:(float)dx dy:(float)dy {
    if (!_orbitEnabled) {
        return;
    }
    // 与 Android / Bridge 同向
    _yaw -= dx * 0.01f;
    _pitch = fminf(1.2f, fmaxf(-1.2f, _pitch - dy * 0.01f));
}

- (void)holm_setColorR:(float)r g:(float)g b:(float)b a:(float)a {
    _colorR = r;
    _colorG = g;
    _colorB = b;
    _colorA = a;
}

- (void)holm_setLit:(BOOL)lit
          lightDirX:(float)x
                  y:(float)y
                  z:(float)z
          intensity:(float)intensity {
    _lit = lit;
    _lightX = x;
    _lightY = y;
    _lightZ = z;
    _lightIntensity = fmaxf(0.f, intensity);
}

- (void)holm_clearMesh {
    if (_glContext) {
        [EAGLContext setCurrentContext:_glContext];
    }
    if (_meshes && _meshCount > 0) {
        for (NSInteger i = 0; i < _meshCount; i++) {
            GLuint bufs[2] = { _meshes[i].vbo, _meshes[i].ibo };
            glDeleteBuffers(2, bufs);
        }
        free(_meshes);
        _meshes = NULL;
        _meshCount = 0;
    }
}

- (void)holm_uploadSnapshot:(HolmDemoHolmIosModelSnapshot *)snapshot {
    if (!snapshot || !_glContext) {
        return;
    }
    [EAGLContext setCurrentContext:_glContext];
    [self holm_clearMesh];
    _centerX = snapshot.centerX;
    _centerY = snapshot.centerY;
    _centerZ = snapshot.centerZ;
    _radius = snapshot.radius > 0.f ? snapshot.radius : 1.f;
    _colorR = snapshot.colorR;
    _colorG = snapshot.colorG;
    _colorB = snapshot.colorB;
    _colorA = snapshot.colorA;
    _yaw = snapshot.yaw;
    _pitch = snapshot.pitch;
    _orbitEnabled = snapshot.orbitEnabled;

    HolmDemoKotlinFloatArray *vertsK = snapshot.verts;
    HolmDemoKotlinIntArray *indicesK = snapshot.indices;
    int32_t vertFloats = (int32_t)vertsK.size;
    int32_t indexCount = (int32_t)indicesK.size;
    if (vertFloats < 6 || indexCount < 3) {
        return;
    }
    // 单合并 mesh（与 Android pack 后一次 draw 等价）
    float *verts = (float *)malloc((size_t)vertFloats * sizeof(float));
    int32_t *indices = (int32_t *)malloc((size_t)indexCount * sizeof(int32_t));
    if (!verts || !indices) {
        free(verts);
        free(indices);
        return;
    }
    for (int32_t i = 0; i < vertFloats; i++) {
        verts[i] = [vertsK getIndex:i];
    }
    for (int32_t i = 0; i < indexCount; i++) {
        indices[i] = [indicesK getIndex:i];
    }
    KRHolmGpuMesh mesh = {0};
    glGenBuffers(1, &mesh.vbo);
    glGenBuffers(1, &mesh.ibo);
    glBindBuffer(GL_ARRAY_BUFFER, mesh.vbo);
    glBufferData(GL_ARRAY_BUFFER, (GLsizeiptr)vertFloats * (GLsizeiptr)sizeof(float), verts, GL_STATIC_DRAW);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.ibo);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, (GLsizeiptr)indexCount * (GLsizeiptr)sizeof(int32_t), indices, GL_STATIC_DRAW);
    mesh.indexCount = (GLsizei)indexCount;
    free(verts);
    free(indices);
    _meshes = (KRHolmGpuMesh *)malloc(sizeof(KRHolmGpuMesh));
    if (_meshes) {
        _meshes[0] = mesh;
        _meshCount = 1;
    } else {
        GLuint bufs[2] = { mesh.vbo, mesh.ibo };
        glDeleteBuffers(2, bufs);
    }
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
}

- (void)holm_drawFrame {
    if (!_glContext || _framebuffer == 0) {
        return;
    }
    [EAGLContext setCurrentContext:_glContext];
    glBindFramebuffer(GL_FRAMEBUFFER, _framebuffer);
    glViewport(0, 0, _viewW, _viewH);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    if (_program == 0 || _meshCount == 0) {
        glBindRenderbuffer(GL_RENDERBUFFER, _colorbuffer);
        [_glContext presentRenderbuffer:GL_RENDERBUFFER];
        return;
    }
    glUseProgram(_program);
    float mvp[16];
    [self holm_computeMvp:mvp];
    glUniformMatrix4fv(_uMvp, 1, GL_FALSE, mvp);
    glUniform4f(_uColor, _colorR, _colorG, _colorB, _colorA);
    glUniform1i(_uLit, _lit ? 1 : 0);
    if (_lit) {
        float len = sqrtf(_lightX * _lightX + _lightY * _lightY + _lightZ * _lightZ);
        if (len < 1e-5f) {
            len = 1e-5f;
        }
        glUniform3f(_uLightDir, _lightX / len, _lightY / len, _lightZ / len);
        glUniform1f(_uLightIntensity, _lightIntensity);
    } else {
        glUniform3f(_uLightDir, 0.3f, 0.8f, 0.4f);
        glUniform1f(_uLightIntensity, 1.f);
    }
    for (NSInteger i = 0; i < _meshCount; i++) {
        KRHolmGpuMesh m = _meshes[i];
        glBindBuffer(GL_ARRAY_BUFFER, m.vbo);
        glEnableVertexAttribArray((GLuint)_aPos);
        glVertexAttribPointer((GLuint)_aPos, 3, GL_FLOAT, GL_FALSE, 24, (const void *)0);
        glEnableVertexAttribArray((GLuint)_aNrm);
        glVertexAttribPointer((GLuint)_aNrm, 3, GL_FLOAT, GL_FALSE, 24, (const void *)12);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m.ibo);
        glDrawElements(GL_TRIANGLES, m.indexCount, GL_UNSIGNED_INT, (const void *)0);
    }
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    glBindRenderbuffer(GL_RENDERBUFFER, _colorbuffer);
    [_glContext presentRenderbuffer:GL_RENDERBUFFER];
}

- (void)holm_computeMvp:(float *)out {
    float view[16];
    float proj[16];
    float eyeDist = _radius * 3.2f;
    float cy = cosf(_pitch);
    float eyeX = _centerX + eyeDist * cy * sinf(_yaw);
    float eyeY = _centerY + eyeDist * sinf(_pitch);
    float eyeZ = _centerZ + eyeDist * cy * cosf(_yaw);
    [self holm_lookAt:view
                 eyeX:eyeX eyeY:eyeY eyeZ:eyeZ
              centerX:_centerX centerY:_centerY centerZ:_centerZ
                  upX:0 upY:1 upZ:0];
    float aspect = (float)_viewW / (float)MAX(1, _viewH);
    float zNear = fmaxf(eyeDist * 0.05f, 0.01f);
    float zFar = fmaxf(eyeDist * 10.f, zNear * 100.f);
    [self holm_perspective:proj fovyDeg:45.f aspect:aspect zNear:zNear zFar:zFar];
    [self holm_multiplyMM:out lhs:proj rhs:view];
}

- (void)holm_lookAt:(float *)rm
               eyeX:(float)eyeX eyeY:(float)eyeY eyeZ:(float)eyeZ
            centerX:(float)centerX centerY:(float)centerY centerZ:(float)centerZ
                upX:(float)upX upY:(float)upY upZ:(float)upZ {
    float fx = centerX - eyeX;
    float fy = centerY - eyeY;
    float fz = centerZ - eyeZ;
    float rlf = 1.f / fmaxf(sqrtf(fx * fx + fy * fy + fz * fz), 1e-8f);
    fx *= rlf; fy *= rlf; fz *= rlf;
    float sx = fy * upZ - fz * upY;
    float sy = fz * upX - fx * upZ;
    float sz = fx * upY - fy * upX;
    float rls = 1.f / fmaxf(sqrtf(sx * sx + sy * sy + sz * sz), 1e-8f);
    sx *= rls; sy *= rls; sz *= rls;
    float ux = sy * fz - sz * fy;
    float uy = sz * fx - sx * fz;
    float uz = sx * fy - sy * fx;
    rm[0] = sx; rm[1] = ux; rm[2] = -fx; rm[3] = 0.f;
    rm[4] = sy; rm[5] = uy; rm[6] = -fy; rm[7] = 0.f;
    rm[8] = sz; rm[9] = uz; rm[10] = -fz; rm[11] = 0.f;
    rm[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ);
    rm[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ);
    rm[14] = fx * eyeX + fy * eyeY + fz * eyeZ;
    rm[15] = 1.f;
}

- (void)holm_perspective:(float *)m fovyDeg:(float)fovyDeg aspect:(float)aspect zNear:(float)zNear zFar:(float)zFar {
    float f = 1.f / tanf(fovyDeg * ((float)M_PI / 180.f) / 2.f);
    float rangeInv = 1.f / (zNear - zFar);
    memset(m, 0, 16 * sizeof(float));
    m[0] = f / aspect;
    m[5] = f;
    m[10] = (zFar + zNear) * rangeInv;
    m[11] = -1.f;
    m[14] = 2.f * zFar * zNear * rangeInv;
}

- (void)holm_multiplyMM:(float *)result lhs:(const float *)lhs rhs:(const float *)rhs {
    for (int c = 0; c < 4; c++) {
        for (int r = 0; r < 4; r++) {
            result[c * 4 + r] =
                lhs[r] * rhs[c * 4] +
                lhs[4 + r] * rhs[c * 4 + 1] +
                lhs[8 + r] * rhs[c * 4 + 2] +
                lhs[12 + r] * rhs[c * 4 + 3];
        }
    }
}

- (GLuint)holm_linkProgram {
    const char *vsSrc =
        "attribute vec3 a_pos;\n"
        "attribute vec3 a_nrm;\n"
        "uniform mat4 u_mvp;\n"
        "varying vec3 v_nrm;\n"
        "void main(){\n"
        "  v_nrm=a_nrm;\n"
        "  gl_Position=u_mvp*vec4(a_pos,1.0);\n"
        "}\n";
    const char *fsSrc =
        "precision mediump float;\n"
        "varying vec3 v_nrm;\n"
        "uniform vec4 u_color;\n"
        "uniform vec3 u_lightDir;\n"
        "uniform int u_lit;\n"
        "uniform float u_lightIntensity;\n"
        "void main(){\n"
        "  if(u_lit==0){ gl_FragColor=u_color; }\n"
        "  else {\n"
        "    float ndl=max(dot(normalize(v_nrm),normalize(u_lightDir)),0.15);\n"
        "    gl_FragColor=vec4(u_color.rgb*ndl*u_lightIntensity,u_color.a);\n"
        "  }\n"
        "}\n";
    GLuint vs = [self holm_compile:GL_VERTEX_SHADER src:vsSrc];
    GLuint fs = [self holm_compile:GL_FRAGMENT_SHADER src:fsSrc];
    GLuint p = glCreateProgram();
    glAttachShader(p, vs);
    glAttachShader(p, fs);
    glLinkProgram(p);
    glDeleteShader(vs);
    glDeleteShader(fs);
    return p;
}

- (GLuint)holm_compile:(GLenum)type src:(const char *)src {
    GLuint s = glCreateShader(type);
    glShaderSource(s, 1, &src, NULL);
    glCompileShader(s);
    return s;
}

@end
