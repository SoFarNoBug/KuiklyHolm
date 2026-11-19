#import "KRHolmSurface.h"
#import "KRHolmMetalHostView.h"
#import "KRHolmGlesHostView.h"
#import "KRHolmGlesModelHostView.h"
#import <OpenKuiklyIOSRender/KRComponentDefine.h>
#import <QuartzCore/QuartzCore.h>
#import <holmDemo/holmDemo.h>

/**
 * @CreateDate: 2026/11/14
 * @Author: jlj
 * @Description: 容器岛 — Runtime 经 HolmWgpuIosBridge（ObjC: HolmDemoHolmWgpuIosBridge；kmp-wgpu Present）+ Lite GLES2（ADR 0009 泵仍 ObjC）。
 */

static const NSInteger kHolmDefaultMaxFps = 60;
static const CGFloat kHolmDefaultRenderScale = 1.0;
static NSString *const kHolmExecutorWgpu = @"renderer-wgpu";
static NSString *const kHolmExecutorGl = @"renderer-gl";
static NSString *const kHolmBackendGlesIos = @"gles2-ios";
static NSString *const kHolmBackendGlesModel = @"gles2-model";

static CGFloat HolmNormalizeRenderScale(CGFloat value) {
    if (!(value > 0.0) || !isfinite(value)) {
        return kHolmDefaultRenderScale;
    }
    if (value > 1.0) {
        return 1.0;
    }
    return value;
}

@class KRHolmSurface;

/** Weak proxy：DisplayLink 强持 proxy，proxy 弱持 Surface，避免 target:self 环。 */
@interface KRHolmDisplayLinkProxy : NSObject
@property (nonatomic, weak, nullable) KRHolmSurface *owner;
- (void)holm_onTick:(CADisplayLink *)link;
@end

@interface KRHolmSurface ()
@property (nonatomic, strong, nullable) KRHolmMetalHostView *metalHost;
@property (nonatomic, strong, nullable) KRHolmGlesHostView *glesHost;
@property (nonatomic, strong, nullable) KRHolmGlesModelHostView *glesModelHost;
@property (nonatomic, strong, nullable) UIPanGestureRecognizer *modelPan;
@property (nonatomic, strong, nullable) CADisplayLink *displayLink;
@property (nonatomic, strong, nullable) KRHolmDisplayLinkProxy *displayLinkProxy;
@property (nonatomic, assign) uint64_t sessionHandle;
@property (nonatomic, assign) uint64_t modelHandle;
@property (nonatomic, assign) int64_t modelSessionId;
@property (nonatomic, assign) uint32_t lastSessionW;
@property (nonatomic, assign) uint32_t lastSessionH;
@property (nonatomic, copy) NSString *executor;
@property (nonatomic, copy) NSString *scene;
@property (nonatomic, copy) NSString *fragmentShader;
@property (nonatomic, copy) NSString *contentId;
@property (nonatomic, copy) NSString *contentKind;
@property (nonatomic, assign) BOOL modelMode;
@property (nonatomic, assign) BOOL playing;
@property (nonatomic, assign) NSInteger maxFps;
@property (nonatomic, assign) CGFloat renderScale;
@property (nonatomic, assign) BOOL readyFired;
@property (nonatomic, assign) BOOL blockedNotified;
@property (nonatomic, copy, nullable) NSString *pendingErrorCode;
@property (nonatomic, copy, nullable) NSString *pendingErrorMessage;
@property (nonatomic, copy, nullable) NSString *backendId;
@property (nonatomic, assign) CFTimeInterval lastFrameTs;
@property (nonatomic, assign) CFTimeInterval perfWindowTs;
@property (nonatomic, assign) NSInteger perfFrames;
@property (nonatomic, assign) CFTimeInterval minFrameInterval;
@property (nonatomic, assign) CFTimeInterval lastDrawTs;
@property (nonatomic, copy, nullable) KuiklyRenderCallback css_onReady;
@property (nonatomic, copy, nullable) KuiklyRenderCallback css_onError;
@property (nonatomic, copy, nullable) KuiklyRenderCallback css_onPerf;
@property (nonatomic, copy, nullable) KuiklyRenderCallback css_onShaderCompiled;
- (void)holm_onTick:(CADisplayLink *)link;
@end

@implementation KRHolmDisplayLinkProxy
- (void)holm_onTick:(CADisplayLink *)link {
    KRHolmSurface *owner = self.owner;
    if (owner) {
        [owner holm_onTick:link];
    }
}
@end

@implementation KRHolmSurface

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _executor = kHolmExecutorGl;
        _scene = @"";
        _fragmentShader = @"";
        _contentId = @"";
        _contentKind = @"empty";
        _modelMode = NO;
        _playing = YES;
        _maxFps = kHolmDefaultMaxFps;
        _renderScale = kHolmDefaultRenderScale;
        _sessionHandle = 0;
        _modelHandle = 0;
        _modelSessionId = 0;
        _minFrameInterval = 0;
        self.opaque = YES;
        self.backgroundColor = [UIColor colorWithRed:0.02 green:0.08 blue:0.28 alpha:1];
        [self holm_recomputeFrameInterval];
    }
    return self;
}

