package com.healthexport.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shared scaffold for every wizard step.
 *
 * Provides:
 * - Animated progress bar at the top
 * - Step indicator dots
 * - Title + subtitle slot
 * - Scrollable content area (passed as [content])
 * - Bottom bar with Back / Next buttons
 */
@Composable
fun WizardScaffold(
    currentStep: Int,           // 1-based
    totalSteps: Int = 4,
    title: String,
    subtitle: String = "",
    onBack: (() -> Unit)? = null,
    onNext: () -> Unit,
    nextLabel: String = if (currentStep == totalSteps) "Esporta" else "Avanti",
    nextEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "wizard_progress",
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Top progress bar ─────────────────────────────────────────
            LinearProgressIndicator(
                progress         = { progress },
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color            = MaterialTheme.colorScheme.primary,
                trackColor       = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap        = StrokeCap.Round,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                // ── Step dots ────────────────────────────────────────────
                StepDots(current = currentStep, total = totalSteps)
                Spacer(modifier = Modifier.height(20.dp))

                // ── Title ─────────────────────────────────────────────
                Text(
                    text  = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text  = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Scrollable content ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                content()
            }

            // ── Bottom navigation bar ─────────────────────────────────────
            WizardBottomBar(
                showBack    = onBack != null,
                onBack      = onBack ?: {},
                nextLabel   = nextLabel,
                nextEnabled = nextEnabled,
                onNext      = onNext,
            )
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            val isActive = index + 1 == current
            val isDone   = index + 1 < current
            val color by animateColorAsState(
                targetValue = when {
                    isActive || isDone -> MaterialTheme.colorScheme.primary
                    else               -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "dot_color_$index",
            )
            val widthFraction by animateFloatAsState(
                targetValue = if (isActive) 24f else 8f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "dot_width_$index",
            )
            Box(
                modifier = Modifier
                    .size(width = widthFraction.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun WizardBottomBar(
    showBack: Boolean,
    onBack: () -> Unit,
    nextLabel: String,
    nextEnabled: Boolean,
    onNext: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        color          = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            if (showBack) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Indietro", fontWeight = FontWeight.Medium)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick  = onNext,
                enabled  = nextEnabled,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(nextLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
