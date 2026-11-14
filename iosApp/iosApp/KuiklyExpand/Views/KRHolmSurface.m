#import "KRHolmSurface.h"
#import "KRHolmMetalHostView.h"
#import <OpenKuiklyIOSRender/KRComponentDefine.h>
#import <QuartzCore/QuartzCore.h>
#import "holm_runtime.h"

/**
 * @CreateDate: 2026/11/14
 * @Author: jlj
 * @Description: ADR 0009 — 容器岛；Runtime Metal Session + 主线程外泵；Lite 诚实失败。
 */

static const NSInteger kHolmDefaultMaxFps = 60;
static NSString *const kHolmExecutorRuntime = @"runtime";
static NSString *const kHolmExecutorLite = @"lite";

@class KRHolmSurface;

/** Weak proxy：DisplayLink 强持 proxy，proxy 弱持 Surface，避免 target:self 环。 */
@interface KRHolmDisplayLinkProxy : NSObject
@property (nonatomic, weak, nullable) KRHolmSurface *owner;
- (void)holm_onTick:(CADisplayLink *)link;
@end

@interface KRHolmSurface ()
@property (nonatomic, strong, nullable) KRHolmMetalHostView *metalHost;
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
    [self holm_teardownRuntime];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    if (_metalHost) {
        _metalHost.frame = self.bounds;
        [_metalHost holm_updateDrawableSize];
        [self holm_resizeSessionIfNeeded];
    }
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self holm_applyExecutorMode];
    } else {
        [self holm_teardownRuntime];
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
    // Compose 先写 fragmentShader 再写 executor：切 Lite 时会先到 GLSL，此时 Session 仍在。
    // 对齐 H5：非 WGSL 跳过，等 executor=lite 拆 Session；勿把 GLSL 喂进 set_custom_fs。
    if (_sessionHandle != 0 && [self holm_isRuntime]) {
        [self holm_applyRuntimeShaders];
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
    if (_playing && [self holm_isRuntime] && _sessionHandle != 0) {
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

- (void)setCss_liquidBoost:(NSNumber *)css_liquidBoost {
    _liquidBoost = css_liquidBoost.floatValue;
    if (_sessionHandle != 0) {
        holm_runtime_session_set_liquid(_sessionHandle, _liquidBoost, _liquidBend);
    }
}

- (void)setCss_liquidBend:(NSNumber *)css_liquidBend {
    _liquidBend = css_liquidBend.floatValue;
    if (_sessionHandle != 0) {
        holm_runtime_session_set_liquid(_sessionHandle, _liquidBoost, _liquidBend);
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
            return;
        }
        if (!holm_runtime_session_set_custom_fs(_sessionHandle, custom.UTF8String)) {
            [self holm_fireError:@"SHADER_CUSTOM" message:@"Runtime setCustomFs failed"];
        }
    } else {
        holm_runtime_session_set_custom_fs(_sessionHandle, "");
        if (_scene.length) {
            holm_runtime_session_set_scene(_sessionHandle, _scene.UTF8String);
        }
    }
}

- (void)holm_applyExecutorMode {
    if (!self.window) {
        return;
    }
    if ([self holm_isRuntime]) {
        [self holm_startRuntime];
    } else {
        [self holm_teardownRuntime];
        self.backgroundColor = [UIColor colorWithRed:0.02 green:0.08 blue:0.28 alpha:1];
        [self holm_fireError:@"LITE_PENDING"
                     message:@"iOS Lite fragmentShader pending (ADR 0009: Runtime first)"];
    }
}

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
    [self holm_applyRuntimeShaders];
    if (_fragmentShader.length > 0 &&
        ![self holm_looksLikeWgslFragment:_fragmentShader] &&
        [self holm_isRuntime]) {
        // 进 Runtime 却仍持 GLSL：诚实失败（非切轨途中）
        [self holm_teardownRuntime];
        [self holm_fireError:@"SHADER_CUSTOM"
                     message:@"Runtime fragmentShader must be WGSL (non-WGSL custom ignored)"];
        return;
    }

    char backendBuf[64] = {0};
    holm_runtime_session_backend_id(_sessionHandle, backendBuf, sizeof(backendBuf));
    _backendId = [NSString stringWithUTF8String:backendBuf] ?: @"wgpu-m0";

    // Presentability: create + at least one successful tick
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

- (void)holm_startDisplayLink {
    if (_displayLink || !_playing || _sessionHandle == 0) {
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
    if (_sessionHandle == 0) {
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

    if (!holm_runtime_session_tick(_sessionHandle, dt)) {
        if (!_blockedNotified) {
            _blockedNotified = YES;
            [self holm_fireBlocked:@"holm_runtime tick failed. No silent GLES stand-in."];
        }
        [self holm_stopDisplayLink];
        return;
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
        CGSize px = _metalHost ? _metalHost.metalLayer.drawableSize : CGSizeMake(1, 1);
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
    _css_onReady(@{
        @"backend": _backendId ?: @"wgpu-m0",
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

@end
