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
fun Step3DestinationScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    WizardScaffold(
        currentStep = 3,
        title       = "Destinazione",
        subtitle    = "Scegli il foglio Google e la modalità",
        onBack      = onBack,
        onNext      = onNext,
    ) {
        // Implemented in Module 3: Google Sign-In, sheet picker, overwrite/append toggle,
        // scheduling options (one-shot / daily / weekly / monthly).
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = "Destinazione e modalità\n(Modulo 3)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
