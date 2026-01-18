package com.lightningstudio.watchrss

import android.app.Application
import android.content.pm.ApplicationInfo
import com.lightningstudio.watchrss.data.AppContainer
import com.lightningstudio.watchrss.data.DefaultAppContainer
import com.lightningstudio.watchrss.debug.DebugLogBuffer

class WatchRssApplication : Application() {
    val container: AppContainer by lazy {
        DefaultAppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        DebugLogBuffer.setEnabled(debuggable)
    }
}
