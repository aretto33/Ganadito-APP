package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.AnimalDao
import com.example.data.local.dao.MedicineDao
import com.example.data.local.dao.TreatmentDao
import com.example.data.local.entities.AnimalEntity
import com.example.data.local.entities.MedicineEntity
import com.example.data.local.entities.TreatmentEntity
import com.example.data.local.entities.SyncStatus
import com.example.data.remote.SupabaseHttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

class RanchRepository(
    private val animalDao: AnimalDao,
    private val medicineDao: MedicineDao,
    private val treatmentDao: TreatmentDao,
    private val supabaseClient: SupabaseHttpClient
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // --- OBSERVADORES REACTIVOS ---

    fun observeAnimals(userId: String): Flow<List<AnimalEntity>> = animalDao.observeAll(userId)
    fun observeMedicines(userId: String): Flow<List<MedicineEntity>> = medicineDao.observeAll(userId)
    fun observeTreatments(userId: String): Flow<List<TreatmentEntity>> = treatmentDao.observeAll(userId)

    // --- ACCIONES - ANIMALES ---

    suspend fun addAnimal(
        name: String,
        tagNumber: String,
        type: String,
        breed: String,
        birthDate: String,
        weight: Double,
        gender: String,
        userId: String
    ) {
        val animal = AnimalEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            tagNumber = tagNumber,
            type = type,
            breed = breed,
            birthDate = birthDate,
            weight = weight,
            gender = gender,
            userId = userId,
            syncStatus = SyncStatus.PENDING_INSERT
        )
        animalDao.insert(animal)
        Log.d("RanchRepository", "Animal added locally: ${animal.id}")
    }

    suspend fun updateAnimal(animal: AnimalEntity) {
        val updated = animal.copy(
            syncStatus = if (animal.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE,
            lastUpdated = System.currentTimeMillis()
        )
        animalDao.update(updated)
    }

    suspend fun deleteAnimal(id: String) {
        val local = animalDao.getById(id) ?: return
        if (local.syncStatus == SyncStatus.PENDING_INSERT) {
            // Nunca se subió, eliminar directamente
            animalDao.deleteLocally(id)
        } else {
            // Marcar para eliminación remota posterior
            animalDao.markForDeletion(id)
        }
    }

    // --- ACCIONES - MEDICAMENTOS ---

    suspend fun addMedicine(
        name: String,
        activeIngredient: String,
        stock: Double,
        dosageUnit: String,
        userId: String
    ) {
        val medicine = MedicineEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            activeIngredient = activeIngredient,
            stock = stock,
            dosageUnit = dosageUnit,
            userId = userId,
            syncStatus = SyncStatus.PENDING_INSERT
        )
        medicineDao.insert(medicine)
    }

    suspend fun updateMedicine(medicine: MedicineEntity) {
        val updated = medicine.copy(
            syncStatus = if (medicine.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE,
            lastUpdated = System.currentTimeMillis()
        )
        medicineDao.update(updated)
    }

    suspend fun deleteMedicine(id: String) {
        val local = medicineDao.getById(id) ?: return
        if (local.syncStatus == SyncStatus.PENDING_INSERT) {
            medicineDao.deleteLocally(id)
        } else {
            medicineDao.markForDeletion(id)
        }
    }

    // --- ACCIONES - TRATAMIENTOS ---

    suspend fun addTreatment(
        animalId: String,
        animalName: String,
        medicineId: String,
        medicineName: String,
        date: String,
        dosage: Double,
        notes: String,
        userId: String
    ) {
        // 1. Descontar stock del medicamento localmente
        val localMedicine = medicineDao.getById(medicineId)
        if (localMedicine != null) {
            val newStock = (localMedicine.stock - dosage).coerceAtLeast(0.0)
            val updatedMedicine = localMedicine.copy(
                stock = newStock,
                syncStatus = if (localMedicine.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE,
                lastUpdated = System.currentTimeMillis()
            )
            medicineDao.update(updatedMedicine)
        }

        // 2. Registrar el tratamiento
        val treatment = TreatmentEntity(
            id = UUID.randomUUID().toString(),
            animalId = animalId,
            animalName = animalName,
            medicineId = medicineId,
            medicineName = medicineName,
            date = date,
            dosage = dosage,
            notes = notes,
            userId = userId,
            syncStatus = SyncStatus.PENDING_INSERT
        )
        treatmentDao.insert(treatment)
    }

    suspend fun deleteTreatment(id: String) {
        val local = treatmentDao.getById(id) ?: return
        if (local.syncStatus == SyncStatus.PENDING_INSERT) {
            treatmentDao.deleteLocally(id)
        } else {
            treatmentDao.markForDeletion(id)
        }
    }

    // --- ENGINE DE SINCRONIZACIÓN AUTOMÁTICA ---

    suspend fun syncAll(): Boolean {
        if (_syncState.value == SyncState.Syncing) return false
        _syncState.value = SyncState.Syncing
        Log.d("RanchRepository", "Sync started...")

        try {
            // Verificar conexión de red primero
            if (!supabaseClient.testConnection()) {
                _syncState.value = SyncState.Error("No se puede establecer conexión con Supabase.")
                return false
            }

            var success = true

            // 1. Sincronizar Animales
            val unsyncedAnimals = animalDao.getUnsynced()
            Log.d("RanchRepository", "Unsynced animals found: ${unsyncedAnimals.size}")
            for (animal in unsyncedAnimals) {
                if (animal.syncStatus == SyncStatus.PENDING_DELETE) {
                    if (supabaseClient.deleteAnimal(animal.id)) {
                        animalDao.deleteLocally(animal.id)
                    } else {
                        success = false
                    }
                } else {
                    if (supabaseClient.pushAnimal(animal)) {
                        animalDao.insert(animal.copy(syncStatus = SyncStatus.SYNCED))
                    } else {
                        success = false
                    }
                }
            }

            // 2. Sincronizar Medicamentos
            val unsyncedMedicines = medicineDao.getUnsynced()
            Log.d("RanchRepository", "Unsynced medicines found: ${unsyncedMedicines.size}")
            for (medicine in unsyncedMedicines) {
                if (medicine.syncStatus == SyncStatus.PENDING_DELETE) {
                    if (supabaseClient.deleteMedicine(medicine.id)) {
                        medicineDao.deleteLocally(medicine.id)
                    } else {
                        success = false
                    }
                } else {
                    if (supabaseClient.pushMedicine(medicine)) {
                        medicineDao.insert(medicine.copy(syncStatus = SyncStatus.SYNCED))
                    } else {
                        success = false
                    }
                }
            }

            // 3. Sincronizar Tratamientos
            val unsyncedTreatments = treatmentDao.getUnsynced()
            Log.d("RanchRepository", "Unsynced treatments found: ${unsyncedTreatments.size}")
            for (treatment in unsyncedTreatments) {
                if (treatment.syncStatus == SyncStatus.PENDING_DELETE) {
                    if (supabaseClient.deleteTreatment(treatment.id)) {
                        treatmentDao.deleteLocally(treatment.id)
                    } else {
                        success = false
                    }
                } else {
                    if (supabaseClient.pushTreatment(treatment)) {
                        treatmentDao.insert(treatment.copy(syncStatus = SyncStatus.SYNCED))
                    } else {
                        success = false
                    }
                }
            }

            if (success) {
                _syncState.value = SyncState.Success
                Log.d("RanchRepository", "Sync successfully completed!")
                return true
            } else {
                _syncState.value = SyncState.Error("Algunos datos no pudieron sincronizarse.")
                return false
            }

        } catch (e: Exception) {
            Log.e("RanchRepository", "Sync failed with exception", e)
            _syncState.value = SyncState.Error("Error en sincronización: ${e.localizedMessage}")
            return false
        }
    }

    // --- ACCIÓN AL INICIAR SESIÓN: TRAER TODO DE LA NUBE ---

    suspend fun downloadRemoteData() {
        _syncState.value = SyncState.Syncing
        try {
            val animals = supabaseClient.fetchRemoteAnimals()
            for (animal in animals) {
                animalDao.insert(animal)
            }

            val medicines = supabaseClient.fetchRemoteMedicines()
            for (med in medicines) {
                medicineDao.insert(med)
            }

            val treatments = supabaseClient.fetchRemoteTreatments()
            for (tr in treatments) {
                treatmentDao.insert(tr)
            }
            _syncState.value = SyncState.Success
        } catch (e: Exception) {
            Log.e("RanchRepository", "Error fetching cloud data", e)
            _syncState.value = SyncState.Error("No se pudo descargar datos de la nube: ${e.localizedMessage}")
        }
    }
}
