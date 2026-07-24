package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Box(
        modifier = Modifier
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo con el mismo estilo de doble anillo de la pantalla de Auth
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(Color.White, CircleShape)
                    .border(BorderStroke(1.dp, Color.LightGray), CircleShape)
                    .padding(8.dp)
                    .border(BorderStroke(2.dp, GanaditoEspresso), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_ganadito),
                    contentDescription = "Logo Mi Ganadito Control",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Branding Typography
            Text(
                text = "Mi Ganadito",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = GanaditoEspresso,
                    letterSpacing = (-1.5).sp
                )
            )
            Text(
                text = "C O N T R O L",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GanaditoDorado,
                    letterSpacing = 6.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SISTEMA INTEGRAL DE GESTIÓN GANADERA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GanaditoMocha.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Botón INICIAR (Espresso Solid)
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GanaditoEspresso),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "INICIAR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón REGISTRARSE (Dorado Outline)
            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                border = BorderStroke(2.dp, GanaditoDorado),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GanaditoEspresso)
            ) {
                Text(
                    text = "REGISTRARSE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
