package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.SyncState
import com.example.ui.screens.AnimalsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MedicinesScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.TreatmentsScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.RanchViewModel

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppEntry()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppEntry(
    viewModel: RanchViewModel = viewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    val animals by viewModel.animals.collectAsState()
    val medicines by viewModel.medicines.collectAsState()
    val treatments by viewModel.treatments.collectAsState()

    val supabaseUrl by viewModel.supabaseUrl.collectAsState()
    val supabaseAnonKey by viewModel.supabaseAnonKey.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isOfflineOnly by viewModel.isOfflineOnly.collectAsState()
    val authStatus by viewModel.authStatus.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()

    var currentTab by remember { mutableStateOf("dashboard") }
    var authSubScreen by remember { mutableStateOf("welcome") } // welcome, login, register

    if (!isLoggedIn) {
        when (authSubScreen) {
            "welcome" -> {
                WelcomeScreen(
                    onLoginClick = { authSubScreen = "login" },
                    onRegisterClick = { authSubScreen = "register" }
                )
            }
            else -> {
                AuthScreen(
                    isLoading = isAuthLoading,
                    authStatus = authStatus,
                    initialIsSignUp = authSubScreen == "register",
                    onLoginClick = { email, pass -> viewModel.login(email, pass) },
                    onRegisterClick = { email, pass, metadata -> viewModel.register(email, pass, metadata) },
                    onBack = { authSubScreen = "welcome" }
                )
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_ganadito),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "Mi Ganadito Control",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    },
                    actions = {
                        // Indicador de conexión arriba a la derecha
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isOnline) RanchGreenPrimary.copy(alpha = 0.12f) else RanchAmber.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) RanchGreenPrimary else RanchAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "Nube Activa" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) RanchGreenPrimary else RanchBrown
                            )
                            if (unsyncedCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(RanchSyncBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unsyncedCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GanaditoDarkBrown,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = GanaditoSurface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentTab == "dashboard",
                        onClick = { currentTab = "dashboard" },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Panel") },
                        label = { Text("Panel", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GanaditoPrimaryBrown,
                            selectedTextColor = GanaditoPrimaryBrown,
                            indicatorColor = GanaditoBackground
                        ),
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == "animales",
                        onClick = { currentTab = "animales" },
                        icon = { Icon(Icons.Default.Pets, contentDescription = "Ganado") },
                        label = { Text("Ganado", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GanaditoPrimaryBrown,
                            selectedTextColor = GanaditoPrimaryBrown,
                            indicatorColor = GanaditoBackground
                        ),
                        modifier = Modifier.testTag("nav_animals")
                    )
                    NavigationBarItem(
                        selected = currentTab == "medicamentos",
                        onClick = { currentTab = "medicamentos" },
                        icon = { Icon(Icons.Default.Vaccines, contentDescription = "Medicina") },
                        label = { Text("Medicinas", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GanaditoPrimaryBrown,
                            selectedTextColor = GanaditoPrimaryBrown,
                            indicatorColor = GanaditoBackground
                        ),
                        modifier = Modifier.testTag("nav_medicines")
                    )
                    NavigationBarItem(
                        selected = currentTab == "tratamientos",
                        onClick = { currentTab = "tratamientos" },
                        icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Tratamientos") },
                        label = { Text("Médico", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GanaditoPrimaryBrown,
                            selectedTextColor = GanaditoPrimaryBrown,
                            indicatorColor = GanaditoBackground
                        ),
                        modifier = Modifier.testTag("nav_treatments")
                    )
                    NavigationBarItem(
                        selected = currentTab == "sync",
                        onClick = { currentTab = "sync" },
                        icon = { 
                            BadgedBox(
                                badge = {
                                    if (unsyncedCount > 0) {
                                        Badge { Text(unsyncedCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Configuración")
                            }
                        },
                        label = { Text("Sincro", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GanaditoPrimaryBrown,
                            selectedTextColor = GanaditoPrimaryBrown,
                            indicatorColor = GanaditoBackground
                        ),
                        modifier = Modifier.testTag("nav_sync")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    "dashboard" -> {
                        DashboardScreen(
                            userEmail = userEmail,
                            isOnline = isOnline,
                            unsyncedCount = unsyncedCount,
                            syncState = syncState,
                            animals = animals,
                            medicines = medicines,
                            treatments = treatments,
                            onSyncClick = { viewModel.forceSync() }
                        )
                    }
                    "animales" -> {
                        AnimalsScreen(
                            animals = animals,
                            onAddAnimal = { name, tag, type, breed, date, weight, gender ->
                                viewModel.addAnimal(name, tag, type, breed, date, weight, gender)
                            },
                            onUpdateAnimal = { animal -> viewModel.updateAnimal(animal) },
                            onDeleteAnimal = { id -> viewModel.deleteAnimal(id) }
                        )
                    }
                    "medicamentos" -> {
                        MedicinesScreen(
                            medicines = medicines,
                            onAddMedicine = { name, ing, stock, unit ->
                                viewModel.addMedicine(name, ing, stock, unit)
                            },
                            onUpdateMedicine = { med -> viewModel.updateMedicine(med) },
                            onDeleteMedicine = { id -> viewModel.deleteMedicine(id) }
                        )
                    }
                    "tratamientos" -> {
                        TreatmentsScreen(
                            treatments = treatments,
                            animals = animals,
                            medicines = medicines,
                            onAddTreatment = { animalId, animalName, medId, medName, date, dosage, notes ->
                                viewModel.addTreatment(animalId, animalName, medId, medName, date, dosage, notes)
                            },
                            onDeleteTreatment = { id -> viewModel.deleteTreatment(id) }
                        )
                    }
                    "sync" -> {
                        SyncScreen(
                            supabaseUrl = supabaseUrl,
                            supabaseAnonKey = supabaseAnonKey,
                            userEmail = userEmail,
                            userId = userId,
                            isOnline = isOnline,
                            isOfflineOnly = isOfflineOnly,
                            unsyncedCount = unsyncedCount,
                            syncState = syncState,
                            onSaveConfig = { url, key -> viewModel.saveSupabaseConfig(url, key) },
                            onToggleOfflineOnly = { offline -> viewModel.toggleOfflineOnly(offline) },
                            onForceSync = { viewModel.forceSync() },
                            onLogout = { viewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}
