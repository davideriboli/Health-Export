package com.healthexport.ui.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healthexport.ui.components.WizardScaffold

@Composable
fun Step4SummaryScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
) {
    WizardScaffold(
        currentStep = 4,
        title       = "Riepilogo",
        subtitle    = "Controlla e avvia l'export",
        onBack      = onBack,
        onNext      = onExport,
        nextLabel   = "Esporta",
    ) {
        // Implemented in Module 4: summary card with all choices, progress bar during export.
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "Riepilogo e conferma\n(Modulo 4)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
