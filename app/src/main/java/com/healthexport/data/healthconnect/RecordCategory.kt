package com.healthexport.data.healthconnect

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

enum class RecordCategory(
    val displayName: String,
    val icon: ImageVector,
) {
    ACTIVITY(    "Attività",      Icons.Default.DirectionsRun),
    BODY(        "Corpo",         Icons.Default.Person),
    VITALS(      "Segni vitali",  Icons.Default.Favorite),
    SLEEP(       "Sonno",         Icons.Default.Bedtime),
    NUTRITION(   "Nutrizione",    Icons.Default.Restaurant),
    REPRODUCTIVE("Riproduzione",  Icons.Default.Female),
}
