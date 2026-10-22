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
        data class Error(val message: String?) : SearchState
    }

    data class UiState(
        val location: AppLocation? = null,
        val searchState: SearchState = SearchState.Idle,
        val radiusKm: Int = 3,
    )

    private val searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    private val radiusKm = MutableStateFlow(3)

    val uiState: StateFlow<UiState> = kotlinx.coroutines.flow.combine(
        container.settingsRepository.settings,
        searchState,
        radiusKm,
    ) { settings, search, radius ->
        UiState(location = settings.location, searchState = search, radiusKm = radius)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    val radiusOptions = listOf(1, 2, 3, 5, 10)

    fun setRadius(km: Int) {
        radiusKm.value = km
    }

    fun search() {
        val location = uiState.value.location ?: return
        if (searchState.value is SearchState.Loading) return
        searchState.value = SearchState.Loading
        viewModelScope.launch {
            val result = container.overpassClient.findMosques(
                location.latitude, location.longitude, radiusKm.value * 1000,
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
                is OverpassClient.Result.Error -> SearchState.Error(result.message)
            }
        }
    }

    companion object {
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