- (void)dealloc {
    [self holm_teardownAll];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    if (_metalHost) {
        _metalHost.frame = self.bounds;
        [_metalHost holm_updateDrawableSize];
        [self holm_resizeSessionIfNeeded];
        [self holm_resizeModelIfNeeded];
    }
    if (_glesHost) {
        _glesHost.frame = self.bounds;
        [_glesHost holm_resizeBuffersIfNeeded];
    }
    if (_glesModelHost) {
        _glesModelHost.frame = self.bounds;
        [_glesModelHost holm_resizeBuffersIfNeeded];
    }
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self holm_applyExecutorMode];
    } else {
        [self holm_teardownAll];
    }
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)setCss_executor:(NSString *)css_executor {
    NSString *next = css_executor.length ? css_executor.lowercaseString : kHolmExecutorGl;
    if (![next isEqualToString:kHolmExecutorWgpu]) {
        next = kHolmExecutorGl;
    }
    if ([next isEqualToString:_executor]) {
        return;
    }
    _executor = [next copy];
    _readyFired = NO;
    _blockedNotified = NO;
    _backendId = nil;
    if (_modelMode) {
        [self holm_ensureModelSession];
        [HolmDemoHolmIosModelBridge.shared setExecutorSessionId:_modelSessionId executor:_executor];
        [self holm_applyModelPresent];
    } else {
        [self holm_applyExecutorMode];
    }
}

- (void)setCss_contentId:(NSString *)css_contentId {
    NSString *next = css_contentId.length ? css_contentId : @"";
    if ([next isEqualToString:_contentId]) {
        return;
    }
    _contentId = [next copy];
    if (_modelMode) {
        [self holm_attachModelContent];
    }
}

- (void)setCss_contentKind:(NSString *)css_contentKind {
    NSString *kind = css_contentKind.length ? css_contentKind.lowercaseString : @"empty";
    if ([kind isEqualToString:_contentKind]) {
        return;
    }
    _contentKind = [kind copy];
    if ([kind isEqualToString:@"model"]) {
        _fragmentShader = @"";
        _modelMode = YES;
        [self holm_applyModelPresent];
        return;
    }
    if (_modelMode) {
        _modelMode = NO;
        [self holm_teardownModel];
        [self holm_applyExecutorMode];
    }
    if ([kind isEqualToString:@"empty"]) {
        _fragmentShader = @"";
        if (_sessionHandle != 0 && [self holm_isWgpu]) {
            [self holm_applyRuntimeShaders];
        } else if (_glesHost && ![self holm_isWgpu]) {
            [self holm_applyLiteShaders];
        }
    }
}

- (void)setCss_fragmentShader:(NSString *)css_fragmentShader {
    NSString *next = css_fragmentShader ?: @"";
    if ([next isEqualToString:_fragmentShader]) {
        return;
    }
    _fragmentShader = [next copy];
    // Compose 经 content 桥写入；切轨途中勿把错方言喂进当前后端。
    if (_sessionHandle != 0 && [self holm_isWgpu]) {
        [self holm_applyRuntimeShaders];
    } else if (_glesHost && ![self holm_isWgpu]) {
        [self holm_applyLiteShaders];
    }
}

- (void)setCss_scene:(NSString *)css_scene {
    NSString *next = css_scene.length ? css_scene : @"";
    if ([next isEqualToString:_scene]) {
        return;
    }
    _scene = [next copy];
    if (_sessionHandle != 0) {
        // ADR 0015 removed: [HolmDemoHolmWgpuIosBridge.shared setSceneHandle:_sessionHandle scene:_scene];
    }
}

- (void)setCss_playing:(NSNumber *)css_playing {
    _playing = css_playing.boolValue;
    if (_sessionHandle != 0) {
        [HolmDemoHolmWgpuIosBridge.shared setPlayingHandle:_sessionHandle playing:_playing];
    }
    if (_modelHandle != 0) {
        [HolmDemoHolmWgpuIosBridge.shared setPlayingModelHandle:_modelHandle playing:_playing];
    }
    if (_playing && [self holm_canPump]) {
        [self holm_startDisplayLink];
    } else if (!_playing) {
        [self holm_stopDisplayLink];
    }
}

- (void)setCss_maxFps:(NSNumber *)css_maxFps {
    NSInteger next = css_maxFps != nil ? css_maxFps.integerValue : kHolmDefaultMaxFps;
    if (next < 0) {
        next = 0;
    }
    if (next == _maxFps) {
        return;
    }
    _maxFps = next;
    [self holm_recomputeFrameInterval];
    [self holm_applyDisplayLinkFrameRate];
}

- (void)setCss_renderScale:(NSNumber *)css_renderScale {
    CGFloat next = HolmNormalizeRenderScale(
        css_renderScale != nil ? css_renderScale.doubleValue : kHolmDefaultRenderScale);
    if (fabs(next - _renderScale) < 1e-6) {
        return;
    }
    _renderScale = next;
    [_glesHost holm_setRenderScale:_renderScale];
    [_glesModelHost holm_setRenderScale:_renderScale];
    [_metalHost holm_setRenderScale:_renderScale];
    if (_metalHost) {
        [self holm_resizeSessionIfNeeded];
        [self holm_resizeModelIfNeeded];
    }
}

