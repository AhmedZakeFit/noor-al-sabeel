package io.github.ahmedzakefit.noor

import android.app.Application
import android.content.Context
import io.github.ahmedzakefit.noor.core.cities.City
import io.github.ahmedzakefit.noor.core.cities.CityCatalog
import io.github.ahmedzakefit.noor.core.calc.PrayerCalculator
import io.github.ahmedzakefit.noor.data.AlarmScheduler
import io.github.ahmedzakefit.noor.data.LocationProvider
import io.github.ahmedzakefit.noor.data.MosqueSearchClient
import io.github.ahmedzakefit.noor.data.NominatimClient
import io.github.ahmedzakefit.noor.data.OverpassClient
import io.github.ahmedzakefit.noor.data.SettingsRepository
import io.github.ahmedzakefit.noor.data.Http
import io.github.ahmedzakefit.noor.data.UpdateManager
import io.github.ahmedzakefit.noor.notifications.NotificationChannels
import java.io.File
import java.io.InputStreamReader

/**
 * Manual dependency container (kept intentionally small: no DI framework,
 * fewer dependencies, easier reproducible builds).
 */
class AppContainer(val appContext: Context) {
    val settingsRepository: SettingsRepository = SettingsRepository(appContext)
    val prayerCalculator: PrayerCalculator = PrayerCalculator()
    val locationProvider: LocationProvider = LocationProvider(appContext)
    val nominatimClient: NominatimClient = NominatimClient()
    val overpassClient: OverpassClient = OverpassClient()
    val mosqueSearchClient: MosqueSearchClient = MosqueSearchClient(overpassClient)
    val updateManager: UpdateManager = UpdateManager(appContext, settingsRepository)
    val alarmScheduler: AlarmScheduler =
        AlarmScheduler(appContext, settingsRepository, prayerCalculator)

    /** Offline city catalog loaded once from the bundled asset. */
    val cityCatalog: List<City> by lazy {
        appContext.assets.open("cities.csv").use { input ->
            CityCatalog.parse(InputStreamReader(input, Charsets.UTF_8))
        }
    }
}

class NoorApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // OpenStreetMap tile usage policy: identify the app; keep cache app-private.
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = Http.USER_AGENT
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        NotificationChannels.ensure(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as NoorApplication).container
