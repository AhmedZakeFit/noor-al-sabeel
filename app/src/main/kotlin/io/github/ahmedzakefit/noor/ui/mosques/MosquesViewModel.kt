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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MosquesViewModel(private val container: AppContainer) : ViewModel() {

    data class MosqueItem(
        val mosque: OverpassClient.Mosque,
        val distanceKm: Double,
        /** Initial bearing from the user to the mosque, degrees from north. */
        val bearing: Double,
    )

    sealed interface SearchState {
        data object Idle : SearchState
        data object Loading : SearchState
        data class Ready(val items: List<MosqueItem>) : SearchState
        data object Empty : SearchState
        /** [reason] drives the localized message; the UI offers a retry button. */
        data class Error(val reason: OverpassClient.Reason) : SearchState
    }

    data class UiState(
        val location: AppLocation? = null,
        val searchState: SearchState = SearchState.Idle,
        val radiusKm: Int = 3,
    )

    private val searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    private val radiusKm = MutableStateFlow(3)
    private var searchJob: Job? = null

    val uiState: StateFlow<UiState> = kotlinx.coroutines.flow.combine(
        container.settingsRepository.settings,
        searchState,
        radiusKm,
    ) { settings, search, radius ->
        UiState(location = settings.location, searchState = search, radiusKm = radius)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    val radiusOptions: List<Int> = RADIUS_OPTIONS_KM

    fun setRadius(km: Int) {
        radiusKm.value = km
    }

    /**
     * Same entry point for a fresh search and for the retry button.
     * @param languageTag current UI language, so the independent Nominatim
     * source can localize mosque names where OSM has translations.
     */
    fun search(languageTag: String) {
        val location = uiState.value.location ?: return
        if (searchState.value is SearchState.Loading) return
        searchState.value = SearchState.Loading
        searchJob = viewModelScope.launch {
            try {
                val result = container.mosqueSearchClient.findNearbyMosques(
                    location.latitude, location.longitude, radiusKm.value * 1000, languageTag,
                )
                searchState.value = when (result) {
                    is OverpassClient.Result.Ok -> {
                        val items = result.mosques.map { m ->
                            MosqueItem(
                                mosque = m,
                                distanceKm = QiblaMath.haversineKm(
                                    location.latitude, location.longitude, m.latitude, m.longitude,
                                ),
                                bearing = QiblaMath.bearingBetween(
                                    location.latitude, location.longitude, m.latitude, m.longitude,
                                ),
                            )
                        }.sortedBy { it.distanceKm }
                        if (items.isEmpty()) SearchState.Empty else SearchState.Ready(items)
                    }
                    is OverpassClient.Result.Error -> SearchState.Error(result.reason)
                }
            } finally {
                // Covers cancellation too: leaving the screen aborts the request
                // and must never strand the UI on a spinner with no way out.
                if (searchState.value is SearchState.Loading) searchState.value = SearchState.Idle
            }
        }
    }

    /**
     * Aborts an in-flight search (socket included) and resets the loading
     * state. Called when the user leaves the mosques screen.
     */
    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        if (searchState.value is SearchState.Loading) searchState.value = SearchState.Idle
    }

    companion object {
        /** Search-radius choices, a clean 1..10 km ladder. */
        val RADIUS_OPTIONS_KM: List<Int> = (1..10).toList()

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
