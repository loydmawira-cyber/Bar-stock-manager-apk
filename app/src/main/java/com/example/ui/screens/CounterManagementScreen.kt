package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Counter
import com.example.data.model.CounterStockWithItem
import com.example.data.model.Item
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.CategoryBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SkyBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CounterManagementScreen(
    currentUser: User?,
    counters: List<Counter>,
    allItems: List<Item>,
    selectedCounterId: Long?,
    counterStocks: List<CounterStockWithItem>,
    onSelectCounter: (Long) -> Unit,
    onCreateCounter: (name: String, location: String) -> Unit,
    onUpdateCounter: (counterId: Long, name: String, location: String) -> Unit = { _, _, _ -> },
    onDeleteCounter: (counterId: Long) -> Unit = {},
    onAssignItemToCounter: (counterId: Long, itemId: Long, initialQty: Int) -> Unit,
    onRestockClick: (Counter) -> Unit
) {
    var showCreateCounterDialog by remember { mutableStateOf(false) }
    var showAssignItemDialog by remember { mutableStateOf(false) }

    // Dialog state for edit/delete
    var counterToEdit by remember { mutableStateOf<Counter?>(null) }
    var counterToDelete by remember { mutableStateOf<Counter?>(null) }

    var editNameInput by remember { mutableStateOf("") }
    var editLocationInput by remember { mutableStateOf("") }

    var newCounterName by remember { mutableStateOf("") }
    var newCounterLocation by remember { mutableStateOf("") }

    var selectedItemForAssign by remember { mutableStateOf<Item?>(null) }
    var assignInitialQtyText by remember { mutableStateOf("24") }
    var expandedItemMenu by remember { mutableStateOf(false) }

    val activeCounter = counters.firstOrNull { it.id == selectedCounterId } ?: counters.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELLING COUNTERS & INVENTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary,
                    letterSpacing = 1.sp
                )

                if (currentUser?.role == UserRole.ADMIN) {
                    Button(
                        onClick = { showCreateCounterDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_new_counter_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Counter", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Horizontal Counter Selector Pills (with combinedClickable for long press)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    counters.forEach { counter ->
                        val isSelected = counter.id == activeCounter?.id
                        var showDropdown by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = if (isSelected) AmberPrimary else DarkSurface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onSelectCounter(counter.id) },
                                        onLongClick = {
                                            if (currentUser?.role == UserRole.ADMIN) {
                                                onSelectCounter(counter.id)
                                                showDropdown = true
                                            }
                                        }
                                    )
                                    .testTag("counter_pill_${counter.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = counter.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                    Text(
                                        text = if (counter.activeAttendantId != null) "● Active" else "○ Idle",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.Black.copy(alpha = 0.8f) else EmeraldGreen
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Counter Details") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        showDropdown = false
                                        counterToEdit = counter
                                        editNameInput = counter.name
                                        editLocationInput = counter.location
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Counter", color = CrimsonRed) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = CrimsonRed) },
                                    onClick = {
                                        showDropdown = false
                                        counterToDelete = counter
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tip: Long press any counter pill to edit or delete it",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Active Counter Detail Card
        activeCounter?.let { counter ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = counter.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (currentUser?.role == UserRole.ADMIN) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                counterToEdit = counter
                                                editNameInput = counter.name
                                                editLocationInput = counter.location
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit counter",
                                                tint = AmberPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                counterToDelete = counter
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete counter",
                                                tint = CrimsonRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Location: ${counter.location}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (counter.activeAttendantId != null) {
                                StatusBadge(
                                    text = "On Duty: ${counter.activeAttendantName}",
                                    containerColor = Color(0xFF0C4A6E),
                                    contentColor = SkyBlue
                                )
                            } else {
                                StatusBadge(
                                    text = "Available",
                                    containerColor = Color(0xFF064E3B),
                                    contentColor = EmeraldGreen
                                )
                            }
                        }

                        if (currentUser?.role == UserRole.ADMIN) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedItemForAssign = allItems.firstOrNull()
                                        showAssignItemDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Assign Item", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onRestockClick(counter) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Restock Units", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Stock Table at this counter
            item {
                Text(
                    text = "ASSIGNED BEVERAGES & CURRENT STOCK (${counterStocks.size} Items)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            if (counterStocks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No items assigned to this counter yet. Click 'Assign Item' to populate stock.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(counterStocks) { stock ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryBadge(category = stock.category)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${formatCurrency(stock.unitPrice)} / ${stock.unitType}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stock.itemName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("On Shelf", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${stock.currentQuantity}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AmberPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Create Counter Dialog
    if (showCreateCounterDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCounterDialog = false },
            title = { Text("Create New Selling Counter", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCounterName,
                        onValueChange = { newCounterName = it },
                        label = { Text("Counter Name (e.g. Rooftop Bar)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_counter_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )

                    OutlinedTextField(
                        value = newCounterLocation,
                        onValueChange = { newCounterLocation = it },
                        label = { Text("Physical Location (e.g. 3rd Floor Poolside)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCounterName.isNotBlank()) {
                            onCreateCounter(newCounterName, newCounterLocation)
                            showCreateCounterDialog = false
                            newCounterName = ""
                            newCounterLocation = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Create Counter", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCounterDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Counter Dialog
    counterToEdit?.let { counter ->
        AlertDialog(
            onDismissRequest = { counterToEdit = null },
            title = { Text("Edit Counter Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Counter Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )

                    OutlinedTextField(
                        value = editLocationInput,
                        onValueChange = { editLocationInput = it },
                        label = { Text("Physical Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.isNotBlank()) {
                            onUpdateCounter(counter.id, editNameInput, editLocationInput)
                            counterToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { counterToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Counter Dialog
    counterToDelete?.let { counter ->
        AlertDialog(
            onDismissRequest = { counterToDelete = null },
            title = { Text("Delete '${counter.name}'?", fontWeight = FontWeight.Bold, color = CrimsonRed) },
            text = {
                Text("Are you sure you want to delete this selling counter? This will remove all assigned stock configurations for this counter.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCounter(counter.id)
                        counterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("Delete Counter", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { counterToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Assign Item to Counter Dialog
    if (showAssignItemDialog && activeCounter != null) {
        AlertDialog(
            onDismissRequest = { showAssignItemDialog = false },
            title = { Text("Assign Item to ${activeCounter.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedItemMenu,
                        onExpandedChange = { expandedItemMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedItemForAssign?.name ?: "Select Item",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Alcohol / Beverage") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItemMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedItemMenu,
                            onDismissRequest = { expandedItemMenu = false }
                        ) {
                            allItems.forEach { itm ->
                                DropdownMenuItem(
                                    text = { Text("${itm.name} (${itm.category.name}) - ${formatCurrency(itm.unitPrice)}") },
                                    onClick = {
                                        selectedItemForAssign = itm
                                        expandedItemMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = assignInitialQtyText,
                        onValueChange = { assignInitialQtyText = it },
                        label = { Text("Initial Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = selectedItemForAssign
                        val qty = assignInitialQtyText.toIntOrNull() ?: 0
                        if (item != null && qty > 0) {
                            onAssignItemToCounter(activeCounter.id, item.id, qty)
                            showAssignItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Assign Stock", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignItemDialog = false }) { Text("Cancel") }
            }
        )
    }
}
