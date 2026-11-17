package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Holm 后端 Provider SPI（ADR 0008）；宿主须显式 register，禁止仅靠 classpath 默选。
 */
interface HolmBackendProvider {
    /** 注册键，如 `gl` / `wgpu`。 */
    val id: String

    /**
     * 对应 Attr `executor`：`renderer-gl` 或 `renderer-wgpu`。
     * backendId 前缀约定：`gles2` / `webgl` / `wgpu-*`。
     */
    val executorKey: String
}