- (void)setCss_onReady:(KuiklyRenderCallback)css_onReady {
    _css_onReady = [css_onReady copy];
    if (_readyFired && _css_onReady) {
        [self holm_fireReady];
    }
}

- (void)setCss_onError:(KuiklyRenderCallback)css_onError {
    _css_onError = [css_onError copy];
    if (_pendingErrorCode.length && _css_onError) {
        [self holm_fireError:_pendingErrorCode message:_pendingErrorMessage ?: @""];
    }
}

- (void)setCss_onPerf:(KuiklyRenderCallback)css_onPerf {
    _css_onPerf = [css_onPerf copy];
}

- (void)setCss_onShaderCompiled:(KuiklyRenderCallback)css_onShaderCompiled {
    _css_onShaderCompiled = [css_onShaderCompiled copy];
}

#pragma mark - Executor

- (BOOL)holm_isWgpu {
    return [_executor isEqualToString:kHolmExecutorWgpu];
}

/** 对齐 HolmShaderResolve.looksLikeWgslFragment / H5：含 @fragment 或 fn fs_main。 */
- (BOOL)holm_looksLikeWgslFragment:(NSString *)src {
    NSString *t = [src stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (t.length == 0) {
        return NO;
    }
    return [t containsString:@"@fragment"] || [t containsString:@"fn fs_main"];
}

- (void)holm_applyRuntimeShaders {
    if (_sessionHandle == 0) {
        return;
    }
    NSString *custom = [_fragmentShader stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (custom.length > 0) {
        if (![self holm_looksLikeWgslFragment:custom]) {
            // 热切 Lite 途中的 GLSL：忽略，不 panic、不误报红字
            [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorWgpu];
            return;
        }
        CFAbsoluteTime t0 = CFAbsoluteTimeGetCurrent();
        BOOL ok = [HolmDemoHolmWgpuIosBridge.shared setCustomFsHandle:_sessionHandle fs:custom];
        float compileMs = (float)((CFAbsoluteTimeGetCurrent() - t0) * 1000.0);
        if (!ok) {
            NSString *code = [HolmDemoHolmWgpuIosBridge.shared lastErrorCodeHandle:_sessionHandle];
            NSString *msg = [HolmDemoHolmWgpuIosBridge.shared lastErrorMessageHandle:_sessionHandle];
            if (code.length == 0) {
                code = @"SHADER_CUSTOM";
            }
            if (msg.length == 0) {
                msg = @"Runtime setCustomFs failed";
            }
            [self holm_fireError:code message:msg];
            [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:compileMs executor:kHolmExecutorWgpu];
        } else {
            [self holm_fireShaderCompiled:YES wrapMs:0.f compileMs:compileMs executor:kHolmExecutorWgpu];
        }
    } else {
        [HolmDemoHolmWgpuIosBridge.shared setCustomFsHandle:_sessionHandle fs:@""];
        if (_scene.length) {
            // ADR 0015 removed: [HolmDemoHolmWgpuIosBridge.shared setSceneHandle:_sessionHandle scene:_scene];
        }
        // 空片元 ClearTri：ok=false，对齐 Lite 拒绝语义（审查 P2）
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorWgpu];
    }
}

- (void)holm_applyLiteShaders {
    if (!_glesHost) {
        return;
    }
    NSString *custom = [_fragmentShader stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (custom.length == 0) {
        // 首包未到：不报错；已 onReady 后被清空才 FRAGMENT_REQUIRED
        if (_readyFired) {
            [self holm_fireError:@"FRAGMENT_REQUIRED"
                         message:@"fragmentShader required (official scenes removed on iOS)"];
            [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorGl];
        }
        return;
    }
    if ([self holm_looksLikeWgslFragment:custom]) {
        // 热切 renderer-wgpu 途中的 WGSL：忽略，等 executor=renderer-wgpu
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorGl];
        return;
    }
    if (![_glesHost holm_setFragmentBody:custom]) {
        return;
    }
    _backendId = kHolmBackendGlesIos;
    if (!_readyFired) {
        _readyFired = YES;
        _blockedNotified = NO;
        _pendingErrorCode = nil;
        _pendingErrorMessage = nil;
        [self holm_fireReady];
    }
    [self holm_startDisplayLink];
}

- (void)holm_applyExecutorMode {
    if (!self.window) {
        return;
    }
    if (_modelMode) {
        [self holm_applyModelPresent];
        return;
    }
    if ([self holm_isWgpu]) {
        [self holm_teardownLite];
        [self holm_teardownModel];
        [self holm_startRuntime];
    } else {
        [self holm_teardownRuntime];
        [self holm_teardownModel];
        [self holm_startLite];
    }
}

- (void)holm_teardownAll {
    [self holm_teardownModel];
    [self holm_teardownRuntime];
    [self holm_teardownLite];
}

#pragma mark - Model Present

- (void)holm_applyModelPresent {
    if (!self.window || !_modelMode) {
        return;
    }
    [self holm_ensureModelSession];
    // 卸 shader 层，保留 orbit/pending mesh 在 Bridge session
    [self holm_teardownLite];
    if ([self holm_isWgpu]) {
        [self holm_teardownGlesModelOnly];
        [self holm_startModelWgpu];
    } else {
        [self holm_teardownModelWgpuOnly];
        [self holm_startModelGles];
    }
    [self holm_attachModelContent];
    [self holm_ensureModelPan];
}

- (void)holm_ensureModelSession {
    if (_modelSessionId != 0) {
        return;
    }
    _modelSessionId = [HolmDemoHolmIosModelBridge.shared createSession];
}

- (void)holm_attachModelContent {
    if (!_modelMode) {
        return;
    }
    if (_contentId.length == 0) {
        return;
    }
    [self holm_ensureModelSession];
    [HolmDemoHolmIosModelBridge.shared attachSessionId:_modelSessionId
                                             contentId:_contentId
                                              executor:_executor];
    NSString *errCode = [HolmDemoHolmIosModelBridge.shared lastErrorCodeSessionId:_modelSessionId];
    if (errCode.length > 0) {
        NSString *msg = [HolmDemoHolmIosModelBridge.shared lastErrorMessageSessionId:_modelSessionId];
        [self holm_fireError:errCode message:msg.length ? msg : @"model attach failed"];
        [HolmDemoHolmIosModelBridge.shared clearErrorSessionId:_modelSessionId];
    }
}

- (void)holm_teardownModel {
    [self holm_teardownGlesModelOnly];
    [self holm_teardownModelWgpuOnly];
    if (_modelPan) {
        [self removeGestureRecognizer:_modelPan];
        _modelPan = nil;
    }
    if (_modelSessionId != 0) {
        [HolmDemoHolmIosModelBridge.shared destroySessionSessionId:_modelSessionId];
        _modelSessionId = 0;
    }
    _modelMode = NO;
}

- (void)holm_teardownGlesModelOnly {
    if (_glesModelHost) {
        [_glesModelHost holm_teardownGL];
        [_glesModelHost removeFromSuperview];
        _glesModelHost = nil;
    }
}

- (void)holm_teardownModelWgpuOnly {
    if (_modelHandle != 0) {
        [HolmDemoHolmWgpuIosBridge.shared destroyModelHandle:_modelHandle];
        _modelHandle = 0;
    }
    if (_sessionHandle == 0 && _metalHost) {
        [_metalHost removeFromSuperview];
        _metalHost = nil;
        _lastSessionW = 0;
        _lastSessionH = 0;
    }
}

- (void)holm_startModelGles {
    NSString *providerErr = [HolmDemoHolmWgpuIosBridge.shared requireProviderExecutor:kHolmExecutorGl];
    if (providerErr.length > 0) {
        [self holm_fireBlocked:providerErr];
        return;
    }
    if (!_glesModelHost) {
        _glesModelHost = [[KRHolmGlesModelHostView alloc] initWithFrame:self.bounds];
        _glesModelHost.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [self addSubview:_glesModelHost];
    }
    [_glesModelHost holm_setRenderScale:_renderScale];
    if (![_glesModelHost holm_setupGL]) {
        [self holm_fireError:@"MODEL_GL" message:@"KRHolmGlesModelHostView setupGL failed"];
        return;
    }
    _backendId = kHolmBackendGlesModel;
    [self holm_startDisplayLink];
}

- (void)holm_startModelWgpu {
    NSString *providerErr = [HolmDemoHolmWgpuIosBridge.shared requireProviderExecutor:kHolmExecutorWgpu];
    if (providerErr.length > 0) {
        [self holm_fireBlocked:providerErr];
        return;
    }
    if (_modelHandle != 0) {
        [self holm_startDisplayLink];
        return;
    }
    if (![HolmDemoHolmWgpuIosBridge.shared hasWgpu]) {
        [self holm_fireBlocked:@"kmp_wgpu has_wgpu=0. No silent GLES stand-in."];
        return;
    }
    if (!_metalHost) {
        _metalHost = [[KRHolmMetalHostView alloc] initWithFrame:self.bounds];
        _metalHost.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [self addSubview:_metalHost];
    }
    [_metalHost holm_setRenderScale:_renderScale];
    [_metalHost holm_updateDrawableSize];
    if (_metalHost.metalLayer.device == nil) {
        [self holm_fireBlocked:@"CAMetalLayer.device is nil. No silent GLES stand-in."];
        return;
    }
    CGSize px = _metalHost.metalLayer.drawableSize;
    int32_t w = (int32_t)MAX(1.0, px.width);
    int32_t h = (int32_t)MAX(1.0, px.height);
    int64_t layerPtr = (int64_t)(__bridge void *)_metalHost.metalLayer;
    int64_t handle = [HolmDemoHolmWgpuIosBridge.shared createModelMetalLayerPtr:layerPtr width:w height:h];
    if (handle == 0) {
        NSString *detail = [HolmDemoHolmWgpuIosBridge.shared lastCreateError];
        [self holm_fireBlocked:[NSString stringWithFormat:
            @"ModelPass create failed: %@. No silent GLES stand-in.",
            detail.length ? detail : @"(no detail)"]];
        return;
    }
    _modelHandle = (uint64_t)handle;
    _lastSessionW = (uint32_t)w;
    _lastSessionH = (uint32_t)h;
    NSString *backend = [HolmDemoHolmWgpuIosBridge.shared backendIdModelHandle:_modelHandle];
    _backendId = backend.length ? backend : @"wgpu-m0";
    [self holm_startDisplayLink];
}

- (void)holm_resizeModelIfNeeded {
    if (_modelHandle == 0 || !_metalHost) {
        return;
    }
    CGSize px = _metalHost.metalLayer.drawableSize;
    uint32_t w = (uint32_t)MAX(1.0, px.width);
    uint32_t h = (uint32_t)MAX(1.0, px.height);
    if (w == _lastSessionW && h == _lastSessionH) {
        return;
    }
    _lastSessionW = w;
    _lastSessionH = h;
    [HolmDemoHolmWgpuIosBridge.shared resizeModelHandle:_modelHandle width:(int32_t)w height:(int32_t)h];
}

- (void)holm_ensureModelPan {
    if (_modelPan) {
        return;
    }
    _modelPan = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(holm_onModelPan:)];
    [self addGestureRecognizer:_modelPan];
}

