export const hasWgpu: () => number;
export const abiVersion: () => number;
export const ensureIsland: (id: string) => boolean;
export const setExecutor: (id: string, executor: string) => void;
export const setContentKind: (id: string, contentKind: string) => void;
export const setContentId: (id: string, contentId: string) => void;
export const nudgeModelOrbit: (id: string, dx: number, dy: number) => void;
export const setFragmentShader: (id: string, fragmentShader: string) => void;
export const setPlaying: (id: string, playing: boolean) => void;
export const setMaxFps: (id: string, maxFps: number) => void;
export const setRenderScale: (id: string, renderScale: number) => void;
export const setCallbacks: (
  id: string,
  onReady?: (payload: object) => void,
  onError?: (payload: object) => void,
  onPerf?: (payload: object) => void,
  onShaderCompiled?: (payload: object) => void
) => void;
export const destroyIsland: (id: string) => void;
