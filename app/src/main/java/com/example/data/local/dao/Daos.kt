package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.AnimalEntity
import com.example.data.local.entities.MedicineEntity
import com.example.data.local.entities.TreatmentEntity
import com.example.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Query("SELECT * FROM animales WHERE syncStatus != 'PENDING_DELETE' AND userId = :userId ORDER BY name ASC")
    fun observeAll(userId: String): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animales WHERE id = :id")
    suspend fun getById(id: String): AnimalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(animal: AnimalEntity)

    @Update
    suspend fun update(animal: AnimalEntity)

    @Query("UPDATE animales SET syncStatus = 'PENDING_DELETE', lastUpdated = :timestamp WHERE id = :id")
    suspend fun markForDeletion(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM animales WHERE id = :id")
    suspend fun deleteLocally(id: String)

    @Query("SELECT * FROM animales WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsynced(): List<AnimalEntity>
}

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicamentos WHERE syncStatus != 'PENDING_DELETE' AND userId = :userId ORDER BY name ASC")
    fun observeAll(userId: String): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicamentos WHERE id = :id")
    suspend fun getById(id: String): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: MedicineEntity)

    @Update
    suspend fun update(medicine: MedicineEntity)

    @Query("UPDATE medicamentos SET syncStatus = 'PENDING_DELETE', lastUpdated = :timestamp WHERE id = :id")
    suspend fun markForDeletion(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM medicamentos WHERE id = :id")
    suspend fun deleteLocally(id: String)

    @Query("SELECT * FROM medicamentos WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsynced(): List<MedicineEntity>
}

@Dao
interface TreatmentDao {
    @Query("SELECT * FROM tratamientos WHERE syncStatus != 'PENDING_DELETE' AND userId = :userId ORDER BY date DESC")
    fun observeAll(userId: String): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM tratamientos WHERE id = :id")
    suspend fun getById(id: String): TreatmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(treatment: TreatmentEntity)

    @Update
    suspend fun update(treatment: TreatmentEntity)

    @Query("UPDATE tratamientos SET syncStatus = 'PENDING_DELETE', lastUpdated = :timestamp WHERE id = :id")
    suspend fun markForDeletion(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM tratamientos WHERE id = :id")
    suspend fun deleteLocally(id: String)

    @Query("SELECT * FROM tratamientos WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsynced(): List<TreatmentEntity>
}
