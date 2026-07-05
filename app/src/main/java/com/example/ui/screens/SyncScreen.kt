package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SyncState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    supabaseUrl: String,
    supabaseAnonKey: String,
    userEmail: String,
    userId: String,
    isOnline: Boolean,
    isOfflineOnly: Boolean,
    unsyncedCount: Int,
    syncState: SyncState,
    onSaveConfig: (url: String, key: String) -> Unit,
    onToggleOfflineOnly: (Boolean) -> Unit,
    onForceSync: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by remember { mutableStateOf(supabaseUrl) }
    var keyInput by remember { mutableStateOf(supabaseAnonKey) }
    var showSavedMessage by remember { mutableStateOf(false) }

    // Sincronizar campos al cambiar el estado externo
    LaunchedEffect(supabaseUrl, supabaseAnonKey) {
        urlInput = supabaseUrl
        keyInput = supabaseAnonKey
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RanchSoftSand)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta de estado de conectividad en tiempo real
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Centro de Sincronización",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = RanchDarkSand
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Conexión Local de Internet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) RanchGreenPrimary else RanchError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Internet Local", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RanchDarkSand)
                            Text(
                                text = if (isOnline) "Disponible (Conectado)" else "No disponible (Trabajando offline)",
                                fontSize = 12.sp,
                                color = RanchGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Conexión con Supabase
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val configured = supabaseUrl.isNotEmpty() && supabaseUrl.contains("supabase")
                        Icon(
                            imageVector = if (configured) Icons.Default.Storage else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (configured) RanchSyncBlue else RanchError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Servidor Supabase", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RanchDarkSand)
                            Text(
                                text = if (configured) "Configurado e identificado" else "Sin configurar. Ingresa credenciales abajo.",
                                fontSize = 12.sp,
                                color = RanchGray
                            )
                        }
                    }
                }
            }

            // Tarjeta de Sincronización Manual y Datos Locales Sucios
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Datos Pendientes de Sincronizar",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = RanchDarkSand
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$unsyncedCount cambios",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (unsyncedCount > 0) RanchSyncBlue else RanchGreenPrimary
                            )
                            Text(
                                text = "registros agregados o modificados localmente",
                                fontSize = 11.sp,
                                color = RanchGray
                            )
                        }

                        Button(
                            onClick = onForceSync,
                            enabled = isOnline && !isOfflineOnly && (supabaseUrl.isNotEmpty() && supabaseAnonKey.isNotEmpty()),
                            colors = ButtonDefaults.buttonColors(containerColor = RanchSyncBlue),
                            modifier = Modifier.testTag("force_sync_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sincronizar ya", fontSize = 13.sp)
                        }
                    }

                    if (syncState is SyncState.Syncing) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(color = RanchSyncBlue, modifier = Modifier.fillMaxWidth())
                    }

                    if (syncState is SyncState.Success) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "¡Sincronización completada con éxito!",
                            color = RanchGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (syncState is SyncState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            syncState.message,
                            color = RanchError,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Interruptor "Modo Solo Local"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = RanchBrown, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Forzar Modo Solo Local", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RanchDarkSand)
                        Text("Ignora internet y guarda todo localmente sin intentar conectar.", fontSize = 11.sp, color = RanchGray)
                    }
                    Switch(
                        checked = isOfflineOnly,
                        onCheckedChange = onToggleOfflineOnly,
                        modifier = Modifier.testTag("offline_only_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = RanchGreenPrimary, checkedTrackColor = RanchLightGreen)
                    )
                }
            }

            // Configuración del Servidor Supabase (URL y KEY)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Configuración de Credenciales",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = RanchDarkSand
                    )
                    Text(
                        text = "La aplicación se conectará a las tablas 'animales', 'medicamentos' y 'tratamientos' de tu proyecto Supabase.",
                        fontSize = 11.sp,
                        color = RanchGray
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Supabase URL Project") },
                        placeholder = { Text("https://xxxx.supabase.co") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("supabase_url_input")
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Supabase Anon Key API") },
                        placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("supabase_key_input")
                    )

                    Button(
                        onClick = {
                            onSaveConfig(urlInput, keyInput)
                            showSavedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RanchBrown),
                        modifier = Modifier.align(Alignment.End).testTag("save_config_button")
                    ) {
                        Text("Guardar Cambios")
                    }

                    if (showSavedMessage) {
                        Text(
                            text = "Configuración guardada correctamente.",
                            color = RanchGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Perfil de Sesión Actual / Cerrar Sesión
            if (userId.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sesión del Ganadero",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = RanchDarkSand
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Correo: $userEmail", fontSize = 13.sp, color = RanchDarkSand)
                        Text(text = "ID: $userId", fontSize = 11.sp, color = RanchGray)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = onLogout,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RanchError),
                            border = BorderStroke(1.dp, RanchError),
                            modifier = Modifier.fillMaxWidth().testTag("logout_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cerrar Sesión")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
