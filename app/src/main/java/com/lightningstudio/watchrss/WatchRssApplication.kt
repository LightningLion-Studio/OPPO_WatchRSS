package com.lightningstudio.watchrss

import android.app.Application
import com.lightningstudio.watchrss.data.AppContainer
import com.lightningstudio.watchrss.data.DefaultAppContainer

class WatchRssApplication : Application() {
    val container: AppContainer by lazy {
        DefaultAppContainer(this)
    }
}
