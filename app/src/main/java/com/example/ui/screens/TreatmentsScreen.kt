package com.example.ui.screens

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
import com.example.data.local.entities.AnimalEntity
import com.example.data.local.entities.MedicineEntity
import com.example.data.local.entities.TreatmentEntity
import com.example.data.local.entities.SyncStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentsScreen(
    treatments: List<TreatmentEntity>,
    animals: List<AnimalEntity>,
    medicines: List<MedicineEntity>,
    onAddTreatment: (animalId: String, animalName: String, medicineId: String, medicineName: String, date: String, dosage: Double, notes: String) -> Unit,
    onDeleteTreatment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isAddingByDialog by remember { mutableStateOf(false) }
    var treatmentToDelete by remember { mutableStateOf<TreatmentEntity?>(null) }

    val filteredTreatments = treatments.filter { t ->
        t.animalName.contains(searchQuery, ignoreCase = true) || 
        t.medicineName.contains(searchQuery, ignoreCase = true) ||
        t.notes.contains(searchQuery, ignoreCase = true)
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
                    placeholder = { Text("Buscar por animal o tratamiento...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RanchAmber) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("treatment_search_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RanchAmber
                    )
                )
            }

            // Lista de tratamientos
            if (filteredTreatments.isEmpty()) {
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
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = RanchGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sin tratamientos médicos",
                            fontWeight = FontWeight.Bold,
                            color = RanchDarkSand,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Aplica vacunas, desparasitantes o sueros y regístralos aquí para controlar el ganado y descontar stock.",
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
                        .testTag("treatment_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTreatments) { treatment ->
                        TreatmentItemRow(
                            treatment = treatment,
                            onDeleteClick = { treatmentToDelete = treatment }
                        )
                    }
                }
            }
        }

        // FAB para añadir tratamiento
        FloatingActionButton(
            onClick = { isAddingByDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .testTag("add_treatment_button"),
            containerColor = RanchAmber,
            contentColor = RanchDarkSand
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aplicar Tratamiento")
        }

        // DIALOGS
        if (isAddingByDialog) {
            TreatmentFormDialog(
                animals = animals,
                medicines = medicines,
                onDismiss = { isAddingByDialog = false },
                onSave = { animalId, animalName, medId, medName, date, dosage, notes ->
                    onAddTreatment(animalId, animalName, medId, medName, date, dosage, notes)
                    isAddingByDialog = false
                }
            )
        }

        if (treatmentToDelete != null) {
            AlertDialog(
                onDismissRequest = { treatmentToDelete = null },
                title = { Text("Eliminar Registro de Tratamiento", fontWeight = FontWeight.Bold, color = RanchDarkSand) },
                text = { Text("¿Estás seguro de que deseas eliminar este tratamiento? Nota: Esto no devolverá el stock de medicamento ya descontado.", color = RanchDarkSand) },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteTreatment(treatmentToDelete!!.id)
                            treatmentToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RanchError)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { treatmentToDelete = null }) {
                        Text("Cancelar", color = RanchDarkSand)
                    }
                }
            )
        }
    }
}

