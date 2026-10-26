package io.github.ahmedzakefit.noor.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ahmedzakefit.noor.R

/**
 * Quiet home-screen banner after the automatic update check: version number,
 * one line of context, and explicit user actions. Postponing stores the
 * version — the banner does not come back for it on later checks.
 */
@Composable
fun UpdateBanner(vm: UpdateViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val info = state.banner ?: return
    Card(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.update_available_title, info.versionName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    stringResource(R.string.update_banner_body, info.versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = {
                        vm.openDialog(info)
                        vm.startDownload(info)
                    },
                ) {
                    Text(stringResource(R.string.update_install_now))
                }
                Row {
                    TextButton(onClick = { vm.openDialog(info) }) {
                        Text(stringResource(R.string.update_view_details))
                    }
                    TextButton(onClick = { vm.postpone(info) }) {
                        Text(stringResource(R.string.update_postpone))
                    }
                }
            }
        }
    }
}

/**
 * Details dialog shared by the banner and the manual settings check:
 * version, changelog summary, verified download with progress, install
 * prompt, and postpone. The download is optional at every step.
 */
@Composable
fun UpdateFlowHost(vm: UpdateViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val info = state.dialog ?: return

    AlertDialog(
        onDismissRequest = { vm.closeDialog() },
        title = { Text(stringResource(R.string.update_available_title, info.versionName)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.update_changelog_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                if (info.changelogSummary.isBlank()) {
                    Text(
                        stringResource(R.string.update_source_note),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        info.changelogSummary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                when (val download = state.download) {
                    is UpdateViewModel.DownloadState.Progress -> {
                        Text(
                            stringResource(R.string.update_downloading),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        val fraction = download.total?.takeIf { it > 0 }?.let {
                            (download.bytes.toFloat() / it).coerceIn(0f, 1f)
                        }
                        if (fraction != null) {
                            LinearProgressIndicator(progress = { fraction })
                        } else {
                            LinearProgressIndicator()
                        }
                    }
                    is UpdateViewModel.DownloadState.Verified -> {
                        Text(
                            stringResource(R.string.update_verify_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    UpdateViewModel.DownloadState.Failed -> {
                        Text(
                            stringResource(R.string.update_download_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    null -> Unit
                }
            }
        },
        confirmButton = {
            when (val download = state.download) {
                is UpdateViewModel.DownloadState.Verified -> {
                    TextButton(onClick = { vm.install(download.file) }) {
                        Text(stringResource(R.string.update_install))
                    }
                }
                is UpdateViewModel.DownloadState.Progress -> {
                    CircularProgressIndicator(Modifier.width(20.dp).height(20.dp))
                }
                UpdateViewModel.DownloadState.Failed -> {
                    TextButton(onClick = { vm.startDownload(info) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
                null -> {
                    TextButton(onClick = { vm.startDownload(info) }) {
                        Text(stringResource(R.string.update_download))
                    }
                }
            }
        },
        dismissButton = {
            Row {
                if (state.download == null || state.download is UpdateViewModel.DownloadState.Failed) {
                    TextButton(onClick = { vm.postpone(info) }) {
                        Text(stringResource(R.string.update_postpone))
                    }
                }
                TextButton(onClick = { vm.closeDialog() }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        },
    )
}
