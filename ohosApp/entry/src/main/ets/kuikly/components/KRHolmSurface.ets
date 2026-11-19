/**
 * KRHolmSurface — ADR 0010/0011 鸿蒙双轨岛（XComponent + libholm_xc）
 * Runtime=wgpu；Lite=同窗 EGL/GLES Shadertoy（backend=gles2-ohos）；禁止 GLES 冒充 wgpu。
 * @date 2026-11-15
 * @author Auto
 */
import { KRAny, KuiklyRenderBaseView, KuiklyRenderCallback } from '@kuikly-open/render';
import { ComponentContent } from '@kit.ArkUI';
import holmXc from 'libholm_xc.so';

const PROP_EXECUTOR = 'executor';
const PROP_CONTENT_ID = 'contentId';
const PROP_CONTENT_KIND = 'contentKind';
const PROP_FRAGMENT_SHADER = 'fragmentShader';
const PROP_MAX_FPS = 'maxFps';
const PROP_RENDER_SCALE = 'renderScale';
const PROP_PLAYING = 'playing';
const PROP_SCENE = 'scene';
const PROP_ON_READY = 'onReady';
const PROP_ON_ERROR = 'onError';
const PROP_ON_PERF = 'onPerf';
const PROP_ON_SHADER_COMPILED = 'onShaderCompiled';

const EXECUTOR_WGPU = 'renderer-wgpu';
const EXECUTOR_GL = 'renderer-gl';
const DEFAULT_MAX_FPS = 60;
const DEFAULT_RENDER_SCALE = 1.0;
const DEEP_BLUE = '#051A47';

function normalizeRenderScale(value: number): number {
  if (!Number.isFinite(value) || value <= 0) {
    return DEFAULT_RENDER_SCALE;
  }
  return value > 1 ? 1 : value;
}

let gHolmXcSeq = 0;

