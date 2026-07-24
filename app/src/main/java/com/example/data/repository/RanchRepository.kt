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

    fun observeAnimals(userId: String): Flow<List<AnimalEntity>> = animalDao.observeAll(userId)
    fun observeMedicines(userId: String): Flow<List<MedicineEntity>> = medicineDao.observeAll(userId)
    fun observeTreatments(userId: String): Flow<List<TreatmentEntity>> = treatmentDao.observeAll(userId)

    suspend fun addAnimal(n: String, t: String, ty: String, b: String, d: String, w: Double, g: String, userId: String) {
        val animal = AnimalEntity(UUID.randomUUID().toString(), n, t, ty, b, d, w, g, userId, SyncStatus.PENDING_INSERT)
        animalDao.insert(animal)
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
        if (local.syncStatus == SyncStatus.PENDING_INSERT) animalDao.deleteLocally(id) else animalDao.markForDeletion(id)
    }

    suspend fun addMedicine(n: String, i: String, s: Double, u: String, userId: String) {
        val medicine = MedicineEntity(UUID.randomUUID().toString(), n, i, s, u, userId, SyncStatus.PENDING_INSERT)
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
        if (local.syncStatus == SyncStatus.PENDING_INSERT) medicineDao.deleteLocally(id) else medicineDao.markForDeletion(id)
    }

    suspend fun addTreatment(aI: String, aN: String, mI: String, mN: String, d: String, ds: Double, notes: String, userId: String) {
        val localMedicine = medicineDao.getById(mI)
        if (localMedicine != null) {
            val updatedMedicine = localMedicine.copy(
                stock = (localMedicine.stock - ds).coerceAtLeast(0.0),
                syncStatus = if (localMedicine.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE,
                lastUpdated = System.currentTimeMillis()
            )
            medicineDao.update(updatedMedicine)
        }
        val treatment = TreatmentEntity(UUID.randomUUID().toString(), aI, aN, mI, mN, d, ds, notes, userId, SyncStatus.PENDING_INSERT)
        treatmentDao.insert(treatment)
    }

    suspend fun deleteTreatment(id: String) {
        val local = treatmentDao.getById(id) ?: return
        val localMedicine = medicineDao.getById(local.medicineId)
        if (localMedicine != null) {
            val updatedMedicine = localMedicine.copy(
                stock = localMedicine.stock + local.dosage,
                syncStatus = if (localMedicine.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE,
                lastUpdated = System.currentTimeMillis()
            )
            medicineDao.update(updatedMedicine)
        }
        if (local.syncStatus == SyncStatus.PENDING_INSERT) treatmentDao.deleteLocally(id) else treatmentDao.markForDeletion(id)
    }

    suspend fun syncAll(userId: String, pkProductor: Int): Boolean {
        if (_syncState.value == SyncState.Syncing) return false
        _syncState.value = SyncState.Syncing
        try {
            if (!supabaseClient.testConnection()) { _syncState.value = SyncState.Error("Sin conexión"); return false }
            
            var success = true
            // Sync Animales
            animalDao.getUnsynced(userId).forEach { 
                if (it.syncStatus == SyncStatus.PENDING_DELETE) {
                    if (supabaseClient.deleteAnimal(it.id)) animalDao.deleteLocally(it.id) else success = false
                } else {
                    if (supabaseClient.pushAnimal(it, pkProductor)) animalDao.insert(it.copy(syncStatus = SyncStatus.SYNCED)) else success = false
                }
            }
            // Sync Medicamentos
            medicineDao.getUnsynced(userId).forEach { 
                if (it.syncStatus == SyncStatus.PENDING_DELETE) {
                    if (supabaseClient.deleteMedicine(it.id)) medicineDao.deleteLocally(it.id) else success = false
                } else {
                    if (supabaseClient.pushMedicine(it)) medicineDao.insert(it.copy(syncStatus = SyncStatus.SYNCED)) else success = false
                }
            }
            // Sync Tratamientos
            treatmentDao.getUnsynced(userId).forEach { 
                if (it.syncStatus == SyncStatus.PENDING_DELETE) {
                    if (supabaseClient.deleteTreatment(it.id)) treatmentDao.deleteLocally(it.id) else success = false
                } else {
                    if (supabaseClient.pushTreatment(it)) treatmentDao.insert(it.copy(syncStatus = SyncStatus.SYNCED)) else success = false
                }
            }

            if (success) { _syncState.value = SyncState.Success; return true }
            else { _syncState.value = SyncState.Error("Algunos fallaron"); return false }
        } catch (e: Exception) { _syncState.value = SyncState.Error(e.localizedMessage); return false }
    }

    suspend fun downloadRemoteData(pkProductor: Int) {
        _syncState.value = SyncState.Syncing
        try {
            supabaseClient.fetchRemoteAnimals(pkProductor).forEach { animalDao.insert(it) }
            supabaseClient.fetchRemoteMedicines().forEach { medicineDao.insert(it) }
            _syncState.value = SyncState.Success
        } catch (e: Exception) { _syncState.value = SyncState.Error(e.localizedMessage) }
    }
}
