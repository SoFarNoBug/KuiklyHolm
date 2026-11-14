export const hasWgpu: () => number;
export const abiVersion: () => number;
export const ensureIsland: (id: string) => boolean;
export const setExecutor: (id: string, executor: string) => void;
export const setFragmentShader: (id: string, fragmentShader: string) => void;
export const setPlaying: (id: string, playing: boolean) => void;
export const setMaxFps: (id: string, maxFps: number) => void;
export const setScene: (id: string, scene: string) => void;
export const setLiquid: (id: string, boost: number, bend: number) => void;
export const setCallbacks: (
  id: string,
  onReady?: (payload: object) => void,
  onError?: (payload: object) => void,
  onPerf?: (payload: object) => void
) => void;
export const destroyIsland: (id: string) => void;
