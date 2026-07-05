package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.MedicineEntity
import com.example.data.local.entities.SyncStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesScreen(
    medicines: List<MedicineEntity>,
    onAddMedicine: (name: String, activeIngredient: String, stock: Double, dosageUnit: String) -> Unit,
    onUpdateMedicine: (MedicineEntity) -> Unit,
    onDeleteMedicine: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isAddingByDialog by remember { mutableStateOf(false) }
    var editingMedicine by remember { mutableStateOf<MedicineEntity?>(null) }
    var medicineToDelete by remember { mutableStateOf<MedicineEntity?>(null) }

    val filteredMedicines = medicines.filter { med ->
        med.name.contains(searchQuery, ignoreCase = true) || 
        med.activeIngredient.contains(searchQuery, ignoreCase = true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RanchSoftSand)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Buscador
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar medicamento o componente...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RanchBrown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("medicine_search_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RanchBrown
                    )
                )
            }

            // Lista de medicamentos
            if (filteredMedicines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vaccines,
                            contentDescription = null,
                            tint = RanchGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sin Medicamentos registrados",
                            fontWeight = FontWeight.Bold,
                            color = RanchDarkSand,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Registra los suministros, vacunas o antiparasitarios de tu granja.",
                            color = RanchGray,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("medicine_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredMedicines) { medicine ->
                        MedicineItemRow(
                            medicine = medicine,
                            onEditClick = { editingMedicine = medicine },
                            onDeleteClick = { medicineToDelete = medicine }
                        )
                    }
                }
            }
        }

        // FAB para añadir medicamento
        FloatingActionButton(
            onClick = { isAddingByDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .testTag("add_medicine_button"),
            containerColor = RanchBrown,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir Medicamento")
        }

        // DIALOGS
        if (isAddingByDialog) {
            MedicineFormDialog(
                title = "Registrar Medicamento",
                onDismiss = { isAddingByDialog = false },
                onSave = { name, ing, stock, unit ->
                    onAddMedicine(name, ing, stock, unit)
                    isAddingByDialog = false
                }
            )
        }

        if (editingMedicine != null) {
            val m = editingMedicine!!
            MedicineFormDialog(
                title = "Actualizar Medicamento",
                initialName = m.name,
                initialIngredient = m.activeIngredient,
                initialStock = m.stock,
                initialUnit = m.dosageUnit,
                onDismiss = { editingMedicine = null },
                onSave = { name, ing, stock, unit ->
                    onUpdateMedicine(
                        m.copy(
                            name = name,
                            activeIngredient = ing,
                            stock = stock,
                            dosageUnit = unit
                        )
                    )
                    editingMedicine = null
                }
            )
        }

        if (medicineToDelete != null) {
            AlertDialog(
                onDismissRequest = { medicineToDelete = null },
                title = { Text("Eliminar Medicamento", fontWeight = FontWeight.Bold, color = RanchDarkSand) },
                text = { Text("¿Deseas eliminar el registro de '${medicineToDelete!!.name}'? Si está en uso en tratamientos, podría afectar los históricos.", color = RanchDarkSand) },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteMedicine(medicineToDelete!!.id)
                            medicineToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RanchError)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { medicineToDelete = null }) {
                        Text("Cancelar", color = RanchDarkSand)
                    }
                }
            )
        }
    }
}

@Composable
fun MedicineItemRow(
    medicine: MedicineEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isLowStock = medicine.stock <= 5.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isLowStock) BorderStroke(1.dp, RanchError.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Vaccines,
                contentDescription = null,
                tint = if (isLowStock) RanchError else RanchBrown,
                modifier = Modifier
                    .size(44.dp)
                    .background(if (isLowStock) RanchError.copy(alpha = 0.1f) else RanchLightGray, CircleShape)
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    fontWeight = FontWeight.Bold,
                    color = RanchDarkSand,
                    fontSize = 16.sp
                )
                Text(
                    text = "Comp: ${medicine.activeIngredient}",
                    color = RanchGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Stock: ${medicine.stock} ${medicine.dosageUnit}",
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) RanchError else RanchDarkGreen,
                        fontSize = 13.sp
                    )
                    if (isLowStock) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = RanchError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Bajo Stock",
                                color = RanchError,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Sync status
                val (icon, tint) = when (medicine.syncStatus) {
                    SyncStatus.SYNCED -> Pair(Icons.Default.CloudQueue, RanchGreenPrimary)
                    SyncStatus.PENDING_INSERT -> Pair(Icons.Default.CloudSync, RanchSyncBlue)
                    SyncStatus.PENDING_UPDATE -> Pair(Icons.Default.CloudUpload, RanchAmber)
                    SyncStatus.PENDING_DELETE -> Pair(Icons.Default.DeleteForever, RanchError)
                }
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = RanchGreenPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RanchError, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MedicineFormDialog(
    title: String,
    initialName: String = "",
    initialIngredient: String = "",
    initialStock: Double = 10.0,
    initialUnit: String = "ml",
    onDismiss: () -> Unit,
    onSave: (name: String, ingredient: String, stock: Double, unit: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var ingredient by remember { mutableStateOf(initialIngredient) }
    var stockStr by remember { mutableStateOf(initialStock.toString()) }
    var unit by remember { mutableStateOf(initialUnit) }

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = RanchDarkSand) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Comercial") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_med_name")
                )

                OutlinedTextField(
                    value = ingredient,
                    onValueChange = { ingredient = it },
                    label = { Text("Principio Activo (Ej. Ivermectina)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_med_component")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Cantidad / Stock") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f).testTag("dialog_med_stock")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unidad (ml, g, etc)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_med_unit")
                    )
                }

                if (showError) {
                    Text("Por favor, llena todos los campos. El stock debe ser un número válido.", color = RanchError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = stockStr.toDoubleOrNull()
                    if (name.isBlank() || ingredient.isBlank() || s == null || unit.isBlank()) {
                        showError = true
                    } else {
                        onSave(name, ingredient, s, unit)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RanchBrown)
            ) {
                Text("Guardar Medicamento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = RanchDarkSand)
            }
        }
    )
}
