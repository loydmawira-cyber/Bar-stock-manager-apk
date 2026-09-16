package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    val selectedIndex = locations.indexOfFirst { it.first == selectedLocation }.coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedIndex) {
            locations.forEachIndexed { index, location ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = {
                        selectedLocation = location.first
                        if (location.first != "STORE") onSelectCounter(location.first.toLong())
                    },
                    text = { Text(location.second) }
                )
            }
        }
        if (selectedLocation == "STORE") storeContent() else countersContent()
    }
}