@Observed
export class KRHolmSurface extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'KRHolmSurface';

  xcId: string = '';
  cssExecutor: string = EXECUTOR_GL;
  cssContentId: string = '';
  cssContentKind: string = 'empty';
  cssFragmentShader: string = '';
  cssMaxFps: number = DEFAULT_MAX_FPS;
  cssRenderScale: number = DEFAULT_RENDER_SCALE;
  cssPlaying: boolean = true;
  cssScene: string = '';
  cssOnReady: KuiklyRenderCallback | null = null;
  cssOnError: KuiklyRenderCallback | null = null;
  cssOnPerf: KuiklyRenderCallback | null = null;
  cssOnShaderCompiled: KuiklyRenderCallback | null = null;
  private nativeBound: boolean = false;

  constructor() {
    super();
    gHolmXcSeq += 1;
    this.xcId = `holm_xc_${gHolmXcSeq}`;
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(
      uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createHolmSurfaceView),
      this
    );
  }

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case PROP_EXECUTOR: {
        const next = ((propValue as string) ?? EXECUTOR_GL).toLowerCase();
        this.cssExecutor = next === EXECUTOR_WGPU ? EXECUTOR_WGPU : EXECUTOR_GL;
        this.pushExecutor();
        return true;
      }
      case PROP_CONTENT_ID: {
        this.cssContentId = (propValue as string) ?? '';
        this.pushContentId();
        return true;
      }
      case PROP_CONTENT_KIND: {
        const kind = ((propValue as string) ?? 'empty').toLowerCase();
        this.cssContentKind = kind;
        if (kind === 'model' || kind === 'empty') {
          this.cssFragmentShader = '';
          this.pushFragmentShader();
        }
        this.pushContentKind();
        return true;
      }
      case PROP_FRAGMENT_SHADER: {
        this.cssFragmentShader = (propValue as string) ?? '';
        this.pushFragmentShader();
        return true;
      }
      case PROP_MAX_FPS: {
        const n = Number(propValue);
        this.cssMaxFps = Number.isFinite(n) ? Math.max(0, Math.floor(n)) : DEFAULT_MAX_FPS;
        this.pushMaxFps();
        return true;
      }
      case PROP_RENDER_SCALE: {
        this.cssRenderScale = normalizeRenderScale(Number(propValue));
        this.pushRenderScale();
        return true;
      }
      case PROP_PLAYING: {
        this.cssPlaying = propValue === true || propValue === 1 || propValue === 'true';
        this.pushPlaying();
        return true;
      }
      case PROP_SCENE: {
        this.cssScene = (propValue as string) ?? '';
        this.pushScene();
        return true;
      }
      case PROP_ON_READY: {
        this.cssOnReady = propValue as KuiklyRenderCallback;
        this.pushCallbacks();
        return true;
      }
      case PROP_ON_ERROR: {
        this.cssOnError = propValue as KuiklyRenderCallback;
        this.pushCallbacks();
        return true;
      }
      case PROP_ON_PERF: {
        this.cssOnPerf = propValue as KuiklyRenderCallback;
        this.pushCallbacks();
        return true;
      }
      case PROP_ON_SHADER_COMPILED: {
        this.cssOnShaderCompiled = propValue as KuiklyRenderCallback;
        this.pushCallbacks();
        return true;
      }
      default:
        return super.setProp(propKey, propValue);
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    return null;
  }

  bindNative(): void {
    if (this.nativeBound) {
      return;
    }
    try {
      holmXc.ensureIsland(this.xcId);
      this.pushCallbacks();
      // 先片元再 executor，与 common HolmSurface 一致，避免首包错方言
      this.pushFragmentShader();
      this.pushContentId();
      this.pushContentKind();
      this.pushExecutor();
      this.pushPlaying();
      this.pushMaxFps();
      this.pushRenderScale();
      this.pushScene();
      this.nativeBound = true;
      if (typeof holmXc.hasWgpu === 'function' && holmXc.hasWgpu() === 0 &&
        this.cssExecutor === EXECUTOR_WGPU) {
        this.fireLocalError('BLOCKED', 'kmp_wgpu has_wgpu=0. No silent GLES stand-in.');
      }
    } catch (e) {
      this.fireLocalError('BLOCKED', `libholm_xc unavailable: ${String(e)}`);
    }
  }

  unbindNative(): void {
    if (!this.nativeBound) {
      return;
    }
    try {
      holmXc.destroyIsland(this.xcId);
    } catch (_e) {
      // ignore
    }
    this.nativeBound = false;
  }

  private pushExecutor(): void {
    try {
      holmXc.setExecutor(this.xcId, this.cssExecutor);
    } catch (_e) {
      // so 未就绪时由 bindNative 再推
    }
  }

  private pushContentId(): void {
    try {
      holmXc.setContentId(this.xcId, this.cssContentId);
    } catch (_e) {
      // ignore
    }
  }

  private pushContentKind(): void {
    try {
      holmXc.setContentKind(this.xcId, this.cssContentKind);
    } catch (_e) {
      // ignore
    }
  }

  private pushFragmentShader(): void {
    try {
      holmXc.setFragmentShader(this.xcId, this.cssFragmentShader);
    } catch (_e) {
      // ignore
    }
  }

  private pushPlaying(): void {
    try {
      holmXc.setPlaying(this.xcId, this.cssPlaying);
    } catch (_e) {
      // ignore
    }
  }

  private pushMaxFps(): void {
    try {
      holmXc.setMaxFps(this.xcId, this.cssMaxFps);
    } catch (_e) {
      // ignore
    }
  }

  private pushRenderScale(): void {
    try {
      holmXc.setRenderScale(this.xcId, this.cssRenderScale);
    } catch (_e) {
      // ignore
    }
  }

  private pushScene(): void {
    try {
      // ADR 0015: setScene removed
    } catch (_e) {
      // ignore
    }
  }


  private pushCallbacks(): void {
    try {
      const self = this;
      holmXc.setCallbacks(
        this.xcId,
        (payload: object) => {
          if (self.cssOnReady) {
            self.cssOnReady(payload as KRAny);
          }
        },
        (payload: object) => {
          if (self.cssOnError) {
            self.cssOnError(payload as KRAny);
          }
        },
        (payload: object) => {
          if (self.cssOnPerf) {
            self.cssOnPerf(payload as KRAny);
          }
        },
        (payload: object) => {
          if (self.cssOnShaderCompiled) {
            self.cssOnShaderCompiled(payload as KRAny);
          }
        }
      );
    } catch (_e) {
      // ignore
    }
  }

  private fireLocalError(code: string, message: string): void {
    if (this.cssOnError) {
      this.cssOnError({ code: code, message: message } as KRAny);
    }
  }
}

@Component
export struct KRHolmSurfaceArk {
  @ObjectLink renderView: KRHolmSurface

  aboutToAppear(): void {
    this.renderView.bindNative();
  }

  aboutToDisappear(): void {
    this.renderView.unbindNative();
  }

  build() {
    // Model orbit：配 libraryname 时 ArkTS onTouch 不触发，改走 C-API DispatchTouchEvent
    Stack() {
      XComponent({
        id: this.renderView.xcId,
        type: XComponentType.SURFACE,
        libraryname: 'holm_xc'
      })
        .width('100%')
        .height('100%')
        .onLoad(() => {
          this.renderView.bindNative();
        })
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DEEP_BLUE)
  }
}

@Builder
function createHolmSurfaceView(view: KuiklyRenderBaseView) {
  KRHolmSurfaceArk({ renderView: view as KRHolmSurface })
}
