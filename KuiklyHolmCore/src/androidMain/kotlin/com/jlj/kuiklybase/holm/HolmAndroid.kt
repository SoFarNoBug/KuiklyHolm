package com.jlj.kuiklybase.holm

import android.app.Application
import android.content.Context

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Android 宿主初始化占位；空壳阶段仅保存 Application Context。
 */
object HolmAndroid {
    @Volatile
    private var _appContext: Context? = null

    fun init(application: Application) {
        _appContext = application.applicationContext
    }

    fun appContextOrNull(): Context? = _appContext
}