- (void)holm_onModelPan:(UIPanGestureRecognizer *)gr {
    if (!_modelMode || _modelSessionId == 0) {
        return;
    }
    CGPoint t = [gr translationInView:self];
    [gr setTranslation:CGPointZero inView:self];
    float dx = (float)t.x;
    float dy = (float)t.y;
    HolmDemoHolmIosModelBridge *bridge = HolmDemoHolmIosModelBridge.shared;
    [bridge nudgeOrbitSessionId:_modelSessionId dx:dx dy:dy];
    if (_glesModelHost) {
        [_glesModelHost holm_setOrbitYaw:[bridge yawSessionId:_modelSessionId]
                                  pitch:[bridge pitchSessionId:_modelSessionId]
                                enabled:[bridge orbitEnabledSessionId:_modelSessionId]];
    }
}

- (void)holm_consumeModelUploadIfAny {
    if (_modelSessionId == 0) {
        return;
    }
    HolmDemoHolmIosModelBridge *bridge = HolmDemoHolmIosModelBridge.shared;
    HolmDemoHolmIosModelSnapshot *snap = [bridge consumeSnapshotSessionId:_modelSessionId];
    if (!snap) {
        return;
    }
    if (!_glesModelHost && _modelHandle == 0) {
        // 无 Present 宿主：还回 pending，下帧再试（P2-consume-snap-no-host）
        [bridge requeueSnapshotSessionId:_modelSessionId snapshot:snap];
        return;
    }
    if (_glesModelHost) {
        [_glesModelHost holm_uploadSnapshot:snap];
        HolmDemoKotlinFloatArray *light = [bridge lightDirIntensitySessionId:_modelSessionId];
        BOOL lit = light.size > 0 && [light getIndex:0] > 0.5f;
        float lx = light.size > 1 ? [light getIndex:1] : 0.3f;
        float ly = light.size > 2 ? [light getIndex:2] : 0.8f;
        float lz = light.size > 3 ? [light getIndex:3] : 0.4f;
        float inten = light.size > 4 ? [light getIndex:4] : 1.f;
        [_glesModelHost holm_setLit:lit lightDirX:lx y:ly z:lz intensity:inten];
        if (!_readyFired) {
            _readyFired = YES;
            _blockedNotified = NO;
            _pendingErrorCode = nil;
            _pendingErrorMessage = nil;
            _backendId = kHolmBackendGlesModel;
            [self holm_fireReady];
        }
    } else if (_modelHandle != 0) {
        BOOL ok = [HolmDemoHolmWgpuIosBridge.shared setMeshesModelHandle:_modelHandle
                                                                  verts:snap.verts
                                                                indices:snap.indices];
        if (!ok) {
            [bridge requeueSnapshotSessionId:_modelSessionId snapshot:snap];
            [self holm_fireError:@"MODEL_MESH" message:@"ModelPass setMeshes failed"];
            return;
        }
        if (!_readyFired) {
            _readyFired = YES;
            _blockedNotified = NO;
            _pendingErrorCode = nil;
            _pendingErrorMessage = nil;
            NSString *backend = [HolmDemoHolmWgpuIosBridge.shared backendIdModelHandle:_modelHandle];
            _backendId = backend.length ? backend : @"wgpu-m0";
            [self holm_fireReady];
        }
    }
}

