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
fun Step2TimeRangeScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    WizardScaffold(
        currentStep = 2,
        title       = "Intervallo temporale",
        subtitle    = "Quanti dati vuoi esportare?",
        onBack      = onBack,
        onNext      = onNext,
    ) {
        // Implemented in Module 2: preset chips (24h / 7gg / 30gg) + custom date picker.
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "Selezione intervallo temporale\n(Modulo 2)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
