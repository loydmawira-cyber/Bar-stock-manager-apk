package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.requiredLabel
import com.example.ui.theme.CrimsonRed

@Composable
fun AdminInventoryScreen(
    counters: List<Counter>,
    onSelectCounter: (Long) -> Unit,
    onUpdateCounter: (counterId: Long, name: String, location: String) -> Unit = { _, _, _ -> },
    onDeleteCounter: (counterId: Long) -> Unit = {},
    countersContent: @Composable () -> Unit,
    storeContent: @Composable () -> Unit
) {
    val locations = counters.map { it.id.toString() to it.name } + ("STORE" to "Store")
    var selectedLocation by remember(locations) {
        mutableStateOf(locations.firstOrNull()?.first ?: "STORE")
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var editingCounter by remember { mutableStateOf<Counter?>(null) }
    var deletingCounter by remember { mutableStateOf<Counter?>(null) }
    var editName by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    val selectedCounter = counters.firstOrNull { it.id.toString() == selectedLocation }

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
                            menuExpanded = false
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
            if (selectedCounter != null) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Manage counter")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Edit Counter") }, onClick = {
                        editName = selectedCounter.name
                        editLocation = selectedCounter.location
                        editingCounter = selectedCounter
                        menuExpanded = false
                    })
                    DropdownMenuItem(text = { Text("Delete Counter") }, onClick = {
                        deletingCounter = selectedCounter
                        menuExpanded = false
                    })
                }
            }
        }
        if (selectedLocation == "STORE") storeContent() else countersContent()
    }

    editingCounter?.let { counter ->
        val isNameValid = editName.trim().isNotBlank()
        val isLocValid = editLocation.trim().isNotBlank()
        val isFormValid = isNameValid && isLocValid

        AlertDialog(
            onDismissRequest = { editingCounter = null },
            title = { Text("Edit Counter") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = requiredLabel("Counter name"),
                        isError = editName.isNotEmpty() && !isNameValid,
                        supportingText = if (editName.isNotEmpty() && !isNameValid) { { Text("Name cannot be blank", color = CrimsonRed) } } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = requiredLabel("Location"),
                        isError = editLocation.isNotEmpty() && !isLocValid,
                        supportingText = if (editLocation.isNotEmpty() && !isLocValid) { { Text("Location cannot be blank", color = CrimsonRed) } } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isFormValid) {
                            onUpdateCounter(counter.id, editName.trim(), editLocation.trim())
                            editingCounter = null
                        }
                    },
                    enabled = isFormValid
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingCounter = null }) { Text("Cancel") } }
        )
    }

    deletingCounter?.let { counter ->
        AlertDialog(
            onDismissRequest = { deletingCounter = null },
            title = { Text("Delete ${counter.name}?") },
            text = { Text("This will remove the counter and its stock configuration. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteCounter(counter.id)
                    deletingCounter = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deletingCounter = null }) { Text("Cancel") } }
        )
    }
}
