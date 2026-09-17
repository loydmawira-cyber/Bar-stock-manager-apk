package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberPrimary
import com.example.data.model.Counter

@Composable
fun AdminInventoryScreen(
    counters: List<Counter>,
    onSelectCounter: (Long) -> Unit,
    countersContent: @Composable () -> Unit,
    storeContent: @Composable () -> Unit
) {
    val locations = counters.map { it.id.toString() to it.name } + ("STORE" to "Store")
    var selectedLocation by remember(locations) {
        mutableStateOf(locations.firstOrNull()?.first ?: "STORE")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(if (locations.size > 4) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                locations.forEach { location ->
                    FilterChip(
                        selected = selectedLocation == location.first,
                        onClick = {
                            selectedLocation = location.first
                            if (location.first != "STORE") onSelectCounter(location.first.toLong())
                        },
                        label = {
                            Text(
                                text = location.second,
                                fontSize = 14.sp,
                                fontWeight = if (selectedLocation == location.first) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        modifier = if (locations.size <= 4) Modifier.weight(1f) else Modifier,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF252830),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
        if (selectedLocation == "STORE") storeContent() else countersContent()
    }

}