#pragma mark - Lite

- (void)holm_startLite {
    NSString *providerErr = [HolmDemoHolmWgpuIosBridge.shared requireProviderExecutor:kHolmExecutorGl];
    if (providerErr.length > 0) {
        [self holm_teardownLite];
        [self holm_fireBlocked:providerErr];
        return;
    }
    if (_glesHost && _glesHost.hasProgram) {
        _backendId = kHolmBackendGlesIos;
        if (!_readyFired) {
            _readyFired = YES;
            _pendingErrorCode = nil;
            _pendingErrorMessage = nil;
            [self holm_fireReady];
        }
        [self holm_startDisplayLink];
        return;
    }
    if (!_glesHost) {
        _glesHost = [[KRHolmGlesHostView alloc] initWithFrame:self.bounds];
        _glesHost.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [self addSubview:_glesHost];
    }
    [_glesHost holm_setRenderScale:_renderScale];
    {
        __weak typeof(self) weakSelf = self;
        _glesHost.onError = ^(NSString *code, NSString *message) {
            __strong typeof(weakSelf) strongSelf = weakSelf;
            if (strongSelf) {
                [strongSelf holm_fireError:code message:message];
            }
        };
        _glesHost.onShaderCompiled = ^(BOOL ok, float wrapMs, float compileMs) {
            __strong typeof(weakSelf) strongSelf = weakSelf;
            if (strongSelf) {
                [strongSelf holm_fireShaderCompiled:ok
                                            wrapMs:wrapMs
                                         compileMs:compileMs
                                          executor:kHolmExecutorGl];
            }
        };
    }
    if (![_glesHost holm_setupGL]) {
        return;
    }

    NSString *custom = [_fragmentShader stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (custom.length == 0) {
        // didMoveToWindow 常早于 Compose 属性；等 setCss_fragmentShader，勿抢报 FRAGMENT_REQUIRED
        self.backgroundColor = [UIColor colorWithRed:0.02 green:0.08 blue:0.28 alpha:1];
        [self holm_startDisplayLink];
        return;
    }
    if ([self holm_looksLikeWgslFragment:custom]) {
        [self holm_fireError:@"SHADER_CUSTOM"
                     message:@"Lite fragmentShader must be GLSL Shadertoy body (WGSL ignored)"];
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorGl];
        [self holm_startDisplayLink];
        return;
    }
    if (![_glesHost holm_setFragmentBody:custom]) {
        // onError already fired; keep clear path
        [self holm_startDisplayLink];
        return;
    }

    _backendId = kHolmBackendGlesIos;
    _readyFired = YES;
    _blockedNotified = NO;
    _pendingErrorCode = nil;
    _pendingErrorMessage = nil;
    [self holm_fireReady];
    [self holm_startDisplayLink];
}

