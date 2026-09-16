package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.data.model.ItemCategory
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.CategoryBadge
import com.example.ui.components.LocalCurrencySymbol
import com.example.ui.components.optionalLabel
import com.example.ui.components.requiredLabel
import com.example.ui.theme.CrimsonRed
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SkyBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemManagementScreen(
    currentUser: User?,
    items: List<Item>,
    onCreateItem: (name: String, category: ItemCategory, unitPrice: Double, casePrice: Double, unitType: String, description: String) -> Unit,
    onUpdateItem: (itemId: Long, name: String, category: ItemCategory, unitPrice: Double, casePrice: Double, unitType: String, description: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteItem: (itemId: Long) -> Unit = {},
    onSyncData: () -> Unit = {}
) {
    val currency = LocalCurrencySymbol.current
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }

    // Form states for New / Edit Item
    var itemName by remember { mutableStateOf("") }
    var itemCategory by remember { mutableStateOf(ItemCategory.BEER) }
    var itemUnitPrice by remember { mutableStateOf("6.00") }
    var itemCasePrice by remember { mutableStateOf("120.00") }
    var itemUnitType by remember { mutableStateOf("Bottle") }
    var itemDesc by remember { mutableStateOf("") }
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    fun openEditDialog(item: Item) {
        editingItem = item
        itemName = item.name
        itemCategory = item.category
        itemUnitPrice = item.unitPrice.toString()
        itemCasePrice = item.casePrice.toString()
        itemUnitType = item.unitType
        itemDesc = item.description
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Explanatory Pricing Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Pricing Guide",
                        tint = AmberPrimary,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SET STOCK PRICES & UNIT TYPES HERE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AmberPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure beverage unit types (Bottle, Shot/Tot, Can, Glass, Peg) and set selling prices per unit and case. Prices set here are used across all counters for shift sales calculation.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "STOCK PRICES LIST (${items.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    OutlinedButton(
                        onClick = onSyncData,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync Prices",
                            tint = AmberPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Sync", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (currentUser?.role == UserRole.OWNER || currentUser?.role == UserRole.MANAGER) {
                    Button(
                        onClick = {
                            itemName = ""
                            itemCategory = ItemCategory.BEER
                            itemUnitPrice = "6.00"
                            itemCasePrice = "120.00"
                            itemUnitType = "Bottle"
                            itemDesc = ""
                            showAddItemDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_new_stock_item_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Beverage", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Beverage List with Unit Prices and Edit Buttons
        items(items) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = currentUser?.role == UserRole.OWNER || currentUser?.role == UserRole.MANAGER) { openEditDialog(item) }
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
                            CategoryBadge(category = item.category)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SkyBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Unit: ${item.unitType}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SkyBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (item.description.isNotBlank()) {
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${formatCurrency(item.unitPrice)} / ${item.unitType.takeWhile { it != ' ' }}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = EmeraldGreen
                            )
                            if (item.casePrice > 0) {
                                Text(
                                    text = "Case: ${formatCurrency(item.casePrice)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (currentUser?.role == UserRole.OWNER || currentUser?.role == UserRole.MANAGER) {
                            IconButton(
                                onClick = { openEditDialog(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Price",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
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

    // Add Item Dialog
    if (showAddItemDialog) {
        val uPriceVal = itemUnitPrice.toDoubleOrNull()
        val isNameValid = itemName.trim().isNotBlank()
        val isPriceValid = uPriceVal != null && uPriceVal > 0.0
        val isUnitValid = itemUnitType.trim().isNotBlank()
        val isFormValid = isNameValid && isPriceValid && isUnitValid

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Beverage & Set Prices", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = requiredLabel("Beverage / Brand Name"),
                        isError = itemName.isNotEmpty() && !isNameValid,
                        supportingText = if (itemName.isNotEmpty() && !isNameValid) { { Text("Name cannot be blank", color = CrimsonRed) } } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stock_item_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedCategoryMenu,
                        onExpandedChange = { expandedCategoryMenu = it }
                    ) {
                        OutlinedTextField(
                            value = itemCategory.name,
                            onValueChange = {},
                            readOnly = true,
                            label = requiredLabel("Category"),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategoryMenu,
                            onDismissRequest = { expandedCategoryMenu = false }
                        ) {
                            ItemCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        itemCategory = cat
                                        itemUnitType = when (cat) {
                                            ItemCategory.BEER -> "Bottle"
                                            ItemCategory.SPIRIT -> "Bottle"
                                            ItemCategory.TOT -> "Shot (30ml)"
                                            ItemCategory.WINE -> "Glass"
                                            ItemCategory.SOFT_DRINK -> "Can"
                                            ItemCategory.OTHER -> "Unit"
                                        }
                                        expandedCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = itemUnitPrice,
                            onValueChange = { itemUnitPrice = it },
                            label = requiredLabel("Selling Price ($currency)"),
                            isError = itemUnitPrice.isNotEmpty() && !isPriceValid,
                            supportingText = if (itemUnitPrice.isNotEmpty() && !isPriceValid) { { Text("Must be > 0", color = CrimsonRed) } } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                        )

                        OutlinedTextField(
                            value = itemCasePrice,
                            onValueChange = { itemCasePrice = it },
                            label = optionalLabel("Case Price ($currency)"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                        )
                    }

                    OutlinedTextField(
                        value = itemUnitType,
                        onValueChange = { itemUnitType = it },
                        label = requiredLabel("Unit Type"),
                        isError = itemUnitType.isNotEmpty() && !isUnitValid,
                        supportingText = if (itemUnitType.isNotEmpty() && !isUnitValid) { { Text("Unit type required", color = CrimsonRed) } } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uPrice = itemUnitPrice.toDoubleOrNull() ?: 0.0
                        val cPrice = itemCasePrice.toDoubleOrNull() ?: 0.0
                        if (isFormValid) {
                            onCreateItem(itemName.trim(), itemCategory, uPrice, cPrice, itemUnitType.trim(), itemDesc.trim())
                            showAddItemDialog = false
                            itemName = ""
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Save Item", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Item Dialog
    editingItem?.let { targetItem ->
        val uPriceVal = itemUnitPrice.toDoubleOrNull()
        val isNameValid = itemName.trim().isNotBlank()
        val isPriceValid = uPriceVal != null && uPriceVal > 0.0
        val isUnitValid = itemUnitType.trim().isNotBlank()
        val isFormValid = isNameValid && isPriceValid && isUnitValid

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edit Item & Price", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = requiredLabel("Beverage Name"),
                        isError = !isNameValid,
                        supportingText = if (!isNameValid) { { Text("Name cannot be blank", color = CrimsonRed) } } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = itemUnitPrice,
                            onValueChange = { itemUnitPrice = it },
                            label = requiredLabel("Unit Selling Price ($currency)"),
                            isError = !isPriceValid,
                            supportingText = if (!isPriceValid) { { Text("Must be > 0", color = CrimsonRed) } } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                        )

                        OutlinedTextField(
                            value = itemCasePrice,
                            onValueChange = { itemCasePrice = it },
                            label = optionalLabel("Case Bulk Price ($currency)"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AmberPrimary)
                        )
                    }

                    OutlinedTextField(
                        value = itemUnitType,
                        onValueChange = { itemUnitType = it },
                        label = requiredLabel("Unit Type"),
                        isError = !isUnitValid,
                        supportingText = if (!isUnitValid) { { Text("Unit type required", color = CrimsonRed) } } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onDeleteItem(targetItem.id)
                            editingItem = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }

                    Button(
                        onClick = {
                            val uPrice = itemUnitPrice.toDoubleOrNull() ?: 0.0
                            val cPrice = itemCasePrice.toDoubleOrNull() ?: 0.0
                            if (isFormValid) {
                                onUpdateItem(targetItem.id, itemName.trim(), itemCategory, uPrice, cPrice, itemUnitType.trim(), itemDesc.trim())
                                editingItem = null
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                    ) {
                        Text("Update Price", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("Cancel") }
            }
        )
    }
}
