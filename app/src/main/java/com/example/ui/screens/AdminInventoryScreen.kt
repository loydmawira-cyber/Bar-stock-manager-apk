package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Counter
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkSurface

@Composable
fun AdminInventoryScreen(
    counters: List<Counter>,
    onSelectCounter: (Long) -> Unit,
    countersContent: @Composable () -> Unit,
    storeContent: @Composable () -> Unit
) {
    var selectedLocation by remember { mutableStateOf("STORE") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            counters.forEach { counter ->
                InventoryLocationCard(
                    title = counter.name,
                    subtitle = "Counter stock",
                    selected = selectedLocation == counter.id.toString(),
                    icon = { Icon(Icons.Filled.Inventory, contentDescription = null, tint = if (selectedLocation == counter.id.toString()) Color.Black else AmberPrimary) },
                    onClick = {
                        selectedLocation = counter.id.toString()
                        onSelectCounter(counter.id)
                    }
                )
            }
            InventoryLocationCard(
                title = "Store",
                subtitle = "Store stock",
                selected = selectedLocation == "STORE",
                icon = { Icon(Icons.Filled.Store, contentDescription = null, tint = if (selectedLocation == "STORE") Color.Black else AmberPrimary) },
                onClick = { selectedLocation = "STORE" }
            )
        }
        if (selectedLocation == "STORE") storeContent() else countersContent()
    }
}

@Composable
private fun InventoryLocationCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) AmberPrimary else DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Text(title, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = if (selected) Color.Black.copy(alpha = 0.75f) else Color.LightGray, fontSize = 11.sp)
        }
    }
}