- (void)holm_teardownLite {
    [self holm_stopDisplayLink];
    if (_glesHost) {
        [_glesHost holm_teardownGL];
        [_glesHost removeFromSuperview];
        _glesHost = nil;
    }
    _lastFrameTs = 0;
    _perfWindowTs = 0;
    _perfFrames = 0;
    _lastDrawTs = 0;
}

#pragma mark - Runtime

- (void)holm_startRuntime {
    if (_sessionHandle != 0) {
        [self holm_startDisplayLink];
        return;
    }
    NSString *providerErr = [HolmDemoHolmWgpuIosBridge.shared requireProviderExecutor:kHolmExecutorWgpu];
    if (providerErr.length > 0) {
        [self holm_teardownRuntime];
        [self holm_fireBlocked:providerErr];
        return;
    }
    if (!_metalHost) {
        _metalHost = [[KRHolmMetalHostView alloc] initWithFrame:self.bounds];
        _metalHost.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [self addSubview:_metalHost];
    }
    [_metalHost holm_setRenderScale:_renderScale];
    [_metalHost holm_updateDrawableSize];
    CGSize px = _metalHost.metalLayer.drawableSize;
    int32_t w = (int32_t)MAX(1.0, px.width);
    int32_t h = (int32_t)MAX(1.0, px.height);

    if (![HolmDemoHolmWgpuIosBridge.shared hasWgpu]) {
        [self holm_teardownRuntime];
        [self holm_fireBlocked:@"kmp_wgpu has_wgpu=0. No silent GLES stand-in."];
        return;
    }

    if (_metalHost.metalLayer.device == nil) {
        [self holm_teardownRuntime];
        [self holm_fireBlocked:@"CAMetalLayer.device is nil (MTLCreateSystemDefaultDevice failed). No silent GLES stand-in."];
        return;
    }

    int64_t layerPtr = (int64_t)(__bridge void *)_metalHost.metalLayer;
    int64_t handle = [HolmDemoHolmWgpuIosBridge.shared createMetalLayerPtr:layerPtr width:w height:h];
    if (handle == 0) {
        NSString *detail = [HolmDemoHolmWgpuIosBridge.shared lastCreateError];
        if (detail.length == 0) {
            detail = @"(no detail)";
        }
        [self holm_teardownRuntime];
        [self holm_fireBlocked:[NSString stringWithFormat:
            @"HolmDemoHolmWgpuIosBridge create failed: %@. No silent GLES stand-in.", detail]];
        return;
    }
    _sessionHandle = (uint64_t)handle;
    _lastSessionW = (uint32_t)w;
    _lastSessionH = (uint32_t)h;
    [HolmDemoHolmWgpuIosBridge.shared setPlayingHandle:_sessionHandle playing:_playing];
    if (_fragmentShader.length > 0 &&
        ![self holm_looksLikeWgslFragment:_fragmentShader] &&
        [self holm_isWgpu]) {
        [self holm_teardownRuntime];
        [self holm_fireError:@"SHADER_CUSTOM"
                     message:@"Runtime fragmentShader must be WGSL (non-WGSL custom ignored)"];
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorWgpu];
        return;
    }
    [self holm_applyRuntimeShaders];

    NSString *backend = [HolmDemoHolmWgpuIosBridge.shared backendIdHandle:_sessionHandle];
    _backendId = backend.length ? backend : @"wgpu-m0";

    float dt = 1.f / 60.f;
    if (![HolmDemoHolmWgpuIosBridge.shared tickHandle:_sessionHandle dtSecs:dt]) {
        [self holm_teardownRuntime];
        [self holm_fireBlocked:@"kmp_wgpu tick failed after create. No silent GLES stand-in."];
        return;
    }

    _readyFired = YES;
    _blockedNotified = NO;
    _pendingErrorCode = nil;
    _pendingErrorMessage = nil;
    [self holm_fireReady];
    [self holm_startDisplayLink];
}

