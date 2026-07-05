package com.example.data.remote

import android.util.Log
import com.example.data.local.entities.AnimalEntity
import com.example.data.local.entities.MedicineEntity
import com.example.data.local.entities.TreatmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseHttpClient(private val configManager: SupabaseConfigManager) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildRequest(path: String, method: String, bodyStr: String? = null): Request {
        val url = configManager.supabaseUrl.value.trimEnd('/') + path
        val apiKey = configManager.supabaseAnonKey.value
        val token = configManager.accessToken.value.ifEmpty { apiKey }

        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")

        if (method == "POST") {
            builder.post((bodyStr ?: "{}").toRequestBody(jsonMediaType))
        } else if (method == "PATCH") {
            builder.patch((bodyStr ?: "{}").toRequestBody(jsonMediaType))
        } else if (method == "DELETE") {
            builder.delete()
        } else {
            builder.get()
        }

        return builder.build()
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        if (!configManager.isConfigured()) return@withContext false
        try {
            // Hacemos una petición simple a la API de rest para verificar conectividad
            val request = buildRequest("/rest/v1/", "GET")
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseHttpClient", "Test connection response code: ${response.code}")
                return@withContext response.isSuccessful || response.code == 401 || response.code == 403 // auth issues mean we reached server
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Connection failed", e)
            false
        }
    }

    // --- AUTENTICACIÓN ---

    data class AuthResponse(
        val isSuccess: Boolean,
        val userId: String = "",
        val email: String = "",
        val token: String = "",
        val errorMessage: String? = null
    )

    suspend fun signUp(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        if (!configManager.isConfigured()) return@withContext AuthResponse(false, errorMessage = "Configuración de Supabase incompleta.")
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = buildRequest("/auth/v1/signup", "POST", body.toString())
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val userJson = json.optJSONObject("user")
                    val uid = userJson?.optString("id") ?: ""
                    val token = json.optString("access_token") ?: ""
                    val resEmail = userJson?.optString("email") ?: email
                    AuthResponse(true, userId = uid, email = resEmail, token = token)
                } else {
                    val errMsg = try {
                        JSONObject(bodyStr).optString("msg", response.message)
                    } catch (e: Exception) {
                        response.message
                    }
                    AuthResponse(false, errorMessage = "Error en Registro: $errMsg")
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Signup exception", e)
            AuthResponse(false, errorMessage = "Error de red: ${e.localizedMessage}")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        if (!configManager.isConfigured()) return@withContext AuthResponse(false, errorMessage = "Configuración de Supabase incompleta.")
        try {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = buildRequest("/auth/v1/token?grant_type=password", "POST", body.toString())
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val userJson = json.optJSONObject("user")
                    val uid = userJson?.optString("id") ?: ""
                    val token = json.optString("access_token") ?: ""
                    val resEmail = userJson?.optString("email") ?: email
                    AuthResponse(true, userId = uid, email = resEmail, token = token)
                } else {
                    val errMsg = try {
                        JSONObject(bodyStr).optString("error_description", JSONObject(bodyStr).optString("error", response.message))
                    } catch (e: Exception) {
                        response.message
                    }
                    AuthResponse(false, errorMessage = "Error de Credenciales: $errMsg")
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Signin exception", e)
            AuthResponse(false, errorMessage = "Error de red: ${e.localizedMessage}")
        }
    }

    // --- SYNC ENGINE - ANIMALES ---

    suspend fun pushAnimal(animal: AnimalEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("id", animal.id)
                put("name", animal.name)
                put("tag_number", animal.tagNumber)
                put("type", animal.type)
                put("breed", animal.breed)
                put("birth_date", animal.birthDate)
                put("weight", animal.weight)
                put("gender", animal.gender)
                put("user_id", animal.userId)
                put("last_updated", animal.lastUpdated)
            }

            // PostgREST upsert can be done using POST with resolution headers, 
            // or we can query if it exists. But a standard POST with "Prefer: resolution=merge-duplicates" is cleanest
            val request = buildRequest("/rest/v1/animales", "POST", body.toString()).newBuilder()
                .addHeader("Prefer", "resolution=merge-duplicates")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d("SupabaseHttpClient", "Push animal response: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Push animal exception", e)
            false
        }
    }

    suspend fun deleteAnimal(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/rest/v1/animales?id=eq.$id", "DELETE")
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseHttpClient", "Delete animal response: ${response.code}")
                return@withContext response.isSuccessful || response.code == 404 // 404 means already deleted
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Delete animal exception", e)
            false
        }
    }

    // --- SYNC ENGINE - MEDICAMENTOS ---

    suspend fun pushMedicine(medicine: MedicineEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("id", medicine.id)
                put("name", medicine.name)
                put("active_ingredient", medicine.activeIngredient)
                put("stock", medicine.stock)
                put("dosage_unit", medicine.dosageUnit)
                put("user_id", medicine.userId)
                put("last_updated", medicine.lastUpdated)
            }

            val request = buildRequest("/rest/v1/medicamentos", "POST", body.toString()).newBuilder()
                .addHeader("Prefer", "resolution=merge-duplicates")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d("SupabaseHttpClient", "Push medicine response: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Push medicine exception", e)
            false
        }
    }

    suspend fun deleteMedicine(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/rest/v1/medicamentos?id=eq.$id", "DELETE")
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseHttpClient", "Delete medicine response: ${response.code}")
                return@withContext response.isSuccessful || response.code == 404
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Delete medicine exception", e)
            false
        }
    }

    // --- SYNC ENGINE - TRATAMIENTOS ---

    suspend fun pushTreatment(treatment: TreatmentEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("id", treatment.id)
                put("animal_id", treatment.animalId)
                put("animal_name", treatment.animalName)
                put("medicine_id", treatment.medicineId)
                put("medicine_name", treatment.medicineName)
                put("date", treatment.date)
                put("dosage", treatment.dosage)
                put("notes", treatment.notes)
                put("user_id", treatment.userId)
                put("last_updated", treatment.lastUpdated)
            }

            val request = buildRequest("/rest/v1/tratamientos", "POST", body.toString()).newBuilder()
                .addHeader("Prefer", "resolution=merge-duplicates")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d("SupabaseHttpClient", "Push treatment response: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Push treatment exception", e)
            false
        }
    }

    suspend fun deleteTreatment(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/rest/v1/tratamientos?id=eq.$id", "DELETE")
            client.newCall(request).execute().use { response ->
                Log.d("SupabaseHttpClient", "Delete treatment response: ${response.code}")
                return@withContext response.isSuccessful || response.code == 404
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Delete treatment exception", e)
            false
        }
    }

    // --- FETCH REMOTE DATA ON LOGIN ---

    suspend fun fetchRemoteAnimals(): List<AnimalEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AnimalEntity>()
        try {
            val request = buildRequest("/rest/v1/animales?select=*", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val array = JSONArray(bodyStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            AnimalEntity(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                tagNumber = obj.getString("tag_number"),
                                type = obj.getString("type"),
                                breed = obj.getString("breed"),
                                birthDate = obj.getString("birth_date"),
                                weight = obj.optDouble("weight", 0.0),
                                gender = obj.getString("gender"),
                                userId = obj.getString("user_id"),
                                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                                lastUpdated = obj.optLong("last_updated", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Fetch animals exception", e)
        }
        list
    }

    suspend fun fetchRemoteMedicines(): List<MedicineEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MedicineEntity>()
        try {
            val request = buildRequest("/rest/v1/medicamentos?select=*", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val array = JSONArray(bodyStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            MedicineEntity(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                activeIngredient = obj.getString("active_ingredient"),
                                stock = obj.optDouble("stock", 0.0),
                                dosageUnit = obj.getString("dosage_unit"),
                                userId = obj.getString("user_id"),
                                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                                lastUpdated = obj.optLong("last_updated", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Fetch medicines exception", e)
        }
        list
    }

    suspend fun fetchRemoteTreatments(): List<TreatmentEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TreatmentEntity>()
        try {
            val request = buildRequest("/rest/v1/tratamientos?select=*", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val array = JSONArray(bodyStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(
                            TreatmentEntity(
                                id = obj.getString("id"),
                                animalId = obj.getString("animal_id"),
                                animalName = obj.optString("animal_name", "Animal Desconocido"),
                                medicineId = obj.getString("medicine_id"),
                                medicineName = obj.optString("medicine_name", "Medicamento Desconocido"),
                                date = obj.getString("date"),
                                dosage = obj.optDouble("dosage", 0.0),
                                notes = obj.optString("notes", ""),
                                userId = obj.getString("user_id"),
                                syncStatus = com.example.data.local.entities.SyncStatus.SYNCED,
                                lastUpdated = obj.optLong("last_updated", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseHttpClient", "Fetch treatments exception", e)
        }
        list
    }
}
