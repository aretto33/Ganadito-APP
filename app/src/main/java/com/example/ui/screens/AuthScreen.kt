package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    isLoading: Boolean,
    authStatus: String?,
    initialIsSignUp: Boolean = false,
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: (String, String, Map<String, Any>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(initialIsSignUp) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Datos completos del productor (Restaurados)
    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var rfc by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GanaditoCrema, Color.White)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = GanaditoEspresso)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Logo Circular Mi Ganadito
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color.White, CircleShape)
                    .border(BorderStroke(1.dp, Color.LightGray), CircleShape)
                    .padding(6.dp)
                    .border(BorderStroke(2.dp, GanaditoEspresso), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_ganadito),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mi Ganadito",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = GanaditoEspresso,
                    letterSpacing = (-1).sp
                )
            )
            Text(
                text = "C O N T R O L",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = GanaditoDorado,
                    letterSpacing = 4.sp
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "DATOS DEL PRODUCTOR" else "BIENVENIDO",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 2.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    ModernTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Correo electrónico",
                        icon = Icons.Default.Email
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ModernTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Contraseña",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible }
                    )

                    if (isSignUp) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ModernTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre(s)")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        ModernTextField(value = apellidoPaterno, onValueChange = { apellidoPaterno = it }, label = "Apellido Paterno")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        ModernTextField(value = apellidoMaterno, onValueChange = { apellidoMaterno = it }, label = "Apellido Materno")
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        ModernTextField(value = rfc, onValueChange = { rfc = it }, label = "RFC")
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = GanaditoDorado)
                    } else {
                        Button(
                            onClick = {
                                if (isSignUp) {
                                    val metadata = mapOf(
                                        "nombre" to nombre,
                                        "apellido_paterno" to apellidoPaterno,
                                        "apellido_materno" to apellidoMaterno,
                                        "rfc" to rfc,
                                        "tipo_usuario" to "Productor"
                                    )
                                    onRegisterClick(email, password, metadata)
                                } else {
                                    onLoginClick(email, password)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GanaditoEspresso),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = if (isSignUp) "CREAR CUENTA" else "ENTRAR",
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        TextButton(onClick = { isSignUp = !isSignUp }) {
                            Text(
                                text = if (isSignUp) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate aquí",
                                color = GanaditoMocha,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (authStatus != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = authStatus,
                    color = if (authStatus.contains("éxito") || authStatus.contains("exit")) Color(0xFF2E7D32) else Color.Red,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 60.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 13.sp) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = GanaditoDorado, modifier = Modifier.size(20.dp)) } },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onPasswordToggle!!) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GanaditoEspresso,
            unfocusedBorderColor = GanaditoCrema,
            focusedLabelColor = GanaditoEspresso,
            unfocusedLabelColor = Color.Gray,
            cursorColor = GanaditoDorado
        ),
        singleLine = true
    )
}
