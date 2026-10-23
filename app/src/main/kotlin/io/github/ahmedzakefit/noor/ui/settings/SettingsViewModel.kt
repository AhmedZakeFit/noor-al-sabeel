package io.github.ahmedzakefit.noor.ui.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import io.github.ahmedzakefit.noor.AppContainer
import io.github.ahmedzakefit.noor.data.AppSettings
import io.github.ahmedzakefit.noor.data.AppLocation
import io.github.ahmedzakefit.noor.notifications.NotificationChannels
import io.github.ahmedzakefit.noor.notifications.Notifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            container.settingsRepository.update(transform)
            container.alarmScheduler.reschedule()
            val latest = container.settingsRepository.settings.first()
            NotificationChannels.applySettings(container.appContext, latest)
        }
    }

    fun setLocation(location: AppLocation?) {
        update { it.copy(location = location) }
    }

    fun setLanguage(language: io.github.ahmedzakefit.noor.data.LanguageSetting) {
        update { it.copy(language = language) }
        val tags = when (language) {
            io.github.ahmedzakefit.noor.data.LanguageSetting.ARABIC -> "ar"
            io.github.ahmedzakefit.noor.data.LanguageSetting.ENGLISH -> "en"
            io.github.ahmedzakefit.noor.data.LanguageSetting.SYSTEM -> ""
        }
        AppCompatDelegate.setApplicationLocales(
            if (tags.isEmpty()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tags),
        )
    }

    fun sendTestNotification() {
        Notifier.showTestNotification(container.appContext)
    }

    /**
     * Plays the standard adhan through the same foreground service a real
     * prayer-time alert uses, so the user can verify volume and sound before
     * relying on it. Stop from the notification at any time.
     */
    fun testAdhan() {
        io.github.ahmedzakefit.noor.notifications.AdhanService.start(
            container.appContext,
            io.github.ahmedzakefit.noor.core.model.Prayer.DHUHR,
            preview = true,
        )
    }

    fun canScheduleExactAlarms(): Boolean = container.alarmScheduler.canScheduleExact()

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[
                        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                    ] as io.github.ahmedzakefit.noor.NoorApplication
                    SettingsViewModel(app.container)
                }
            }
    }
}
