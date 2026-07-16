package com.github.jvsena42.mandacaru

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.github.jvsena42.mandacaru.data.AppUpdateRepository
import com.github.jvsena42.mandacaru.data.FlorestaRpc
import com.github.jvsena42.mandacaru.data.GeoIpDatabaseRepository
import com.github.jvsena42.mandacaru.data.PreferencesDataSource
import com.github.jvsena42.mandacaru.data.PreferencesDataSourceImpl
import com.github.jvsena42.mandacaru.data.floresta.FlorestaDaemonImpl
import com.github.jvsena42.mandacaru.data.floresta.FlorestaRpcImpl
import com.github.jvsena42.mandacaru.data.geoip.GeoIpDatabase
import com.github.jvsena42.mandacaru.data.geoip.GeoIpDatabaseRepositoryImpl
import com.github.jvsena42.mandacaru.data.geoip.MmdbPeerCountryLookup
import com.github.jvsena42.mandacaru.data.network.NetworkPolicy
import com.github.jvsena42.mandacaru.data.network.NetworkPolicyManager
import com.github.jvsena42.mandacaru.data.update.AppUpdateRepositoryImpl
import com.github.jvsena42.mandacaru.domain.floresta.FlorestaDaemon
import com.github.jvsena42.mandacaru.domain.geoip.PeerCountryLookup
import com.github.jvsena42.mandacaru.domain.floresta.UtreexoBridgeAutoConnect
import com.github.jvsena42.mandacaru.domain.floresta.UtreexoSnapshotService
import com.github.jvsena42.mandacaru.domain.scan.BdkTransactionDecoder
import com.github.jvsena42.mandacaru.domain.scan.DefaultDescriptorQrScanner
import com.github.jvsena42.mandacaru.domain.scan.DefaultQrTransactionScanner
import com.github.jvsena42.mandacaru.domain.scan.DescriptorQrScanner
import com.github.jvsena42.mandacaru.domain.scan.QrTransactionScanner
import com.github.jvsena42.mandacaru.domain.scan.TransactionDecoder
import com.github.jvsena42.mandacaru.presentation.ui.screens.blockchain.BlockchainViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.logs.DeveloperLogsViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.main.MainViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.node.NodeViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.transaction.TransactionViewModel
import com.github.jvsena42.mandacaru.presentation.ui.screens.settings.SettingsViewModel
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.android.inject
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

private const val GEOIP_DATABASE_FILE = "dbip-country.mmdb"

val Context.florestaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "floresta",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "floresta"))
    }
)

class MandacaruApplication : Application() {
    private val networkPolicyManager: NetworkPolicyManager by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MandacaruApplication)
            modules(
                presentationModule,
                dataModule
            )
        }
        // Bind the process network before the service starts the daemon, so the
        // very first peer socket already respects the WiFi-only policy.
        networkPolicyManager.apply()
    }
}

val presentationModule = module {
    viewModel {
        NodeViewModel(
            florestaRpc = get(),
            snapshotService = get(),
            florestaDaemon = get(),
            preferencesDataSource = get(),
            networkPolicyManager = get(),
            peerCountryLookup = get(),
        )
    }
    viewModel {
        SettingsViewModel(
            florestaRpc = get(),
            preferencesDataSource = get(),
            appUpdateRepository = get(),
            geoIpDatabaseRepository = get(),
            descriptorScanner = get(),
            context = androidContext(),
        )
    }
    viewModel {
        MainViewModel(
            appUpdateRepository = get(),
            geoIpDatabaseRepository = get(),
        )
    }
    viewModel { DeveloperLogsViewModel(context = androidContext()) }
    viewModel {
        TransactionViewModel(
            florestaRpc = get(),
            qrScanner = get(),
            transactionDecoder = get(),
        )
    }
    viewModel { BlockchainViewModel(florestaRpc = get()) }
}

val dataModule = module {
    single<FlorestaDaemon> {
        FlorestaDaemonImpl(
            datadir = androidContext().filesDir.toString(),
            preferencesDataSource = get()
        )
    }
    single<FlorestaRpc> { FlorestaRpcImpl(gson = Gson(), preferencesDataSource = get()) }
    single<AppUpdateRepository> {
        AppUpdateRepositoryImpl(gson = Gson(), preferencesDataSource = get())
    }
    single { GeoIpDatabase(databaseFile = File(androidContext().filesDir, GEOIP_DATABASE_FILE)) }
    single<PeerCountryLookup> {
        MmdbPeerCountryLookup(database = get(), preferencesDataSource = get())
    }
    single<GeoIpDatabaseRepository> {
        GeoIpDatabaseRepositoryImpl(
            database = get(),
            preferencesDataSource = get(),
            networkPolicy = get(),
            cacheDir = androidContext().cacheDir,
        )
    }
    single<PreferencesDataSource> {
        PreferencesDataSourceImpl(
            dataStore = androidContext().florestaDataStore
        )
    }
    single {
        NetworkPolicyManager(
            context = androidContext(),
            preferencesDataSource = get()
        )
    } bind NetworkPolicy::class
    single { UtreexoBridgeAutoConnect(florestaRpc = get(), preferencesDataSource = get()) }
    single { UtreexoSnapshotService(daemon = get()) }
    factory<QrTransactionScanner> { DefaultQrTransactionScanner() }
    factory<DescriptorQrScanner> { DefaultDescriptorQrScanner() }
    single<TransactionDecoder> { BdkTransactionDecoder() }
}