- (void)holm_teardownRuntime {
    [self holm_stopDisplayLink];
    if (_sessionHandle != 0) {
        [HolmDemoHolmWgpuIosBridge.shared destroyHandle:_sessionHandle];
        _sessionHandle = 0;
    }
    if (_metalHost) {
        [_metalHost removeFromSuperview];
        _metalHost = nil;
    }
    _lastSessionW = 0;
    _lastSessionH = 0;
    _lastFrameTs = 0;
    _perfWindowTs = 0;
    _perfFrames = 0;
    _lastDrawTs = 0;
}

- (void)holm_resizeSessionIfNeeded {
    if (_sessionHandle == 0 || !_metalHost) {
        return;
    }
    CGSize px = _metalHost.metalLayer.drawableSize;
    uint32_t w = (uint32_t)MAX(1.0, px.width);
    uint32_t h = (uint32_t)MAX(1.0, px.height);
    if (w == _lastSessionW && h == _lastSessionH) {
        return;
    }
    _lastSessionW = w;
    _lastSessionH = h;
    [HolmDemoHolmWgpuIosBridge.shared resizeHandle:_sessionHandle width:(int32_t)w height:(int32_t)h];
}

#pragma mark - DisplayLink

- (void)holm_recomputeFrameInterval {
    if (_maxFps <= 0) {
        _minFrameInterval = 0;
    } else {
        _minFrameInterval = 1.0 / (double)_maxFps;
    }
}

- (void)holm_applyDisplayLinkFrameRate {
    if (!_displayLink) {
        return;
    }
    if (@available(iOS 15.0, *)) {
        if (_maxFps <= 0) {
            _displayLink.preferredFrameRateRange = CAFrameRateRangeMake(30, 120, 60);
        } else {
            float fps = (float)_maxFps;
            _displayLink.preferredFrameRateRange = CAFrameRateRangeMake(fps, fps, fps);
        }
    } else {
        _displayLink.preferredFramesPerSecond = _maxFps > 0 ? (NSInteger)_maxFps : 0;
    }
}

- (BOOL)holm_canPump {
    if (_modelMode) {
        if ([self holm_isWgpu]) {
            return _modelHandle != 0;
        }
        return _glesModelHost != nil;
    }
    if ([self holm_isWgpu]) {
        return _sessionHandle != 0;
    }
    return _glesHost != nil;
}

