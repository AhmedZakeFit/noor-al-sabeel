package io.github.ahmedzakefit.noor.ui.mosques

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import io.github.ahmedzakefit.noor.AppContainer
import io.github.ahmedzakefit.noor.core.qibla.QiblaMath
import io.github.ahmedzakefit.noor.data.AppLocation
import io.github.ahmedzakefit.noor.data.OverpassClient
import io.github.ahmedzakefit.noor.ui.common.Formatters
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

class MosquesViewModel(private val container: AppContainer) : ViewModel() {

    data class MosqueItem(
        val mosque: OverpassClient.Mosque,
        val distanceKm: Double,
        /** Initial bearing from the user to the mosque, degrees from north. */
        val bearing: Double,
    )

    /**
     * The geographic point a search actually runs around. [isCurrentLocation]
     * is true only for a live device fix; when the center mirrors the saved
     * app location, [saved] carries it along so the UI can show its label.
     */
    data class SearchCenter(
        val latitude: Double,
        val longitude: Double,
        val isCurrentLocation: Boolean,
        val saved: AppLocation? = null,
    )

    sealed interface SearchState {
        data object Idle : SearchState
        /** Acquiring a fresh device-location fix (phase before the network search). */
        data object Locating : SearchState
        data object Loading : SearchState
        data class Ready(val items: List<MosqueItem>) : SearchState
        data object Empty : SearchState
        /** [reason] drives the localized message; the UI offers a retry button. */
        data class Error(val reason: OverpassClient.Reason) : SearchState
        /** Permission is granted but no location fix could be obtained. */
        data object LocationFailed : SearchState
        /**
         * The location permission is missing. The UI shows the in-context
         * grant path (rationale, system dialog, app-settings link) and an
         * *explicit* saved-location choice — never a silent fallback.
         */
        data object PermissionRequired : SearchState
    }

    data class UiState(
        val location: AppLocation? = null,
        /** What the last (or, via fallback, next) search revolves around. */
        val activeCenter: SearchCenter? = null,
        val searchState: SearchState = SearchState.Idle,
        val radiusKm: Int = 3,
        val hasLocationPermission: Boolean = false,
    )

    private val searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    private val radiusKm = MutableStateFlow(3)
    private val activeCenter = MutableStateFlow<SearchCenter?>(null)
    private val hasLocationPermission = MutableStateFlow(false)
    private var searchJob: Job? = null

    val uiState: StateFlow<UiState> = combine(
        container.settingsRepository.settings,
        searchState,
        radiusKm,
        activeCenter,
        hasLocationPermission,
    ) { settings, search, radius, center, permission ->
        UiState(
            location = settings.location,
            activeCenter = center,
            searchState = search,
            radiusKm = radius,
            hasLocationPermission = permission,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        refreshLocationPermission()
    }

    /** Re-reads the location-permission grant; call on resume and after any permission dialog. */
    fun refreshLocationPermission() {
        hasLocationPermission.value = container.locationProvider.hasPermission()
    }

    val radiusOptions: List<Int> = RADIUS_OPTIONS_KM

    fun setRadius(km: Int) {
        radiusKm.value = km
    }

    /**
     * Entry point for the search button (and the retry buttons): search
     * around the device's *current* location — a fresh fix per search, so
     * travelling to another area and searching again returns that area's
     * mosques. When the permission is missing this does NOT silently fall
     * back to the saved location (the v1.0.5 field report); it surfaces
     * [SearchState.PermissionRequired] so the UI can run the in-context
     * grant flow, and the saved location stays reachable only as the
     * explicit on-screen choice ([searchAroundSaved]).
     *
     * @param languageTag current UI language, so the independent Nominatim
     * source can localize mosque names where OSM has translations.
     */
    fun search(languageTag: String) {
        when (resolveSearchPlan(container.locationProvider.hasPermission())) {
            SearchPlan.LOCATE_AND_SEARCH -> locateAndSearch(languageTag)
            SearchPlan.PERMISSION_REQUIRED -> searchState.value = SearchState.PermissionRequired
        }
    }

    /** Grant status read at the moment of use — the UI asks for the permission first when missing. */
    fun locationPermissionGranted(): Boolean = container.locationProvider.hasPermission()

    /**
     * The system permission dialog came back denied: keep the guidance on
     * screen (grant path + app-settings link), but never destroy results
     * the user is already looking at.
     */
    fun markPermissionDenied() {
        val s = searchState.value
        if (s is SearchState.Idle || s is SearchState.PermissionRequired) {
            searchState.value = SearchState.PermissionRequired
        }
    }

    /**
     * Re-reads the grant after the screen resumes (the user may just have
     * granted access in the system dialog or in the app settings). When a
     * search was waiting on exactly that grant, it starts now — the user's
     * pending intent is honored without another tap.
     */
    fun onPermissionPossiblyChanged(languageTag: String) {
        refreshLocationPermission()
        if (hasLocationPermission.value && searchState.value is SearchState.PermissionRequired) {
            search(languageTag)
        }
    }

    /**
     * Always searches around the saved app location — the explicit fallback
     * offered when a GPS fix cannot be obtained.
     */
    fun searchAroundSaved(languageTag: String) {
        val saved = uiState.value.location ?: return
        if (searchState.value is SearchState.Locating || searchState.value is SearchState.Loading) return
        val center = SearchCenter(
            saved.latitude, saved.longitude, isCurrentLocation = false, saved = saved,
        )
        activeCenter.value = center
        runSearch(center, languageTag)
    }

    private fun locateAndSearch(languageTag: String) {
        if (searchState.value is SearchState.Locating || searchState.value is SearchState.Loading) return
        searchState.value = SearchState.Locating
        searchJob = viewModelScope.launch {
            try {
                val fix = container.locationProvider.getCurrentFix(LOCATE_TIMEOUT_MS)
                if (fix == null) {
                    searchState.value = SearchState.LocationFailed
                    return@launch
                }
                val center = SearchCenter(fix.latitude, fix.longitude, isCurrentLocation = true)
                activeCenter.value = center
                performSearch(center, languageTag)
            } finally {
                // Covers cancellation too: leaving the screen aborts the fix
                // or the request and must never strand the UI on a spinner.
                if (searchState.value is SearchState.Locating || searchState.value is SearchState.Loading) {
                    searchState.value = SearchState.Idle
                }
            }
        }
    }

    private fun runSearch(center: SearchCenter, languageTag: String) {
        searchState.value = SearchState.Loading
        searchJob = viewModelScope.launch {
            try {
                performSearch(center, languageTag)
            } finally {
                if (searchState.value is SearchState.Loading) searchState.value = SearchState.Idle
            }
        }
    }

    private suspend fun performSearch(center: SearchCenter, languageTag: String) {
        searchState.value = SearchState.Loading
        val result = container.mosqueSearchClient.findNearbyMosques(
            center.latitude, center.longitude, radiusKm.value * 1000, languageTag,
        )
        searchState.value = when (result) {
            is OverpassClient.Result.Ok -> {
                val items = result.mosques.map { m ->
                    MosqueItem(
                        mosque = m,
                        distanceKm = QiblaMath.haversineKm(
                            center.latitude, center.longitude, m.latitude, m.longitude,
                        ),
                        bearing = QiblaMath.bearingBetween(
                            center.latitude, center.longitude, m.latitude, m.longitude,
                        ),
                    )
                }.sortedBy { it.distanceKm }
                if (items.isEmpty()) SearchState.Empty else SearchState.Ready(items)
            }
            is OverpassClient.Result.Error -> SearchState.Error(result.reason)
        }
    }

    /**
     * Aborts an in-flight fix or search (socket included) and resets the
     * transient state. Called when the user leaves the mosques screen.
     */
    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        val s = searchState.value
        if (s is SearchState.Locating || s is SearchState.Loading) searchState.value = SearchState.Idle
    }

