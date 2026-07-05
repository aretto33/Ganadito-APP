package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AnimalEntity
import com.example.data.local.entities.MedicineEntity
import com.example.data.local.entities.TreatmentEntity
import com.example.data.repository.SyncState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userEmail: String,
    isOnline: Boolean,
    unsyncedCount: Int,
    syncState: SyncState,
    animals: List<AnimalEntity>,
    medicines: List<MedicineEntity>,
    treatments: List<TreatmentEntity>,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calcular estadísticas
    val totalAnimals = animals.size
    val totalMedicines = medicines.size
    val totalTreatments = treatments.size

    val femaleCount = animals.count { it.gender.equals("Hembra", ignoreCase = true) }
    val maleCount = animals.count { it.gender.equals("Macho", ignoreCase = true) }

    val lowStockMedicines = medicines.filter { it.stock <= 5.0 }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RanchSoftSand)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Cabecera de bienvenida
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¡Hola, Ganadero!",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RanchDarkSand
                            )
                        )
                        Text(
                            text = userEmail.ifEmpty { "Modo Demostración" },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = RanchGray
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Botón para Sincronizar
                    Box {
                        FilledTonalButton(
                            onClick = onSyncClick,
                            modifier = Modifier.testTag("sync_dashboard_button"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (unsyncedCount > 0) RanchSyncBlue.copy(alpha = 0.15f) else RanchGreenSecondary.copy(alpha = 0.1f),
                                contentColor = if (unsyncedCount > 0) RanchSyncBlue else RanchGreenPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (syncState is SyncState.Syncing) Icons.Default.Sync else Icons.Default.CloudSync,
                                contentDescription = "Sync",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (syncState is SyncState.Syncing) "Sincronizando..." else "Sincronizar",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Barra informativa de estado offline/online
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isOnline) RanchGreenSecondary.copy(alpha = 0.08f)
                            else RanchAmber.copy(alpha = 0.1f)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) RanchGreenPrimary else RanchAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOnline) "Conectado a la Nube" else "Trabajando Sin Conexión (Offline)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RanchDarkSand,
                        modifier = Modifier.weight(1f)
                    )
                    if (unsyncedCount > 0) {
                        Surface(
                            color = RanchSyncBlue,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "$unsyncedCount pendientes",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Fila de Métricas principales (Tarjetas)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Animales",
                        value = totalAnimals.toString(),
                        icon = Icons.Default.Pets,
                        color = RanchGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Tratamientos",
                        value = totalTreatments.toString(),
                        icon = Icons.Default.MedicalServices,
                        color = RanchBrown,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Medicamentos",
                        value = totalMedicines.toString(),
                        icon = Icons.Default.Vaccines,
                        color = RanchAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Sincronizados",
                        value = "${totalAnimals + totalMedicines + totalTreatments - unsyncedCount}",
                        icon = Icons.Default.CloudQueue,
                        color = RanchSyncBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Alertas de Stock Bajo de Medicamentos
            if (lowStockMedicines.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = RanchError.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, RanchError.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerta",
                                tint = RanchError,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Atención: Stock Crítico",
                                    fontWeight = FontWeight.Bold,
                                    color = RanchError,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Tienes ${lowStockMedicines.size} medicamento(s) con stock menor o igual a 5.0 unidades.",
                                    color = RanchDarkSand,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Distribución de Género del Ganado (Visualización Creativa)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Distribución por Sexo",
                            fontWeight = FontWeight.Bold,
                            color = RanchDarkSand,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (totalAnimals == 0) {
                            Text(
                                text = "Registra animales para ver las estadísticas",
                                color = RanchGray,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val femaleRatio = femaleCount.toFloat() / totalAnimals
                            val maleRatio = maleCount.toFloat() / totalAnimals

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Female, contentDescription = "Hembras", tint = Color(0xFFE91E63))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hembras: $femaleCount (${(femaleRatio * 100).toInt()}%)", fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Male, contentDescription = "Machos", tint = Color(0xFF2196F3))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Machos: $maleCount (${(maleRatio * 100).toInt()}%)", fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Barra gráfica progresiva
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RanchLightGray)
                            ) {
                                if (femaleCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(femaleRatio)
                                            .background(Color(0xFFE91E63))
                                    )
                                }
                                if (maleCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(maleRatio)
                                            .background(Color(0xFF2196F3))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Historial de últimos tratamientos
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tratamientos Recientes",
                        fontWeight = FontWeight.Bold,
                        color = RanchDarkSand,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Historial completo",
                        color = RanchGreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (treatments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, contentDescription = null, tint = RanchGray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No se han registrado tratamientos aún.", color = RanchGray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(treatments.take(3)) { treatment ->
                    RecentTreatmentRow(treatment = treatment)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = RanchDarkSand
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = RanchGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecentTreatmentRow(treatment: TreatmentEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalHospital,
                contentDescription = null,
                tint = RanchBrown,
                modifier = Modifier
                    .size(36.dp)
                    .background(RanchBrown.copy(alpha = 0.1f), CircleShape)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = treatment.animalName,
                    fontWeight = FontWeight.Bold,
                    color = RanchDarkSand,
                    fontSize = 14.sp
                )
                Text(
                    text = "${treatment.medicineName} (${treatment.dosage})",
                    color = RanchGray,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = treatment.date,
                    color = RanchGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = if (treatment.syncStatus == com.example.data.local.entities.SyncStatus.SYNCED) RanchGreenSecondary.copy(alpha = 0.12f) else RanchSyncBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (treatment.syncStatus == com.example.data.local.entities.SyncStatus.SYNCED) "Nube" else "Local",
                        color = if (treatment.syncStatus == com.example.data.local.entities.SyncStatus.SYNCED) RanchGreenPrimary else RanchSyncBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
