package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.local.entities.SyncStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalsScreen(
    animals: List<AnimalEntity>,
    onAddAnimal: (name: String, tagNumber: String, type: String, breed: String, birthDate: String, weight: Double, gender: String) -> Unit,
    onUpdateAnimal: (AnimalEntity) -> Unit,
    onDeleteAnimal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("Todos") }
    var selectedBreedFilter by remember { mutableStateOf("Todos") }

    var isAddingByDialog by remember { mutableStateOf(false) }
    var editingAnimal by remember { mutableStateOf<AnimalEntity?>(null) }
    var animalToDelete by remember { mutableStateOf<AnimalEntity?>(null) }

    // Tipos de animal
    val types = listOf("Todos", "Vaca", "Toro", "Ternero", "Novilla")
    // Razas
    val breeds = listOf("Todos", "Holstein", "Jersey", "Angus", "Cebú", "Criollo", "Pardo Suizo")

    // Filtrar animales
    val filteredAnimals = animals.filter { animal ->
        val matchesSearch = animal.name.contains(searchQuery, ignoreCase = true) || 
                            animal.tagNumber.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedTypeFilter == "Todos" || animal.type.equals(selectedTypeFilter, ignoreCase = true)
        val matchesBreed = selectedBreedFilter == "Todos" || animal.breed.equals(selectedBreedFilter, ignoreCase = true)
        matchesSearch && matchesType && matchesBreed
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RanchSoftSand)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Buscador y filtros
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por nombre o arete...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RanchGreenPrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("animal_search_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RanchGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Filtrar por Categoría:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RanchDarkSand)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(types) { type ->
                            FilterChip(
                                selected = selectedTypeFilter == type,
                                onClick = { selectedTypeFilter = type },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RanchGreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Filtrar por Raza:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RanchDarkSand)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(breeds) { breed ->
                            FilterChip(
                                selected = selectedBreedFilter == breed,
                                onClick = { selectedBreedFilter = breed },
                                label = { Text(breed) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RanchBrown,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Lista de animales
            if (filteredAnimals.isEmpty()) {
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
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = RanchGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No se encontraron animales",
                            fontWeight = FontWeight.Bold,
                            color = RanchDarkSand,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Intenta cambiar los filtros o añade un nuevo animal con el botón inferior.",
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
                        .testTag("animal_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredAnimals) { animal ->
                        AnimalItemRow(
                            animal = animal,
                            onEditClick = { editingAnimal = animal },
                            onDeleteClick = { animalToDelete = animal }
                        )
                    }
                }
            }
        }

        // FAB para añadir animal
        FloatingActionButton(
            onClick = { isAddingByDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .testTag("add_animal_button"),
            containerColor = RanchGreenPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Añadir Animal")
        }

        // DIALOGS: Añadir / Editar / Eliminar
        if (isAddingByDialog) {
            AnimalFormDialog(
                title = "Registrar Nuevo Animal",
                onDismiss = { isAddingByDialog = false },
                onSave = { name, tag, type, breed, date, weight, gender ->
                    onAddAnimal(name, tag, type, breed, date, weight, gender)
                    isAddingByDialog = false
                }
            )
        }

        if (editingAnimal != null) {
            val a = editingAnimal!!
            AnimalFormDialog(
                title = "Editar Ficha de Animal",
                initialName = a.name,
                initialTag = a.tagNumber,
                initialType = a.type,
                initialBreed = a.breed,
                initialDate = a.birthDate,
                initialWeight = a.weight,
                initialGender = a.gender,
                onDismiss = { editingAnimal = null },
                onSave = { name, tag, type, breed, date, weight, gender ->
                    onUpdateAnimal(
                        a.copy(
                            name = name,
                            tagNumber = tag,
                            type = type,
                            breed = breed,
                            birthDate = date,
                            weight = weight,
                            gender = gender
                        )
                    )
                    editingAnimal = null
                }
            )
        }

        if (animalToDelete != null) {
            AlertDialog(
                onDismissRequest = { animalToDelete = null },
                title = { Text("Eliminar Registro", fontWeight = FontWeight.Bold, color = RanchDarkSand) },
                text = { Text("¿Estás seguro de que deseas eliminar a '${animalToDelete!!.name}' (arete: ${animalToDelete!!.tagNumber})? Si estás en línea se eliminará también de Supabase.", color = RanchDarkSand) },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteAnimal(animalToDelete!!.id)
                            animalToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RanchError)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { animalToDelete = null }) {
                        Text("Cancelar", color = RanchDarkSand)
                    }
                }
            )
        }
    }
}

