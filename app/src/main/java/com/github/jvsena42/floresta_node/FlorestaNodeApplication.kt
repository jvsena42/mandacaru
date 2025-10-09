package com.github.jvsena42.floresta_node

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.github.jvsena42.floresta_node.data.FlorestaRpc
import com.github.jvsena42.floresta_node.data.PreferencesDataSource
import com.github.jvsena42.floresta_node.domain.PreferencesDataSourceImpl
import com.github.jvsena42.floresta_node.domain.floresta.FlorestaDaemon
import com.github.jvsena42.floresta_node.domain.floresta.FlorestaDaemonImpl
import com.github.jvsena42.floresta_node.domain.floresta.FlorestaRpcImpl
import com.github.jvsena42.floresta_node.domain.floresta.FlorestaService
import com.github.jvsena42.floresta_node.presentation.ui.screens.node.NodeViewModel
import com.github.jvsena42.floresta_node.presentation.ui.screens.search.SearchViewModel
import com.github.jvsena42.floresta_node.presentation.ui.screens.settings.SettingsViewModel
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class FlorestaNodeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        currentActivity = CurrentActivity().also { registerActivityLifecycleCallbacks(it) }
        startKoin {
            androidLogger()
            androidContext(this@FlorestaNodeApplication)
            modules(
                presentationModule,
                domainModule
            )
        }
        try {
            startForegroundService(Intent(this, FlorestaService::class.java))
        } catch (e: Exception) {
            Log.e("FlorestaApplication", "onCreate: ", e)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        internal var currentActivity: CurrentActivity? = null
    }
}

val presentationModule = module {
    viewModel { NodeViewModel(florestaRpc = get()) }
    viewModel { SettingsViewModel(florestaRpc = get(), preferencesDataSource = get()) }
    viewModel { SearchViewModel(florestaRpc = get()) }
}

val domainModule = module {
    single<FlorestaDaemon> {
        FlorestaDaemonImpl(
            datadir = androidContext().filesDir.toString(),
            preferencesDataSource = get()
        )
    }
    single<FlorestaRpc> { FlorestaRpcImpl(gson = Gson(), preferencesDataSource = get()) }
    single<PreferencesDataSource> {
        PreferencesDataSourceImpl(
            sharedPreferences = androidContext().getSharedPreferences("floresta", MODE_PRIVATE)
        )
    }
}

class CurrentActivity : ActivityLifecycleCallbacks {
    var value: Activity? = null
        private set

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        this.value = activity
    }

    override fun onActivityStarted(activity: Activity) {
        this.value = activity
    }

    override fun onActivityResumed(activity: Activity) {
        this.value = activity
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}