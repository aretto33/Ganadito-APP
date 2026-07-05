package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncStatus {
    SYNCED,
    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE
}

@Entity(tableName = "animales")
data class AnimalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tagNumber: String,
    val type: String, // Vaca, Toro, Ternero, Novilla, etc.
    val breed: String, // Raza: Angus, Holstein, Cebú, etc.
    val birthDate: String, // YYYY-MM-DD
    val weight: Double, // en kg
    val gender: String, // Macho / Hembra
    val userId: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "medicamentos")
data class MedicineEntity(
    @PrimaryKey val id: String,
    val name: String, // Nombre del medicamento
    val activeIngredient: String, // Principio activo
    val stock: Double, // Stock disponible
    val dosageUnit: String, // ml, g, UI, etc.
    val userId: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "tratamientos")
data class TreatmentEntity(
    @PrimaryKey val id: String,
    val animalId: String,
    val animalName: String, // Cache para mostrar sin joins complejos en offline
    val medicineId: String,
    val medicineName: String, // Cache
    val date: String, // YYYY-MM-DD
    val dosage: Double, // Dosis administrada
    val notes: String,
    val userId: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT,
    val lastUpdated: Long = System.currentTimeMillis()
)
