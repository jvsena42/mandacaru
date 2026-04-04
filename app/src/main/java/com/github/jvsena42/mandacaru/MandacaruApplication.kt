package com.github.jvsena42.mandacaru

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.domain.PreferencesDataSourceImpl
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemonImpl
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaRpcImpl
import com.github.jvsena42.mandacaru.presentation.ui.screens.blockchain.BlockchainViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.node.NodeViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.transaction.TransactionViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.settings.SettingsViewModel
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val Context.florestaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "floresta",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "floresta"))
    }
)

class MandacaruApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MandacaruApplication)
            modules(
                presentationModule,
                domainModule
            )
        }
    }
}

val presentationModule = module {
    viewModel { NodeViewModel(florestaRpc = get()) }
    viewModel { SettingsViewModel(florestaRpc = get(), preferencesDataSource = get()) }
    viewModel { TransactionViewModel(florestaRpc = get()) }
    viewModel { BlockchainViewModel(florestaRpc = get()) }
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
            dataStore = androidContext().florestaDataStore
        )
    }
}