@Composable
fun TreatmentItemRow(
    treatment: TreatmentEntity,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MedicalServices,
                contentDescription = null,
                tint = RanchAmber,
                modifier = Modifier
                    .size(44.dp)
                    .background(RanchAmber.copy(alpha = 0.15f), CircleShape)
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = treatment.animalName,
                    fontWeight = FontWeight.Bold,
                    color = RanchDarkSand,
                    fontSize = 16.sp
                )
                Text(
                    text = "Tratamiento: ${treatment.medicineName}",
                    fontWeight = FontWeight.SemiBold,
                    color = RanchBrown,
                    fontSize = 13.sp
                )
                Text(
                    text = "Dosis: ${treatment.dosage} • Fecha: ${treatment.date}",
                    color = RanchGray,
                    fontSize = 12.sp
                )
                if (treatment.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Notas: \"${treatment.notes}\"",
                        color = RanchDarkSand.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val (icon, tint) = when (treatment.syncStatus) {
                    SyncStatus.SYNCED -> Pair(Icons.Default.CloudQueue, RanchGreenPrimary)
                    SyncStatus.PENDING_INSERT -> Pair(Icons.Default.CloudSync, RanchSyncBlue)
                    SyncStatus.PENDING_UPDATE -> Pair(Icons.Default.CloudUpload, RanchAmber)
                    SyncStatus.PENDING_DELETE -> Pair(Icons.Default.DeleteForever, RanchError)
                }
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))

                Spacer(modifier = Modifier.height(16.dp))

                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RanchError, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentFormDialog(
    animals: List<AnimalEntity>,
    medicines: List<MedicineEntity>,
    onDismiss: () -> Unit,
    onSave: (animalId: String, animalName: String, medicineId: String, medicineName: String, date: String, dosage: Double, notes: String) -> Unit
) {
    if (animals.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Faltan Animales", fontWeight = FontWeight.Bold, color = RanchDarkSand) },
            text = { Text("No puedes registrar tratamientos si aún no tienes animales en tu catálogo. Por favor, registra un animal primero.", color = RanchDarkSand) },
            confirmButton = {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RanchGreenPrimary)) {
                    Text("Entendido")
                }
            }
        )
        return
    }

    if (medicines.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Faltan Medicamentos", fontWeight = FontWeight.Bold, color = RanchDarkSand) },
            text = { Text("No puedes aplicar tratamientos si no has registrado ningún medicamento o vacuna en el inventario. Registra uno primero.", color = RanchDarkSand) },
            confirmButton = {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RanchBrown)) {
                    Text("Entendido")
                }
            }
        )
        return
    }

    var selectedAnimal by remember { mutableStateOf(animals.first()) }
    var selectedMedicine by remember { mutableStateOf(medicines.first()) }
    var dosageStr by remember { mutableStateOf("1.0") }
    var date by remember { mutableStateOf("2026-07-05") }
    var notes by remember { mutableStateOf("") }

    var expandedAnimalMenu by remember { mutableStateOf(false) }
    var expandedMedicineMenu by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var stockWarning by remember { mutableStateOf(false) }

    // Monitorear si la dosis supera el stock
    LaunchedEffect(dosageStr, selectedMedicine) {
        val dosage = dosageStr.toDoubleOrNull() ?: 0.0
        stockWarning = dosage > selectedMedicine.stock
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar Tratamiento / Vacuna", fontWeight = FontWeight.Bold, color = RanchDarkSand) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector de Animal
                Text("Seleccionar Animal:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RanchDarkSand)
                ExposedDropdownMenuBox(
                    expanded = expandedAnimalMenu,
                    onExpandedChange = { expandedAnimalMenu = !expandedAnimalMenu }
                ) {
                    OutlinedTextField(
                        value = "${selectedAnimal.name} (Arete: ${selectedAnimal.tagNumber})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAnimalMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAnimalMenu,
                        onDismissRequest = { expandedAnimalMenu = false }
                    ) {
                        animals.forEach { animal ->
                            DropdownMenuItem(
                                text = { Text("${animal.name} - Arete ${animal.tagNumber}") },
                                onClick = {
                                    selectedAnimal = animal
                                    expandedAnimalMenu = false
                                }
                            )
                        }
                    }
                }

                // Selector de Medicamento
                Text("Seleccionar Medicamento:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RanchDarkSand)
                ExposedDropdownMenuBox(
                    expanded = expandedMedicineMenu,
                    onExpandedChange = { expandedMedicineMenu = !expandedMedicineMenu }
                ) {
                    OutlinedTextField(
                        value = "${selectedMedicine.name} (Stock: ${selectedMedicine.stock} ${selectedMedicine.dosageUnit})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMedicineMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMedicineMenu,
                        onDismissRequest = { expandedMedicineMenu = false }
                    ) {
                        medicines.forEach { medicine ->
                            DropdownMenuItem(
                                text = { Text("${medicine.name} (Stock: ${medicine.stock} ${medicine.dosageUnit})") },
                                onClick = {
                                    selectedMedicine = medicine
                                    expandedMedicineMenu = false
                                }
                            )
                        }
                    }
                }

                // Cantidad aplicada
                OutlinedTextField(
                    value = dosageStr,
                    onValueChange = { dosageStr = it },
                    label = { Text("Dosis a Aplicar (${selectedMedicine.dosageUnit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_treatment_dosage")
                )

                if (stockWarning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(RanchError.copy(alpha = 0.1f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RanchError, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Atención: La dosis ingresada es mayor al stock disponible de este medicamento.",
                            color = RanchError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha de Aplicación (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_treatment_date")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Observaciones Médicas") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_treatment_notes")
                )

                if (showError) {
                    Text("Por favor, llena los campos correctamente. La dosis debe ser un número válido.", color = RanchError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = dosageStr.toDoubleOrNull()
                    if (d == null || date.isBlank()) {
                        showError = true
                    } else {
                        onSave(
                            selectedAnimal.id,
                            selectedAnimal.name,
                            selectedMedicine.id,
                            selectedMedicine.name,
                            date,
                            d,
                            notes
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RanchAmber, contentColor = RanchDarkSand)
            ) {
                Text("Registrar Tratamiento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = RanchDarkSand)
            }
        }
    )
}
