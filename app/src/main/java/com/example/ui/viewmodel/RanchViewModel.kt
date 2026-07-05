package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AnimalEntity
import com.example.data.local.entities.MedicineEntity
import com.example.data.local.entities.TreatmentEntity
import com.example.data.remote.SupabaseConfigManager
import com.example.data.remote.SupabaseHttpClient
import com.example.data.repository.RanchRepository
import com.example.data.repository.SyncState
import com.example.utils.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RanchViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val configManager = SupabaseConfigManager(application)
    private val supabaseClient = SupabaseHttpClient(configManager)
    
    val repository = RanchRepository(
        database.animalDao(),
        database.medicineDao(),
        database.treatmentDao(),
        supabaseClient
    )

    private val networkMonitor = NetworkMonitor(application)

    // --- ESTADOS DE CONFIGURACIÓN Y RED ---
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val supabaseUrl: StateFlow<String> = configManager.supabaseUrl
    val supabaseAnonKey: StateFlow<String> = configManager.supabaseAnonKey
    val userId: StateFlow<String> = configManager.userId
    val userEmail: StateFlow<String> = configManager.userEmail
    val isOfflineOnly: StateFlow<Boolean> = configManager.isOfflineOnly
    val syncState: StateFlow<SyncState> = repository.syncState

    val isLoggedIn: StateFlow<Boolean> = configManager.userId
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), configManager.isLoggedIn())

    // --- OPERACIONES DE FLUJO REACTIVO ---
    
    val animals: StateFlow<List<AnimalEntity>> = userId
        .flatMapLatest { uid ->
            if (uid.isEmpty()) flowOf(emptyList())
            else repository.observeAnimals(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicines: StateFlow<List<MedicineEntity>> = userId
        .flatMapLatest { uid ->
            if (uid.isEmpty()) flowOf(emptyList())
            else repository.observeMedicines(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val treatments: StateFlow<List<TreatmentEntity>> = userId
        .flatMapLatest { uid ->
            if (uid.isEmpty()) flowOf(emptyList())
            else repository.observeTreatments(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CONTADORES PENDIENTES DE SYNC ---
    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    // --- ESTADOS DE UI ---
    private val _authStatus = MutableStateFlow<String?>(null)
    val authStatus: StateFlow<String?> = _authStatus.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    init {
        // Observar conexión para Sincronización Automática
        viewModelScope.launch {
            isOnline.collect { online ->
                Log.d("RanchViewModel", "Connection change: online=$online")
                if (online && isLoggedIn.value && !isOfflineOnly.value && configManager.isConfigured()) {
                    Log.d("RanchViewModel", "Online connection detected! Triggering automatic sync...")
                    repository.syncAll()
                }
                updateUnsyncedCount()
            }
        }

        // Observar cambios en las tablas para actualizar el contador de cambios locales pendientes
        viewModelScope.launch {
            combine(animals, medicines, treatments) { a, m, t ->
                updateUnsyncedCount()
            }.collect()
        }
    }

    private suspend fun updateUnsyncedCount() {
        val pendingAnimals = database.animalDao().getUnsynced().size
        val pendingMeds = database.medicineDao().getUnsynced().size
        val pendingTreats = database.treatmentDao().getUnsynced().size
        _unsyncedCount.value = pendingAnimals + pendingMeds + pendingTreats
    }

    // --- AUTENTICACIÓN ---

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authStatus.value = "Por favor, completa todos los campos."
            return
        }
        _isAuthLoading.value = true
        _authStatus.value = null
        viewModelScope.launch {
            if (isOnline.value && !isOfflineOnly.value && configManager.isConfigured()) {
                val response = supabaseClient.signIn(email, pass)
                _isAuthLoading.value = false
                if (response.isSuccess) {
                    configManager.saveSession(response.userId, response.email, response.token)
                    _authStatus.value = "Sesión iniciada con éxito (Nube)"
                    // Descargar datos remotos
                    repository.downloadRemoteData()
                } else {
                    _authStatus.value = response.errorMessage
                }
            } else {
                // Modo Offline: Permitir login con cualquier credencial si ya había una guardada localmente,
                // o iniciar sesión de demostración offline para que puedan probar la app sin internet.
                _isAuthLoading.value = false
                val savedUid = configManager.userId.value
                val savedEmail = configManager.userEmail.value
                if (savedUid.isNotEmpty() && email == savedEmail) {
                    _authStatus.value = "Sesión iniciada (Modo Offline)"
                } else {
                    // Generar sesión de demostración local
                    val demoUid = "demo-user-offline-uuid-123456"
                    configManager.saveSession(demoUid, email, "demo-token")
                    _authStatus.value = "Iniciada Sesión de Demostración Offline (Los datos se guardarán localmente)"
                }
            }
            updateUnsyncedCount()
        }
    }

    fun register(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authStatus.value = "Por favor, completa todos los campos."
            return
        }
        _isAuthLoading.value = true
        _authStatus.value = null
        viewModelScope.launch {
            if (isOnline.value && !isOfflineOnly.value && configManager.isConfigured()) {
                val response = supabaseClient.signUp(email, pass)
                _isAuthLoading.value = false
                if (response.isSuccess) {
                    configManager.saveSession(response.userId, response.email, response.token)
                    _authStatus.value = "Usuario registrado e iniciado con éxito (Nube)"
                } else {
                    _authStatus.value = response.errorMessage
                }
            } else {
                _isAuthLoading.value = false
                _authStatus.value = "Requiere conexión a internet para registrar un nuevo usuario en la nube."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            configManager.clearSession()
            _authStatus.value = "Sesión cerrada"
        }
    }

    // --- CONFIGURACIÓN DE SUPABASE ---

    fun saveSupabaseConfig(url: String, key: String) {
        configManager.updateConfig(url, key)
        viewModelScope.launch {
            if (isOnline.value && isLoggedIn.value && !isOfflineOnly.value) {
                repository.syncAll()
            }
        }
    }

    fun toggleOfflineOnly(offline: Boolean) {
        configManager.setOfflineOnly(offline)
        if (!offline && isOnline.value && isLoggedIn.value) {
            viewModelScope.launch {
                repository.syncAll()
            }
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            repository.syncAll()
            updateUnsyncedCount()
        }
    }

    // --- GUARDAR ENTIDADES ---

    fun addAnimal(name: String, tagNumber: String, type: String, breed: String, birthDate: String, weight: Double, gender: String) {
        viewModelScope.launch {
            repository.addAnimal(name, tagNumber, type, breed, birthDate, weight, gender, userId.value)
            updateUnsyncedCount()
            triggerSyncIfNeeded()
        }
    }

    fun updateAnimal(animal: AnimalEntity) {
        viewModelScope.launch {
            repository.updateAnimal(animal)
            updateUnsyncedCount()
            triggerSyncIfNeeded()
        }
    }

    fun deleteAnimal(id: String) {
        viewModelScope.launch {
            repository.deleteAnimal(id)
            updateUnsyncedCount()
            triggerSyncIfNeeded()
        }
    }

    fun addMedicine(name: String, activeIngredient: String, stock: Double, dosageUnit: String) {
        viewModelScope.launch {
            repository.addMedicine(name, activeIngredient, stock, dosageUnit, userId.value)
            updateUnsyncedCount()
            triggerSyncIfNeeded()
        }
    }

    fun updateMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            repository.updateMedicine(medicine)
            updateUnsyncedCount()
            triggerSyncIfNeeded()
        }
    }

    fun deleteMedicine(id: String) {
        viewModelScope.launch {
            repository.deleteMedicine(id)
            updateUnsyncedCount()
            triggerSyncIfNeeded()
        }
    }

    fun addTreatment(animalId: String, animalName: String, medicineId: String, medicineName: String, date: String, dosage: Double, notes: String) {
        viewModelScope.launch {
            repository.addTreatment(animalId, animalName, medicineId, medicineName, date, dosage, notes, userId.value)
            updateUnsyncedCount()
            triggerSyncIfNeeded()
        }
    }

    fun deleteTreatment(id: String) {
        viewModelScope.launch {
            repository.deleteTreatment(id)
            updateUnsyncedCount()
            triggerSyncIfNeeded()
        }
    }

    private fun triggerSyncIfNeeded() {
        if (isOnline.value && !isOfflineOnly.value && configManager.isConfigured() && isLoggedIn.value) {
            viewModelScope.launch {
                repository.syncAll()
            }
        }
    }
}