@Composable
fun AnimalItemRow(
    animal: AnimalEntity,
    onEditClick: () -> Unit,
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
            // Icono según tipo de animal
            Icon(
                imageVector = if (animal.gender.equals("Macho", ignoreCase = true)) Icons.Default.Male else Icons.Default.Female,
                contentDescription = null,
                tint = if (animal.gender.equals("Macho", ignoreCase = true)) Color(0xFF2196F3) else Color(0xFFE91E63),
                modifier = Modifier
                    .size(44.dp)
                    .background(RanchLightGray, CircleShape)
                    .padding(10.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = animal.name,
                        fontWeight = FontWeight.Bold,
                        color = RanchDarkSand,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = RanchBrown.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Arete: ${animal.tagNumber}",
                            color = RanchBrown,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${animal.type} • ${animal.breed}",
                    color = RanchGray,
                    fontSize = 13.sp
                )
                Text(
                    text = "Peso: ${animal.weight} kg • Nacido: ${animal.birthDate}",
                    color = RanchDarkSand.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }

            // Sync Status Icon y acciones
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Estado de Sincronización
                val (icon, tint, tooltip) = when (animal.syncStatus) {
                    SyncStatus.SYNCED -> Triple(Icons.Default.CloudQueue, RanchGreenPrimary, "Sincronizado")
                    SyncStatus.PENDING_INSERT -> Triple(Icons.Default.CloudSync, RanchSyncBlue, "Pendiente de subir")
                    SyncStatus.PENDING_UPDATE -> Triple(Icons.Default.CloudUpload, RanchAmber, "Actualización pendiente")
                    SyncStatus.PENDING_DELETE -> Triple(Icons.Default.DeleteForever, RanchError, "Por borrar")
                }
                Icon(
                    imageVector = icon,
                    contentDescription = tooltip,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = RanchGreenPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = RanchError, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalFormDialog(
    title: String,
    initialName: String = "",
    initialTag: String = "",
    initialType: String = "Vaca",
    initialBreed: String = "Angus",
    initialDate: String = "2024-01-01",
    initialWeight: Double = 150.0,
    initialGender: String = "Hembra",
    onDismiss: () -> Unit,
    onSave: (name: String, tagNumber: String, type: String, breed: String, birthDate: String, weight: Double, gender: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var tag by remember { mutableStateOf(initialTag) }
    var type by remember { mutableStateOf(initialType) }
    var breed by remember { mutableStateOf(initialBreed) }
    var birthDate by remember { mutableStateOf(initialDate) }
    var weightStr by remember { mutableStateOf(initialWeight.toString()) }
    var gender by remember { mutableStateOf(initialGender) }

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
                    label = { Text("Nombre / Alias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_animal_name")
                )

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Número de Arete (ID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_animal_tag")
                )

                // Selección de Tipo de Animal
                Text("Categoría del Animal:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RanchDarkSand)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Vaca", "Toro", "Novilla", "Ternero").forEach { t ->
                        val isSel = type == t
                        Surface(
                            onClick = { type = t },
                            color = if (isSel) RanchGreenPrimary else Color.White,
                            border = BorderStroke(1.dp, if (isSel) RanchGreenPrimary else RanchGray),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                t,
                                color = if (isSel) Color.White else RanchDarkSand,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Selección de Género
                Text("Género:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RanchDarkSand)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Hembra", "Macho").forEach { g ->
                        val isSel = gender == g
                        Surface(
                            onClick = { gender = g },
                            color = if (isSel) (if (g == "Hembra") Color(0xFFE91E63) else Color(0xFF2196F3)) else Color.White,
                            border = BorderStroke(1.dp, if (isSel) Color.Transparent else RanchGray),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                g,
                                color = if (isSel) Color.White else RanchDarkSand,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Selección de Raza
                Text("Raza:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RanchDarkSand)
                var expandedBreedMenu by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedBreedMenu,
                    onExpandedChange = { expandedBreedMenu = !expandedBreedMenu }
                ) {
                    OutlinedTextField(
                        value = breed,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBreedMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBreedMenu,
                        onDismissRequest = { expandedBreedMenu = false }
                    ) {
                        listOf("Holstein", "Jersey", "Angus", "Cebú", "Criollo", "Pardo Suizo", "Charolais", "Simmental").forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b) },
                                onClick = {
                                    breed = b
                                    expandedBreedMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Fecha de Nacimiento (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_animal_date")
                )

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Peso aproximado (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_animal_weight")
                )

                if (showError) {
                    Text("Por favor, llena los campos correctamente. El peso debe ser numérico.", color = RanchError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightStr.toDoubleOrNull()
                    if (name.isBlank() || tag.isBlank() || w == null || birthDate.isBlank()) {
                        showError = true
                    } else {
                        onSave(name, tag, type, breed, birthDate, w, gender)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RanchGreenPrimary)
            ) {
                Text("Guardar Ficha")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = RanchDarkSand)
            }
        }
    )
}
