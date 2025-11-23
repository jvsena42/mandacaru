package com.github.jvsena42.floresta_node

import android.app.Application
import com.github.jvsena42.floresta_node.platform.initAndroidContext

class FlorestaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Android context for platform-specific code
        initAndroidContext(this)
    }
}