    /** What tapping the search button does, given the current inputs. */
    enum class SearchPlan { LOCATE_AND_SEARCH, PERMISSION_REQUIRED }

    companion object {
        /** Search-radius choices, a clean 1..10 km ladder. */
        val RADIUS_OPTIONS_KM: List<Int> = (1..10).toList()

        /** Budget for obtaining a device-location fix before reporting failure. */
        internal const val LOCATE_TIMEOUT_MS = 12_000L

        /** Web-Mercator ground resolution at the equator for 256 px tiles, meters/px at zoom 0. */
        private const val EQUATOR_M_PER_PX_AT_Z0 = 156_543.03392

        /** MAPNIK's usable zoom range; also bounds the pinch gesture. */
        internal const val MIN_ZOOM = 3.0
        internal const val MAX_ZOOM = 19.0

        /**
         * Pure decision for the search button (unit-tested): a live device
         * fix whenever the permission allows it; otherwise the UI must run
         * the in-context permission request — the saved location is NEVER
         * searched silently (2026-10-24 field-report fix).
         */
        internal fun resolveSearchPlan(hasPermission: Boolean): SearchPlan =
            if (hasPermission) SearchPlan.LOCATE_AND_SEARCH else SearchPlan.PERMISSION_REQUIRED

        /**
         * Center the map (and the pre-search label) on the saved app
         * location until an actual search has run. Pure for tests.
         */
        internal fun fallbackCenter(saved: AppLocation?): SearchCenter? =
            saved?.let { SearchCenter(it.latitude, it.longitude, isCurrentLocation = false, saved = it) }

        /**
         * Zoom level that fits [radiusMeters] (plus a small margin) across
         * the map's smaller dimension. Pure for tests: monotonic in the
         * radius and clamped to the tile source's range.
         */
        internal fun zoomForRadius(radiusMeters: Int, latitude: Double, viewportMinPx: Float): Double {
            val px = max(viewportMinPx, 1f)
            val cosLat = max(cos(Math.toRadians(latitude)), 0.01)
            val affordableMetersPerPx = (2.3 * radiusMeters) / px
            val zoom = log2(EQUATOR_M_PER_PX_AT_Z0 * cosLat / affordableMetersPerPx)
            return zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        }

        /**
         * Map height as a fraction of the screen height, floored/ceilinged so
         * the map stays meaningful on small phones and never starves the
         * results list. Pure for tests.
         */
        internal fun mapHeightDp(screenHeightDp: Int): Int =
            (screenHeightDp * MAP_HEIGHT_FRACTION).roundToInt()
                .coerceIn(MAP_HEIGHT_MIN_DP, MAP_HEIGHT_MAX_DP)

        private const val MAP_HEIGHT_FRACTION = 0.32f
        internal const val MAP_HEIGHT_MIN_DP = 150
        internal const val MAP_HEIGHT_MAX_DP = 260

        /**
         * Builds one radius-chip label from the localized distance pattern
         * (e.g. "%1$s km" / "%1$s كم") so every chip shows its own number
         * followed by the unit. Replaces the old substring hack that stripped
         * the number and left every chip showing only "km"/"كم".
         */
        fun radiusChipLabel(km: Int, arabicDigits: Boolean, localizedPattern: String): String =
            localizedPattern.format(Formatters.digits(km.toString(), arabicDigits))

        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[
                        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                    ] as io.github.ahmedzakefit.noor.NoorApplication
                    MosquesViewModel(app.container)
                }
            }
    }
}
