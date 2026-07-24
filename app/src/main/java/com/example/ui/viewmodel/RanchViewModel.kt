package com.example.ui.viewmodel

import android.app.Application
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
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

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val userId: StateFlow<String> = configManager.userId
    val userEmail: StateFlow<String> = configManager.userEmail
    val pkProductor: StateFlow<Int> = configManager.pkProductor
    val idUsuario: StateFlow<Int> = configManager.idUsuario
    val supabaseUrl: StateFlow<String> = configManager.supabaseUrl
    val supabaseAnonKey: StateFlow<String> = configManager.supabaseAnonKey
    val isOfflineOnly: StateFlow<Boolean> = configManager.isOfflineOnly
    val syncState: StateFlow<SyncState> = repository.syncState

    val isLoggedIn: StateFlow<Boolean> = configManager.userId
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), configManager.isLoggedIn())

    val animals: StateFlow<List<AnimalEntity>> = userId.flatMapLatest { uid ->
        if (uid.isEmpty()) flowOf(emptyList()) else repository.observeAnimals(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicines: StateFlow<List<MedicineEntity>> = userId.flatMapLatest { uid ->
        if (uid.isEmpty()) flowOf(emptyList()) else repository.observeMedicines(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val treatments: StateFlow<List<TreatmentEntity>> = userId.flatMapLatest { uid ->
        if (uid.isEmpty()) flowOf(emptyList()) else repository.observeTreatments(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _unsyncedCount = MutableStateFlow(0)
    val unsyncedCount: StateFlow<Int> = _unsyncedCount.asStateFlow()

    private val _authStatus = MutableStateFlow<String?>(null)
    val authStatus: StateFlow<String?> = _authStatus.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    init {
        viewModelScope.launch {
            combine(animals, medicines, treatments) { _, _, _ -> }.collect { updateUnsyncedCount() }
        }
    }

    private suspend fun updateUnsyncedCount() {
        val uid = userId.value
        if (uid.isEmpty()) { _unsyncedCount.value = 0; return }
        val pA = database.animalDao().getUnsynced(uid).size
        val pM = database.medicineDao().getUnsynced(uid).size
        val pT = database.treatmentDao().getUnsynced(uid).size
        _unsyncedCount.value = pA + pM + pT
    }

    fun login(email: String, pass: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || pass.isBlank()) { 
            _authStatus.value = "Escribe correo y contraseña."
            return 
        }
        _isAuthLoading.value = true
        _authStatus.value = null
        
        viewModelScope.launch {
            try {
                val response = supabaseClient.signIn(cleanEmail, pass)
                if (response.isSuccess) {
                    // ENTRADA INMEDIATA: Guardamos lo básico para que la app cambie de pantalla
                    configManager.saveSession(response.userId, response.email, response.token)
                    _isAuthLoading.value = false
                    _authStatus.value = "Sesión iniciada."
                    
                    // El resto de los datos (IDs de DB) se buscan en segundo plano
                    launch {
                        val ids = supabaseClient.getUserDatabaseIds(response.userId)
                        configManager.saveSession(response.userId, response.email, response.token, ids.first, ids.second)
                        repository.downloadRemoteData(ids.second)
                        updateUnsyncedCount()
                    }
                } else {
                    _isAuthLoading.value = false
                    _authStatus.value = "Error: ${response.errorMessage ?: "Credenciales inválidas"}"
                }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                _authStatus.value = "Fallo técnico: ${e.localizedMessage}"
            }
        }
    }

    fun register(email: String, pass: String, metadata: Map<String, Any> = emptyMap()) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || pass.isBlank()) { _authStatus.value = "Completa los campos."; return }
        _isAuthLoading.value = true
        _authStatus.value = null
        
        viewModelScope.launch {
            try {
                val response = supabaseClient.signUp(cleanEmail, pass, metadata)
                if (response.isSuccess) {
                    val res = supabaseClient.createUserProfile(response.userId, response.email, metadata, response.token)
                    configManager.saveSession(response.userId, response.email, response.token, res.idUsuario, res.pkProductor)
                    _isAuthLoading.value = false
                    _authStatus.value = "Registro exitoso. ¡Bienvenido!"
                } else {
                    _isAuthLoading.value = false
                    _authStatus.value = "Error: ${response.errorMessage}"
                }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                _authStatus.value = "Fallo registro: ${e.localizedMessage}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch { configManager.clearSession(); _authStatus.value = "Sesión cerrada" }
    }

    fun forceSync() {
        viewModelScope.launch { repository.syncAll(userId.value, pkProductor.value); updateUnsyncedCount() }
    }

    fun addAnimal(n: String, t: String, ty: String, b: String, d: String, w: Double, g: String) {
        viewModelScope.launch { 
            repository.addAnimal(n, t, ty, b, d, w, g, userId.value)
            updateUnsyncedCount()
            repository.syncAll(userId.value, pkProductor.value)
        }
    }
    
    fun updateAnimal(a: AnimalEntity) { viewModelScope.launch { repository.updateAnimal(a); updateUnsyncedCount(); repository.syncAll(userId.value, pkProductor.value) } }
    fun deleteAnimal(id: String) { viewModelScope.launch { repository.deleteAnimal(id); updateUnsyncedCount(); repository.syncAll(userId.value, pkProductor.value) } }
    fun addMedicine(n: String, i: String, s: Double, u: String) { viewModelScope.launch { repository.addMedicine(n, i, s, u, userId.value); updateUnsyncedCount(); repository.syncAll(userId.value, pkProductor.value) } }
    fun updateMedicine(m: MedicineEntity) { viewModelScope.launch { repository.updateMedicine(m); updateUnsyncedCount(); repository.syncAll(userId.value, pkProductor.value) } }
    fun deleteMedicine(id: String) { viewModelScope.launch { repository.deleteMedicine(id); updateUnsyncedCount(); repository.syncAll(userId.value, pkProductor.value) } }
    fun addTreatment(aI: String, aN: String, mI: String, mN: String, d: String, ds: Double, notes: String) {
        viewModelScope.launch { 
            repository.addTreatment(aI, aN, mI, mN, d, ds, notes, userId.value)
            updateUnsyncedCount()
            repository.syncAll(userId.value, pkProductor.value)
        }
    }
    fun deleteTreatment(id: String) { viewModelScope.launch { repository.deleteTreatment(id); updateUnsyncedCount(); repository.syncAll(userId.value, pkProductor.value) } }
    fun saveSupabaseConfig(u: String, k: String) { /* Hardcoded */ }
    fun toggleOfflineOnly(o: Boolean) { /* Hardcoded */ }
}
