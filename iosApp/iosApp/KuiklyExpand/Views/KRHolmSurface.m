#import "KRHolmSurface.h"
#import "KRHolmMetalHostView.h"
#import "KRHolmGlesHostView.h"
#import <OpenKuiklyIOSRender/KRComponentDefine.h>
#import <QuartzCore/QuartzCore.h>
#import "holm_runtime.h"

/**
 * @CreateDate: 2026/11/14
 * @Author: jlj
 * @Description: 容器岛 — Runtime Metal C Session + Lite GLES2 子 View（ADR 0009 后单）。
 */

static const NSInteger kHolmDefaultMaxFps = 60;
static const CGFloat kHolmDefaultRenderScale = 1.0;
static NSString *const kHolmExecutorRuntime = @"runtime";
static NSString *const kHolmExecutorLite = @"lite";
static NSString *const kHolmBackendGlesIos = @"gles2-ios";

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
@property (nonatomic, strong, nullable) CADisplayLink *displayLink;
@property (nonatomic, strong, nullable) KRHolmDisplayLinkProxy *displayLinkProxy;
@property (nonatomic, assign) uint64_t sessionHandle;
@property (nonatomic, assign) uint32_t lastSessionW;
@property (nonatomic, assign) uint32_t lastSessionH;
@property (nonatomic, copy) NSString *executor;
@property (nonatomic, copy) NSString *scene;
@property (nonatomic, copy) NSString *fragmentShader;
@property (nonatomic, assign) BOOL playing;
@property (nonatomic, assign) NSInteger maxFps;
@property (nonatomic, assign) CGFloat renderScale;
@property (nonatomic, assign) float liquidBoost;
@property (nonatomic, assign) float liquidBend;
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
        _executor = kHolmExecutorLite;
        _scene = @"";
        _fragmentShader = @"";
        _playing = YES;
        _maxFps = kHolmDefaultMaxFps;
        _renderScale = kHolmDefaultRenderScale;
        _liquidBoost = 1.35f;
        _liquidBend = 0.05f;
        _sessionHandle = 0;
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
    }
    if (_glesHost) {
        _glesHost.frame = self.bounds;
        [_glesHost holm_resizeBuffersIfNeeded];
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
    NSString *next = css_executor.length ? css_executor.lowercaseString : kHolmExecutorLite;
    if (![next isEqualToString:kHolmExecutorRuntime]) {
        next = kHolmExecutorLite;
    }
    if ([next isEqualToString:_executor]) {
        return;
    }
    _executor = [next copy];
    _readyFired = NO;
    _blockedNotified = NO;
    _backendId = nil;
    [self holm_applyExecutorMode];
}

- (void)setCss_fragmentShader:(NSString *)css_fragmentShader {
    NSString *next = css_fragmentShader ?: @"";
    if ([next isEqualToString:_fragmentShader]) {
        return;
    }
    _fragmentShader = [next copy];
    // Compose 先写 fragmentShader 再写 executor：切轨途中勿把错方言喂进当前后端。
    if (_sessionHandle != 0 && [self holm_isRuntime]) {
        [self holm_applyRuntimeShaders];
    } else if (_glesHost && ![self holm_isRuntime]) {
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
        holm_runtime_session_set_scene(_sessionHandle, _scene.UTF8String);
    }
}

- (void)setCss_playing:(NSNumber *)css_playing {
    _playing = css_playing.boolValue;
    if (_sessionHandle != 0) {
        holm_runtime_session_set_playing(_sessionHandle, _playing ? 1u : 0u);
    }
    if (_playing && ([self holm_isRuntime] ? _sessionHandle != 0 : _glesHost != nil)) {
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
    [_metalHost holm_setRenderScale:_renderScale];
    if (_metalHost) {
        [self holm_resizeSessionIfNeeded];
    }
}

- (void)setCss_liquidBoost:(NSNumber *)css_liquidBoost {
    _liquidBoost = css_liquidBoost.floatValue;
    if (_sessionHandle != 0) {
        holm_runtime_session_set_liquid(_sessionHandle, _liquidBoost, _liquidBend);
    }
    [_glesHost holm_setLiquidBoost:_liquidBoost bend:_liquidBend];
}

- (void)setCss_liquidBend:(NSNumber *)css_liquidBend {
    _liquidBend = css_liquidBend.floatValue;
    if (_sessionHandle != 0) {
        holm_runtime_session_set_liquid(_sessionHandle, _liquidBoost, _liquidBend);
    }
    [_glesHost holm_setLiquidBoost:_liquidBoost bend:_liquidBend];
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

- (BOOL)holm_isRuntime {
    return [_executor isEqualToString:kHolmExecutorRuntime];
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
            [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorRuntime];
            return;
        }
        CFAbsoluteTime t0 = CFAbsoluteTimeGetCurrent();
        BOOL ok = holm_runtime_session_set_custom_fs(_sessionHandle, custom.UTF8String);
        float compileMs = (float)((CFAbsoluteTimeGetCurrent() - t0) * 1000.0);
        if (!ok) {
            char codeBuf[64] = {0};
            char msgBuf[512] = {0};
            holm_runtime_session_last_error_code(_sessionHandle, codeBuf, sizeof(codeBuf));
            holm_runtime_session_last_error_message(_sessionHandle, msgBuf, sizeof(msgBuf));
            NSString *code = codeBuf[0] ? [NSString stringWithUTF8String:codeBuf] : @"SHADER_CUSTOM";
            NSString *msg = msgBuf[0] ? [NSString stringWithUTF8String:msgBuf] : @"Runtime setCustomFs failed";
            [self holm_fireError:code message:msg];
            [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:compileMs executor:kHolmExecutorRuntime];
        } else {
            [self holm_fireShaderCompiled:YES wrapMs:0.f compileMs:compileMs executor:kHolmExecutorRuntime];
        }
    } else {
        holm_runtime_session_set_custom_fs(_sessionHandle, "");
        if (_scene.length) {
            holm_runtime_session_set_scene(_sessionHandle, _scene.UTF8String);
        }
        // 空片元 ClearTri：ok=false，对齐 Lite 拒绝语义（审查 P2）
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorRuntime];
    }
}

