package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Provider 注册表；未注册 / 未设 default → 明确失败（ADR 0008）。
 * 约定在宿主主线程 / Kuikly 线程注册与查询（无跨线程锁）。
 */
object HolmProviders {
    private val _byId = linkedMapOf<String, HolmBackendProvider>()
    private val _byExecutor = linkedMapOf<String, HolmBackendProvider>()
    private var _defaultId: String? = null

    fun register(provider: HolmBackendProvider) {
        _byId[provider.id] = provider
        _byExecutor[HolmExecutor.normalize(provider.executorKey)] = provider
    }

    fun setDefault(id: String) {
        require(_byId.containsKey(id)) {
            "HolmProviders.setDefault: provider '$id' not registered"
        }
        _defaultId = id
    }

    fun defaultIdOrNull(): String? = _defaultId

    fun get(id: String): HolmBackendProvider? = _byId[id]

    fun forExecutorOrNull(executor: String?): HolmBackendProvider? {
        val key = HolmExecutor.normalize(executor)
        return _byExecutor[key]
    }

    /**
     * 岛切轨前调用：缺 Provider 或未设 default → 抛 [IllegalStateException]（调用方转 BLOCKED）。
     */
    fun requireForExecutor(executor: String?): HolmBackendProvider {
        val key = HolmExecutor.normalize(executor)
        val provider = forExecutorOrNull(key)
            ?: throw IllegalStateException(missingProviderMessage(key))
        if (_defaultId.isNullOrBlank()) {
            throw IllegalStateException(missingDefaultMessage())
        }
        return provider
    }

    fun missingProviderMessage(executor: String): String {
        val key = HolmExecutor.normalize(executor)
        return "DualTrack BLOCKED: HolmProvider for executor='$key' not registered. " +
            "Depend holm-renderer-gl / holm-renderer-wgpu and call HolmProviders.register (or use KuiklyHolmCore umbrella)."
    }

    fun missingDefaultMessage(): String =
        "DualTrack BLOCKED: HolmProviders.setDefault not called. " +
            "Host must set a default Provider (ADR 0008)."

    fun clearForTests() {
        _byId.clear()
        _byExecutor.clear()
        _defaultId = null
    }
}
