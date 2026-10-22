package io.github.ahmedzakefit.noor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import io.github.ahmedzakefit.noor.AppContainer
import io.github.ahmedzakefit.noor.data.AppSettings
import io.github.ahmedzakefit.noor.data.calculationParametersFor
import io.github.ahmedzakefit.noor.core.calc.PrayerTimes
import io.github.ahmedzakefit.noor.core.model.GeoPosition
import io.github.ahmedzakefit.noor.core.model.Prayer
import io.github.ahmedzakefit.noor.notifications.NotificationChannels
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings(),
        val times: PrayerTimes? = null,
        val now: ZonedDateTime? = null,
        val nextPrayer: Prayer? = null,
        val nextTime: ZonedDateTime? = null,
        val nextIsTomorrow: Boolean = false,
        val remainingMinutes: Long? = null,
    )

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(30_000)
        }
    }

    val uiState: StateFlow<UiState> =
        combine(container.settingsRepository.settings, ticker) { s, _ -> s }
            .map(::build)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        // Keep the alarm chain and channel sounds in sync with the user's choices
        // whenever the app is alive; boot/time receivers cover the rest.
        viewModelScope.launch {
            container.settingsRepository.settings.collect {
                container.alarmScheduler.reschedule()
                NotificationChannels.applySettings(container.appContext, it)
            }
        }
    }

    fun markNotificationPermissionAsked() {
        viewModelScope.launch {
            container.settingsRepository.setNotificationPermissionAsked()
        }
    }

    private fun build(s: AppSettings): UiState {
        val loc = s.location ?: return UiState(settings = s)
        val zone = ZoneId.of(loc.timeZoneId)
        val now = ZonedDateTime.now(zone)
        val today = LocalDate.now(zone)
        val geo = GeoPosition(loc.latitude, loc.longitude, 0.0, loc.timeZoneId)
        val times = container.prayerCalculator.compute(
            today, geo, s.calculationParametersFor(today), zone,
        )
        val tomorrowDate = today.plusDays(1)
        val tomorrowFajr = container.prayerCalculator.compute(
            tomorrowDate, geo, s.calculationParametersFor(tomorrowDate), zone,
        ).fajr

        val next = times.nextPrayerAfter(now)
        val prayer: Prayer
        val time: ZonedDateTime
        val isTomorrow: Boolean
        if (next != null) {
            prayer = next.first
            time = next.second
            isTomorrow = false
        } else {
            prayer = Prayer.FAJR
            time = tomorrowFajr
            isTomorrow = true
        }
        val remaining = Duration.between(now, time).toMinutes()
        return UiState(
            settings = s,
            times = times,
            now = now,
            nextPrayer = prayer,
            nextTime = time,
            nextIsTomorrow = isTomorrow,
            remainingMinutes = remaining,
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[
                        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                    ] as io.github.ahmedzakefit.noor.NoorApplication
                    HomeViewModel(app.container)
                }
            }
    }
}
