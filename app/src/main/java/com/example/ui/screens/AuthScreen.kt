package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RanchBrown
import com.example.ui.theme.RanchGreenPrimary
import com.example.ui.theme.RanchGreenSecondary
import com.example.ui.theme.RanchSoftSand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    isOnline: Boolean,
    isLoading: Boolean,
    authStatus: String?,
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: (String, String) -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RanchGreenPrimary.copy(alpha = 0.15f),
                        RanchSoftSand
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Icono de Ganadería principal
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = "Ganadería",
                tint = RanchGreenPrimary,
                modifier = Modifier
                    .size(72.dp)
                    .background(RanchGreenSecondary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ganadería Sincro",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = RanchBrown
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Control ganadero offline-first con sincronización automática",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = RanchBrown.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Crear Nueva Cuenta" else "Iniciar Sesión",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = RanchGreenPrimary
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = RanchGreenPrimary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RanchGreenPrimary,
                            focusedLabelColor = RanchGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RanchGreenPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RanchGreenPrimary,
                            focusedLabelColor = RanchGreenPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = RanchGreenPrimary)
                    } else {
                        Button(
                            onClick = {
                                onLoginClick(email, password)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RanchGreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isSignUp) "Registrarse" else "Ingresar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { isSignUp = !isSignUp },
                            modifier = Modifier.testTag("toggle_auth_button")
                        ) {
                            Text(
                                text = if (isSignUp) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate gratis",
                                color = RanchGreenPrimary
                            )
                        }
                    }

                    if (authStatus != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = authStatus,
                            color = if (authStatus.contains("éxito", true) || authStatus.contains("Iniciada", true)) RanchGreenPrimary else MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Conectividad & Estado offline descriptivo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RanchGreenSecondary.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudQueue else Icons.Default.OfflinePin,
                        contentDescription = "Estado",
                        tint = if (isOnline) RanchGreenPrimary else RanchBrown,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isOnline) "Modo En Línea Detectado" else "Trabajando en Modo Fuera de Línea",
                            fontWeight = FontWeight.Bold,
                            color = RanchBrown,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isOnline) "Tus datos se sincronizarán inmediatamente con la nube de Supabase." else "Puedes registrar animales y tratamientos offline. Al detectar conexión se sincronizarán solos.",
                            color = RanchBrown.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón flotante/secundario para ver configuración de Supabase
            OutlinedButton(
                onClick = onConfigureClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RanchBrown),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("configure_supabase_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configurar Servidor / Supabase", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