- (void)holm_startDisplayLink {
    if (_displayLink || !_playing || ![self holm_canPump]) {
        return;
    }
    if (!_displayLinkProxy) {
        _displayLinkProxy = [[KRHolmDisplayLinkProxy alloc] init];
        _displayLinkProxy.owner = self;
    }
    _displayLink = [CADisplayLink displayLinkWithTarget:_displayLinkProxy
                                               selector:@selector(holm_onTick:)];
    [self holm_applyDisplayLinkFrameRate];
    [_displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
}

- (void)holm_stopDisplayLink {
    [_displayLink invalidate];
    _displayLink = nil;
    _displayLinkProxy = nil;
}

- (void)holm_onTick:(CADisplayLink *)link {
    if (![self holm_canPump]) {
        return;
    }
    CFTimeInterval now = link.timestamp;
    if (_minFrameInterval > 0 && _lastDrawTs > 0 && (now - _lastDrawTs) < _minFrameInterval * 0.92) {
        return;
    }
    float dt = 0.f;
    if (_lastFrameTs > 0) {
        dt = (float)MAX(0.0, now - _lastFrameTs);
    } else {
        dt = (float)link.duration;
    }
    _lastFrameTs = now;

    if (_modelMode) {
        HolmDemoHolmIosModelBridge *bridge = HolmDemoHolmIosModelBridge.shared;
        NSString *errCode = [bridge lastErrorCodeSessionId:_modelSessionId];
        if (errCode.length > 0) {
            NSString *msg = [bridge lastErrorMessageSessionId:_modelSessionId];
            [self holm_fireError:errCode message:msg.length ? msg : @"model load failed"];
            [bridge clearErrorSessionId:_modelSessionId];
        }
        [self holm_consumeModelUploadIfAny];
        if (_glesModelHost) {
            HolmDemoKotlinFloatArray *light = [bridge lightDirIntensitySessionId:_modelSessionId];
            BOOL lit = light.size > 0 && [light getIndex:0] > 0.5f;
            float lx = light.size > 1 ? [light getIndex:1] : 0.3f;
            float ly = light.size > 2 ? [light getIndex:2] : 0.8f;
            float lz = light.size > 3 ? [light getIndex:3] : 0.4f;
            float inten = light.size > 4 ? [light getIndex:4] : 1.f;
            [_glesModelHost holm_setLit:lit lightDirX:lx y:ly z:lz intensity:inten];
            [_glesModelHost holm_drawFrame];
        } else if (_modelHandle != 0) {
            CGSize px = _metalHost.metalLayer.drawableSize;
            float aspect = (float)MAX(1.0, px.width) / (float)MAX(1.0, px.height);
            if (![HolmDemoHolmWgpuIosBridge.shared presentModelFrameHandle:_modelHandle
                                                               sessionId:_modelSessionId
                                                                  aspect:aspect
                                                                  dtSecs:dt
                                                                 playing:_playing]) {
                if (_readyFired && !_blockedNotified) {
                    _blockedNotified = YES;
                    NSString *code = [HolmDemoHolmWgpuIosBridge.shared lastErrorCodeModelHandle:_modelHandle];
                    NSString *msg = [HolmDemoHolmWgpuIosBridge.shared lastErrorMessageModelHandle:_modelHandle];
                    [self holm_fireError:code.length ? code : @"TICK"
                                 message:msg.length ? msg : @"ModelPass tick failed"];
                }
            }
        }
    } else if ([self holm_isWgpu]) {
        if (![HolmDemoHolmWgpuIosBridge.shared tickHandle:_sessionHandle dtSecs:dt]) {
            if (!_blockedNotified) {
                _blockedNotified = YES;
                NSString *code = [HolmDemoHolmWgpuIosBridge.shared lastErrorCodeHandle:_sessionHandle];
                NSString *msg = [HolmDemoHolmWgpuIosBridge.shared lastErrorMessageHandle:_sessionHandle];
                if (code.length == 0) {
                    code = @"TICK";
                }
                if (msg.length == 0) {
                    msg = @"kmp_wgpu tick failed. No silent GLES stand-in.";
                }
                [self holm_fireError:code message:msg];
            }
            [self holm_stopDisplayLink];
            return;
        }
    } else {
        [_glesHost holm_drawFrame:dt];
    }
    _lastDrawTs = now;
    [self holm_notePerfFrame:now];
}

- (void)holm_notePerfFrame:(CFTimeInterval)now {
    if (!_css_onPerf) {
        return;
    }
    if (_perfWindowTs <= 0) {
        _perfWindowTs = now;
        _perfFrames = 0;
    }
    _perfFrames += 1;
    CFTimeInterval elapsed = now - _perfWindowTs;
    if (elapsed >= 0.5) {
        double fps = _perfFrames / elapsed;
        double frameMs = elapsed * 1000.0 / MAX(1, _perfFrames);
        CGSize px = CGSizeMake(1, 1);
        if (_metalHost) {
            px = _metalHost.metalLayer.drawableSize;
        } else if (_glesModelHost) {
            px = _glesModelHost.drawablePixelSize;
        } else if (_glesHost) {
            px = _glesHost.drawablePixelSize;
        }
        _css_onPerf(@{
            @"fps": @((NSInteger)lround(fps)),
            @"frameMs": @(frameMs),
            @"width": @((NSInteger)px.width),
            @"height": @((NSInteger)px.height),
        });
        _perfWindowTs = now;
        _perfFrames = 0;
    }
}

#pragma mark - Events

- (void)holm_fireReady {
    if (!_css_onReady) {
        return;
    }
    NSString *backend = _backendId;
    if (!backend) {
        if (_modelMode && ![self holm_isWgpu]) {
            backend = kHolmBackendGlesModel;
        } else {
            backend = [self holm_isWgpu] ? @"wgpu-m0" : kHolmBackendGlesIos;
        }
    }
    _css_onReady(@{
        @"backend": backend,
        @"quality": @"auto",
        @"scene": _scene ?: @"",
    });
}

- (void)holm_fireBlocked:(NSString *)message {
    [self holm_fireError:@"BLOCKED" message:message];
}

- (void)holm_fireError:(NSString *)code message:(NSString *)message {
    _pendingErrorCode = [code copy] ?: @"";
    _pendingErrorMessage = [message copy] ?: @"";
    if (!_css_onError) {
        return;
    }
    _css_onError(@{ @"code": _pendingErrorCode, @"message": _pendingErrorMessage });
}

- (void)holm_fireShaderCompiled:(BOOL)ok
                        wrapMs:(float)wrapMs
                     compileMs:(float)compileMs
                      executor:(NSString *)executor {
    if (!_css_onShaderCompiled) {
        return;
    }
    _css_onShaderCompiled(@{
        @"ok": @(ok),
        @"wrapMs": @(wrapMs),
        @"compileMs": @(compileMs),
        @"executor": executor ?: @"",
    });
}

@end
