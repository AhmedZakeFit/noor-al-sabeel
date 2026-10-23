package io.github.ahmedzakefit.noor.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import io.github.ahmedzakefit.noor.AppContainer
import io.github.ahmedzakefit.noor.data.UpdateManager
import io.github.ahmedzakefit.noor.data.UpdateParser.ReleaseInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Shared (activity-scoped) state for the in-app update flow:
 * a quiet banner on the home screen after the automatic check on app open,
 * and the details dialog used from the banner and from settings' manual
 * "check for updates" button.
 */
class UpdateViewModel(private val container: AppContainer) : ViewModel() {

    sealed interface DownloadState {
        data class Progress(val bytes: Long, val total: Long?) : DownloadState
        data class Verified(val file: File) : DownloadState
        data object Failed : DownloadState
    }

    /** One-line outcome for the manual check button. */
    enum class ManualMessage { NONE, UP_TO_DATE, FAILED }

    data class UiState(
        /** Non-null while an update is available and not postponed -> banner. */
        val banner: ReleaseInfo? = null,
        /** Non-null while the details dialog is open. */
        val dialog: ReleaseInfo? = null,
        val checking: Boolean = false,
        val manualMessage: ManualMessage = ManualMessage.NONE,
        val download: DownloadState? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        // Automatic check on app open: silent on every failure — prayer and
        // adhan functions are never disturbed by update connectivity.
        autoCheck()
    }

    fun autoCheck() {
        viewModelScope.launch {
            val result = runCatching { container.updateManager.check(manual = false) }
                .getOrNull() ?: return@launch
            if (result is UpdateManager.CheckResult.Available) {
                _state.value = _state.value.copy(banner = result.info)
            }
        }
    }

    /** Manual re-check from settings; always reports a visible outcome. */
    fun manualCheck() {
        _state.value = _state.value.copy(checking = true, manualMessage = ManualMessage.NONE)
        viewModelScope.launch {
            val result = runCatching { container.updateManager.check(manual = true) }
                .getOrNull() ?: UpdateManager.CheckResult.Failed
            _state.value = when (result) {
                is UpdateManager.CheckResult.Available -> _state.value.copy(
                    checking = false,
                    banner = result.info,
                    dialog = result.info,
                    download = null,
                )
                UpdateManager.CheckResult.UpToDate -> _state.value.copy(
                    checking = false,
                    manualMessage = ManualMessage.UP_TO_DATE,
                )
                UpdateManager.CheckResult.Failed -> _state.value.copy(
                    checking = false,
                    manualMessage = ManualMessage.FAILED,
                )
            }
        }
    }

    fun openDialog(info: ReleaseInfo) {
        _state.value = _state.value.copy(dialog = info)
    }

    fun closeDialog() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = _state.value.copy(dialog = null, download = null)
    }

    /** Postpone exactly this version: auto checks stay quiet until a newer one. */
    fun postpone(info: ReleaseInfo) {
        viewModelScope.launch {
            container.updateManager.postpone(info)
            _state.value = _state.value.copy(banner = null, dialog = null, download = null)
        }
    }

    fun startDownload(info: ReleaseInfo) {
        if (downloadJob?.isActive == true) return
        _state.value = _state.value.copy(
            download = DownloadState.Progress(0, info.apkSizeBytes.takeIf { it > 0 }),
        )
        downloadJob = viewModelScope.launch {
            val result = container.updateManager.downloadApk(info) { read, total ->
                _state.value = _state.value.copy(download = DownloadState.Progress(read, total))
            }
            _state.value = _state.value.copy(
                download = when (result) {
                    is UpdateManager.DownloadResult.Verified -> DownloadState.Verified(result.file)
                    is UpdateManager.DownloadResult.Failed -> DownloadState.Failed
                },
            )
        }
    }

    /** Verified file -> system install prompt (user approval required). */
    fun install(file: File) {
        container.updateManager.installPrompt(file)
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[
                        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                    ] as io.github.ahmedzakefit.noor.NoorApplication
                    UpdateViewModel(app.container)
                }
            }
    }
}
