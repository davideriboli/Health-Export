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
fun Step1DataSelectionScreen(
    onNext: () -> Unit,
) {
    WizardScaffold(
        currentStep = 1,
        title       = "Dati da esportare",
        subtitle    = "Scegli i tipi di dati Health Connect",
        onBack      = null,
        onNext      = onNext,
    ) {
        // Implemented in Module 2: list of Health Connect record types with toggles.
        Box(
            modifier          = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment  = Alignment.Center,
        ) {
            Text(
                text  = "Selezione tipi di dati\n(Modulo 2)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
