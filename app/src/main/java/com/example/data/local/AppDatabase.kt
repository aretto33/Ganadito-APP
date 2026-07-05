package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AnimalDao
import com.example.data.local.dao.MedicineDao
import com.example.data.local.dao.TreatmentDao
import com.example.data.local.entities.AnimalEntity
import com.example.data.local.entities.MedicineEntity
import com.example.data.local.entities.TreatmentEntity

@Database(
    entities = [
        AnimalEntity::class,
        MedicineEntity::class,
        TreatmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animalDao(): AnimalDao
    abstract fun medicineDao(): MedicineDao
    abstract fun treatmentDao(): TreatmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ganaderia_sincro_db"
                )
                .fallbackToDestructiveMigration() // para prototipo rápido en AI Studio
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
