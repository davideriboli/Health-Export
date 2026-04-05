package com.healthexport.ui.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.permission.HealthPermission
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthexport.data.healthconnect.HealthRecordType
import com.healthexport.data.healthconnect.RecordCategory
import com.healthexport.ui.components.WizardScaffold
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController

@Composable
fun Step1DataSelectionScreen(
    viewModel: WizardViewModel,
    onNext: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // HC permission launcher — called when user taps "Concedi permessi"
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { granted -> viewModel.onPermissionsResult(granted) },
    )

    // Re-check permissions whenever the screen becomes active
    LaunchedEffect(Unit) { viewModel.refreshPermissions() }

    val missingPermissions = viewModel.missingPermissions()
    val hasAllPermissions  = missingPermissions.isEmpty()

    WizardScaffold(
        currentStep = 1,
        title       = "Dati da esportare",
        subtitle    = "Scegli cosa esportare da Health Connect",
        onBack      = null,
        onNext      = onNext,
        nextEnabled = uiState.selectedTypes.isNotEmpty(),
    ) {
        LazyColumn {

            // ── Permission banner ─────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = uiState.sdkStatus == SdkStatus.AVAILABLE && !hasAllPermissions,
                    enter   = expandVertically(),
                    exit    = shrinkVertically(),
                ) {
                    PermissionBanner(
                        onGrant = { permissionLauncher.launch(missingPermissions) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // ── HC unavailable banner ─────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = uiState.sdkStatus == SdkStatus.UNAVAILABLE ||
                              uiState.sdkStatus == SdkStatus.UPDATE_REQUIRED,
                ) {
                    UnavailableBanner(
                        needsUpdate = uiState.sdkStatus == SdkStatus.UPDATE_REQUIRED,
                        modifier    = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            // ── Global select/deselect ────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { viewModel.selectAll() }) {
                        Text("Seleziona tutto", style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(onClick = { viewModel.deselectAll() }) {
                        Text("Deseleziona tutto", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // ── One section per category ──────────────────────────────────
            RecordCategory.entries.forEach { category ->
                val types = HealthRecordType.byCategory[category] ?: emptyList()

                item(key = "header_${category.name}") {
                    CategoryHeader(
                        category = category,
                        types    = types,
                        selected = uiState.selectedTypes,
                        onToggleAll = { allSelected ->
                            viewModel.setAllInCategory(category, !allSelected)
                        },
                    )
                }

                items(items = types, key = { it.id }) { type ->
                    RecordTypeRow(
                        type     = type,
                        checked  = type in uiState.selectedTypes,
                        onToggle = { viewModel.toggleType(type) },
                    )
                }

                item(key = "divider_${category.name}") {
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun CategoryHeader(
    category: RecordCategory,
    types: List<HealthRecordType>,
    selected: Set<HealthRecordType>,
    onToggleAll: (allSelected: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allSelected = types.all { it in selected }
    val someSelected = types.any { it in selected }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = category.icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text     = category.displayName,
            style    = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked         = allSelected,
            onCheckedChange = { onToggleAll(allSelected) },
        )
    }
}

@Composable
private fun RecordTypeRow(
    type: HealthRecordType,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = type.displayName,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun PermissionBanner(
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier      = modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        color         = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Default.Warning,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onErrorContainer,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Permessi mancanti",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text  = "Alcuni dati potrebbero non essere leggibili",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onGrant) {
                Text("Concedi")
            }
        }
    }
}

@Composable
private fun UnavailableBanner(
    needsUpdate: Boolean,
    modifier: Modifier = Modifier,
) {
    val message = if (needsUpdate)
        "Health Connect deve essere aggiornato per funzionare con questa app."
    else
        "Health Connect non è disponibile su questo dispositivo."

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
    ) {
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
