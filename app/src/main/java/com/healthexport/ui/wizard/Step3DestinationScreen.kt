package com.healthexport.ui.wizard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthexport.data.google.GoogleSignInResult
import com.healthexport.ui.components.WizardScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step3DestinationScreen(
    viewModel: WizardViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Launcher for the Sheets auth recovery intent (UserRecoverableAuthIOException)
    val authRecoveryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* user has authorised — they can retry the export from Step 4 */ }

    val canProceed = uiState.googleAccountEmail != null &&
            when (uiState.spreadsheetMode) {
                SpreadsheetMode.CREATE_NEW   -> uiState.spreadsheetName.isNotBlank()
                SpreadsheetMode.USE_EXISTING -> !uiState.spreadsheetId.isNullOrBlank()
            }

    WizardScaffold(
        currentStep = 3,
        title       = "Destinazione",
        subtitle    = "Scegli il foglio e la modalità di export",
        onBack      = onBack,
        onNext      = onNext,
        nextEnabled = canProceed,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {

            // ── Google account ────────────────────────────────────────────
            SectionTitle("Account Google")
            Spacer(Modifier.height(8.dp))

            if (uiState.googleAccountEmail == null) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = viewModel.authManager.signIn(context)
                            when (result) {
                                is GoogleSignInResult.Success ->
                                    viewModel.onSignInSuccess(result.email, result.displayName)
                                is GoogleSignInResult.Error ->
                                    { /* TODO: show snackbar */ }
                                GoogleSignInResult.Cancelled -> Unit
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Accedi con Google")
                }
            } else {
                AccountCard(
                    email       = uiState.googleAccountEmail!!,
                    displayName = uiState.googleAccountName,
                    onSignOut   = { viewModel.signOut() },
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Spreadsheet ───────────────────────────────────────────────
            SectionTitle("Google Spreadsheet")
            Spacer(Modifier.height(12.dp))

            SpreadsheetModeRow(
                label    = "Crea nuovo foglio",
                selected = uiState.spreadsheetMode == SpreadsheetMode.CREATE_NEW,
                onClick  = { viewModel.setSpreadsheetMode(SpreadsheetMode.CREATE_NEW) },
            )
            if (uiState.spreadsheetMode == SpreadsheetMode.CREATE_NEW) {
                OutlinedTextField(
                    value         = uiState.spreadsheetName,
                    onValueChange = { viewModel.setSpreadsheetName(it) },
                    label         = { Text("Nome del foglio") },
                    singleLine    = true,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, bottom = 8.dp),
                )
            }

            SpreadsheetModeRow(
                label    = "Usa foglio esistente",
                selected = uiState.spreadsheetMode == SpreadsheetMode.USE_EXISTING,
                onClick  = { viewModel.setSpreadsheetMode(SpreadsheetMode.USE_EXISTING) },
            )
            if (uiState.spreadsheetMode == SpreadsheetMode.USE_EXISTING) {
                OutlinedTextField(
                    value         = uiState.spreadsheetId ?: "",
                    onValueChange = { viewModel.setSpreadsheetId(it) },
                    label         = { Text("ID o URL del foglio") },
                    singleLine    = true,
                    supportingText = { Text("Incolla l'URL o solo l'ID del foglio Google") },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, bottom = 8.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Export mode ───────────────────────────────────────────────
            SectionTitle("Modalità di scrittura")
            Spacer(Modifier.height(12.dp))

            ExportModeRow(
                label       = "Sovrascrivi",
                description = "Cancella i dati esistenti e scrive da zero",
                selected    = uiState.exportMode == ExportMode.OVERWRITE,
                onClick     = { viewModel.setExportMode(ExportMode.OVERWRITE) },
            )
            Spacer(Modifier.height(4.dp))
            ExportModeRow(
                label       = "Aggiungi (append)",
                description = "Aggiunge nuove righe dopo quelle esistenti",
                selected    = uiState.exportMode == ExportMode.APPEND,
                onClick     = { viewModel.setExportMode(ExportMode.APPEND) },
            )

            Spacer(Modifier.height(24.dp))

            // ── Schedule ──────────────────────────────────────────────────
            SectionTitle("Frequenza di export")
            Spacer(Modifier.height(12.dp))

            val scheduleOptions = listOf(
                ScheduleType.ONE_SHOT to "Una tantum",
                ScheduleType.DAILY    to "Giornaliero",
                ScheduleType.WEEKLY   to "Settimanale",
                ScheduleType.MONTHLY  to "Mensile",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                scheduleOptions.forEach { (type, label) ->
                    FilterChip(
                        selected = uiState.scheduleType == type,
                        onClick  = { viewModel.setScheduleType(type) },
                        label    = { Text(label) },
                    )
                }
            }
            if (uiState.scheduleType != ScheduleType.ONE_SHOT) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "L'export automatico verrà configurato con WorkManager (Modulo 5).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text      = text,
        style     = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color     = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun AccountCard(
    email: String,
    displayName: String?,
    onSignOut: () -> Unit,
) {
    Surface(
        shape         = RoundedCornerShape(12.dp),
        color         = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp,
        modifier      = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Default.CheckCircle,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                if (displayName != null) {
                    Text(displayName, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            TextButton(onClick = onSignOut) { Text("Esci") }
        }
    }
}

@Composable
private fun SpreadsheetModeRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.fillMaxWidth(),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ExportModeRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier          = Modifier.fillMaxWidth(),
    ) {
        RadioButton(selected = selected, onClick = onClick,
            modifier = Modifier.padding(top = 2.dp))
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
