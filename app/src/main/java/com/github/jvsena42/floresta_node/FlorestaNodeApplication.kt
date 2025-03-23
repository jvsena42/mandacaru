package com.github.jvsena42.floresta_node

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.Intent
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

class FlorestaNodeApplication: Application() {
    override fun onCreate() {
        super.onCreate()
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
    single<PreferencesDataSource> { PreferencesDataSourceImpl(
        sharedPreferences = androidContext().getSharedPreferences("floresta", MODE_PRIVATE)
    ) }
}