- (void)holm_applyLiteShaders {
    if (!_glesHost) {
        return;
    }
    NSString *custom = [_fragmentShader stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (custom.length == 0) {
        [self holm_fireError:@"FRAGMENT_REQUIRED"
                     message:@"fragmentShader required (official scenes removed on iOS)"];
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorLite];
        return;
    }
    if ([self holm_looksLikeWgslFragment:custom]) {
        // 热切 Runtime 途中的 WGSL：忽略，等 executor=runtime
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorLite];
        return;
    }
    [_glesHost holm_setFragmentBody:custom];
}

- (void)holm_applyExecutorMode {
    if (!self.window) {
        return;
    }
    if ([self holm_isRuntime]) {
        [self holm_teardownLite];
        [self holm_startRuntime];
    } else {
        [self holm_teardownRuntime];
        [self holm_startLite];
    }
}

- (void)holm_teardownAll {
    [self holm_teardownRuntime];
    [self holm_teardownLite];
}

#pragma mark - Lite

- (void)holm_startLite {
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
                                          executor:kHolmExecutorLite];
            }
        };
    }
    if (![_glesHost holm_setupGL]) {
        return;
    }
    [_glesHost holm_setLiquidBoost:_liquidBoost bend:_liquidBend];

    NSString *custom = [_fragmentShader stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
    if (custom.length == 0) {
        self.backgroundColor = [UIColor colorWithRed:0.02 green:0.08 blue:0.28 alpha:1];
        [self holm_fireError:@"FRAGMENT_REQUIRED"
                     message:@"fragmentShader required (official scenes removed on iOS)"];
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorLite];
        [self holm_startDisplayLink];
        return;
    }
    if ([self holm_looksLikeWgslFragment:custom]) {
        [self holm_fireError:@"SHADER_CUSTOM"
                     message:@"Lite fragmentShader must be GLSL Shadertoy body (WGSL ignored)"];
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorLite];
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
    if (!_metalHost) {
        _metalHost = [[KRHolmMetalHostView alloc] initWithFrame:self.bounds];
        _metalHost.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [self addSubview:_metalHost];
    }
    [_metalHost holm_setRenderScale:_renderScale];
    [_metalHost holm_updateDrawableSize];
    CGSize px = _metalHost.metalLayer.drawableSize;
    uint32_t w = (uint32_t)MAX(1.0, px.width);
    uint32_t h = (uint32_t)MAX(1.0, px.height);

    if (holm_runtime_has_wgpu() == 0) {
        [self holm_teardownRuntime];
        [self holm_fireBlocked:@"holm_runtime has_wgpu=0. No silent GLES stand-in."];
        return;
    }

    uint64_t handle = holm_runtime_session_create_metal((__bridge void *)_metalHost.metalLayer, w, h);
    if (handle == 0) {
        [self holm_teardownRuntime];
        [self holm_fireBlocked:@"holm_runtime_session_create_metal failed. No silent GLES stand-in."];
        return;
    }
    _sessionHandle = handle;
    _lastSessionW = w;
    _lastSessionH = h;
    holm_runtime_session_set_liquid(_sessionHandle, _liquidBoost, _liquidBend);
    holm_runtime_session_set_playing(_sessionHandle, _playing ? 1u : 0u);
    if (_fragmentShader.length > 0 &&
        ![self holm_looksLikeWgslFragment:_fragmentShader] &&
        [self holm_isRuntime]) {
        [self holm_teardownRuntime];
        [self holm_fireError:@"SHADER_CUSTOM"
                     message:@"Runtime fragmentShader must be WGSL (non-WGSL custom ignored)"];
        [self holm_fireShaderCompiled:NO wrapMs:0.f compileMs:0.f executor:kHolmExecutorRuntime];
        return;
    }
    [self holm_applyRuntimeShaders];

    char backendBuf[64] = {0};
    holm_runtime_session_backend_id(_sessionHandle, backendBuf, sizeof(backendBuf));
    _backendId = [NSString stringWithUTF8String:backendBuf] ?: @"wgpu-m0";

    float dt = 1.f / 60.f;
    if (!holm_runtime_session_tick(_sessionHandle, dt)) {
        [self holm_teardownRuntime];
        [self holm_fireBlocked:@"holm_runtime tick failed after create. No silent GLES stand-in."];
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
        holm_runtime_session_destroy(_sessionHandle);
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
    holm_runtime_session_resize(_sessionHandle, w, h);
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
    if ([self holm_isRuntime]) {
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

    if ([self holm_isRuntime]) {
        if (!holm_runtime_session_tick(_sessionHandle, dt)) {
            if (!_blockedNotified) {
                _blockedNotified = YES;
                char codeBuf[64] = {0};
                char msgBuf[512] = {0};
                holm_runtime_session_last_error_code(_sessionHandle, codeBuf, sizeof(codeBuf));
                holm_runtime_session_last_error_message(_sessionHandle, msgBuf, sizeof(msgBuf));
                NSString *code = codeBuf[0] ? [NSString stringWithUTF8String:codeBuf] : @"TICK";
                NSString *msg = msgBuf[0]
                    ? [NSString stringWithUTF8String:msgBuf]
                    : @"holm_runtime tick failed. No silent GLES stand-in.";
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
        backend = [self holm_isRuntime] ? @"wgpu-m0" : kHolmBackendGlesIos;
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
