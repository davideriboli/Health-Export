package com.healthexport.ui.wizard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthexport.ui.components.WizardScaffold

@Composable
fun Step4SummaryScreen(
    viewModel: WizardViewModel,
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val isExporting = uiState.exportState is ExportState.InProgress

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* user authorised — they can retry */ }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WizardEvent.SheetsAuthRequired -> authLauncher.launch(event.intent)
            }
        }
    }

    WizardScaffold(
        currentStep = 4,
        title       = "Riepilogo",
        subtitle    = "Controlla e avvia l'export",
        onBack      = if (isExporting) null else onBack,
        onNext      = {
            when (uiState.exportState) {
                is ExportState.Success -> onExport()
                else                   -> viewModel.startExport()
            }
        },
        nextLabel   = when (uiState.exportState) {
            is ExportState.Success -> "Fatto"
            is ExportState.Error   -> "Riprova"
            else                   -> "Esporta"
        },
        nextEnabled = !isExporting && uiState.googleAccountEmail != null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {

            // ── Summary cards ─────────────────────────────────────────────
            SummaryCard(label = "Dati") {
                Text("${uiState.selectedTypes.size} tipi selezionati",
                    style = MaterialTheme.typography.bodyMedium)
                Text(
                    uiState.selectedTypes.joinToString(", ") { it.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            SummaryCard(label = "Intervallo") {
                Text(uiState.timeRange.displayName, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            SummaryCard(label = "Account") {
                Text(uiState.googleAccountEmail ?: "–", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            SummaryCard(label = "Foglio") {
                val dest = when (uiState.spreadsheetMode) {
                    SpreadsheetMode.CREATE_NEW   -> "Nuovo: \"${uiState.spreadsheetName}\""
                    SpreadsheetMode.USE_EXISTING -> "Esistente: ${uiState.spreadsheetId ?: "–"}"
                }
                Text(dest, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            SummaryCard(label = "Modalità") {
                val mode = when (uiState.exportMode) {
                    ExportMode.OVERWRITE -> "Sovrascrivi"
                    ExportMode.APPEND    -> "Aggiungi (append)"
                }
                val schedule = when (uiState.scheduleType) {
                    ScheduleType.ONE_SHOT -> "Una tantum"
                    ScheduleType.DAILY    -> "Giornaliero"
                    ScheduleType.WEEKLY   -> "Settimanale"
                    ScheduleType.MONTHLY  -> "Mensile"
                }
                Text("$mode · $schedule", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Export state ──────────────────────────────────────────────
            when (val state = uiState.exportState) {
                is ExportState.Idle -> Unit

                is ExportState.InProgress -> ExportProgressBlock(state)

                is ExportState.Success -> ExportSuccessBlock(state)

                is ExportState.Error -> ExportErrorBlock(state.message)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Export state composables ──────────────────────────────────────────────────

@Composable
private fun ExportProgressBlock(state: ExportState.InProgress) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Esportazione in corso…", style = MaterialTheme.typography.titleSmall)

        if (state.currentType.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text  = state.currentType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.recordCount > 0) {
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "${state.recordCount} righe",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress  = { state.progress },
            modifier  = Modifier.fillMaxWidth(),
            strokeCap = StrokeCap.Round,
        )

        if (state.totalTypes > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "${state.currentIndex + 1} / ${state.totalTypes}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExportSuccessBlock(state: ExportState.Success) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = Icons.Default.CheckCircle,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(24.dp),
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            Text(
                text       = "Export completato!",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text  = "${state.totalRows} righe esportate",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state.skippedTypes > 0) {
            Text(
                text  = "${state.skippedTypes} ${if (state.skippedTypes == 1) "tipo saltato" else "tipi saltati"} (nessun dato nel periodo)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExportErrorBlock(message: String) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onErrorContainer,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.padding(horizontal = 6.dp))
            Text(
                text     = message,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Shared card ───────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    label: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}
