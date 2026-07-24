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
import java.util.concurrent.TimeUnit

class SupabaseHttpClient(private val configManager: SupabaseConfigManager) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildRequest(path: String, method: String, bodyStr: String? = null, isAuth: Boolean = false, overrideToken: String? = null): Request {
        val url = configManager.supabaseUrl.value.trimEnd('/') + path
        val apiKey = configManager.supabaseAnonKey.value
        val currentToken = overrideToken ?: configManager.accessToken.value

        val builder = Request.Builder().url(url).addHeader("apikey", apiKey).addHeader("Content-Type", "application/json")

        val authHeader = overrideToken ?: currentToken.ifEmpty { apiKey }
        builder.addHeader("Authorization", "Bearer $authHeader")

        when (method) {
            "POST" -> builder.post((bodyStr ?: "{}").toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch((bodyStr ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete()
            else -> builder.get()
        }
        return builder.build()
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val r = buildRequest("/rest/v1/", "GET")
            client.newCall(r).execute().use { it.isSuccessful || it.code == 401 }
        } catch (e: Exception) { false }
    }

    data class AuthResponse(val isSuccess: Boolean, val userId: String = "", val email: String = "", val token: String = "", val errorMessage: String? = null)

    suspend fun signUp(email: String, password: String, metadata: Map<String, Any> = emptyMap()): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email); put("password", password)
                if (metadata.isNotEmpty()) put("data", JSONObject(metadata))
            }
            val request = buildRequest("/auth/v1/signup", "POST", body.toString(), isAuth = true)
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val userJson = json.optJSONObject("user")
                    AuthResponse(true, userJson?.optString("id") ?: "", userJson?.optString("email") ?: email, json.optString("access_token") ?: "")
                } else {
                    val jobj = try { JSONObject(bodyStr) } catch(e:Exception) { null }
                    val errMsg = jobj?.optString("msg").takeIf { !it.isNullOrEmpty() } 
                                ?: jobj?.optString("error_description").takeIf { !it.isNullOrEmpty() }
                                ?: "Error ${response.code}: ${response.message}"
                    AuthResponse(false, errorMessage = errMsg)
                }
            }
        } catch (e: Exception) { AuthResponse(false, errorMessage = "Fallo de red: ${e.localizedMessage}") }
    }

    data class ProfileResult(val isSuccess: Boolean, val idUsuario: Int = -1, val pkProductor: Int = -1, val error: String? = null)

    suspend fun createUserProfile(authUid: String, email: String, metadata: Map<String, Any>, token: String): ProfileResult = withContext(Dispatchers.IO) {
        try {
            val userBody = JSONObject().apply {
                put("usuario", email); put("email", email); put("auth_uid", authUid)
                put("password", "N/A"); put("rol", metadata["tipo_usuario"])
            }
            val uReq = buildRequest("/rest/v1/usuarios", "POST", userBody.toString(), overrideToken = token).newBuilder()
                .addHeader("Prefer", "return=representation").build()

            client.newCall(uReq).execute().use { uRes ->
                val uBody = uRes.body?.string() ?: "[]"
                if (!uRes.isSuccessful) return@withContext ProfileResult(false, error = "Usuarios: $uBody")
                val uArray = JSONArray(uBody)
                if (uArray.length() == 0) return@withContext ProfileResult(false, error = "No ID")
                val idU = uArray.getJSONObject(0).getInt("id_usuario")

                if (metadata["tipo_usuario"] == "Productor") {
                    val pBody = JSONObject().apply {
                        put("fk_usuario", idU); put("nombre", metadata["nombre"])
                        put("apellido_pat", metadata["apellido_paterno"]); put("apellido_mat", metadata["apellido_materno"])
                        put("rfc", metadata["rfc"])
                    }
                    val pReq = buildRequest("/rest/v1/productores", "POST", pBody.toString(), overrideToken = token).newBuilder()
                        .addHeader("Prefer", "return=representation").build()
                    client.newCall(pReq).execute().use { pRes ->
                        val pBodyStr = pRes.body?.string() ?: "[]"
                        if (!pRes.isSuccessful) return@withContext ProfileResult(false, error = "Prod: $pBodyStr")
                        val pArray = JSONArray(pBodyStr)
                        val pkP = if (pArray.length() > 0) pArray.getJSONObject(0).getInt("pk_productor") else -1
                        return@withContext ProfileResult(true, idUsuario = idU, pkProductor = pkP)
                    }
                }
                return@withContext ProfileResult(true, idUsuario = idU)
            }
        } catch (e: Exception) { ProfileResult(false, error = e.localizedMessage) }
    }

    suspend fun signIn(email: String, password: String): AuthResponse = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("email", email); put("password", password) }
            val request = buildRequest("/auth/v1/token?grant_type=password", "POST", body.toString(), isAuth = true)
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(bodyStr)
                    val userJson = json.optJSONObject("user")
                    AuthResponse(true, userJson?.optString("id") ?: "", userJson?.optString("email") ?: email, json.optString("access_token") ?: "")
                } else {
                    val jobj = try { JSONObject(bodyStr) } catch(e:Exception) { null }
                    val errMsg = jobj?.optString("error_description").takeIf { !it.isNullOrEmpty() }
                                ?: jobj?.optString("error").takeIf { !it.isNullOrEmpty() }
                                ?: jobj?.optString("msg").takeIf { !it.isNullOrEmpty() }
                                ?: "Error ${response.code}: ${response.message}"
                    AuthResponse(false, errorMessage = errMsg)
                }
            }
        } catch (e: Exception) { AuthResponse(false, errorMessage = "Error red: ${e.localizedMessage}") }
    }

    suspend fun getUserDatabaseIds(authUid: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        try {
            val r = buildRequest("/rest/v1/usuarios?auth_uid=eq.$authUid&select=id_usuario,productores(pk_productor)", "GET")
            client.newCall(r).execute().use { res ->
                if (res.isSuccessful) {
                    val array = JSONArray(res.body?.string() ?: "[]")
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        val idU = obj.optInt("id_usuario", -1)
                        val prodArray = obj.optJSONArray("productores")
                        val pkP = if (prodArray != null && prodArray.length() > 0) prodArray.getJSONObject(0).optInt("pk_productor", -1) else -1
                        return@withContext Pair(idU, pkP)
                    }
                }
            }
        } catch (e: Exception) { }
        Pair(-1, -1)
    }

    suspend fun pushAnimal(animal: AnimalEntity, pkProductor: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("nombre", animal.name); put("fecha_nacimiento", animal.birthDate)
                put("sexo", if (animal.gender.startsWith("M", true)) "M" else "H")
                put("peso_actual", animal.weight)
                if (pkProductor != -1) put("fk_productor", pkProductor)
                put("user_id", animal.userId)
            }
            val r = buildRequest("/rest/v1/animales", "POST", body.toString()).newBuilder().addHeader("Prefer", "resolution=merge-duplicates").build()
            client.newCall(r).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    suspend fun deleteAnimal(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val r = buildRequest("/rest/v1/animales?pk_animal=eq.$id", "DELETE")
            client.newCall(r).execute().use { it.isSuccessful || it.code == 404 }
        } catch (e: Exception) { false }
    }

    suspend fun pushMedicine(medicine: MedicineEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("nombre", medicine.name); put("stock_actual", medicine.stock)
                put("categoria", medicine.activeIngredient); put("user_id", medicine.userId)
            }
            val r = buildRequest("/rest/v1/insumos_medicos", "POST", body.toString()).newBuilder().addHeader("Prefer", "resolution=merge-duplicates").build()
            client.newCall(r).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    suspend fun deleteMedicine(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val r = buildRequest("/rest/v1/insumos_medicos?id_insumo=eq.$id", "DELETE")
            client.newCall(r).execute().use { it.isSuccessful || it.code == 404 }
        } catch (e: Exception) { false }
    }

    suspend fun pushTreatment(tr: TreatmentEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("nombre", tr.animalName + " - " + tr.medicineName)
                put("medicamento", tr.medicineName); put("dosis", tr.dosage.toString())
                put("observaciones", tr.notes); put("user_id", tr.userId)
            }
            val r = buildRequest("/rest/v1/tratamientos", "POST", body.toString()).newBuilder().addHeader("Prefer", "resolution=merge-duplicates").build()
            client.newCall(r).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    suspend fun deleteTreatment(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val r = buildRequest("/rest/v1/tratamientos?pk_tratamiento=eq.$id", "DELETE")
            client.newCall(r).execute().use { it.isSuccessful || it.code == 404 }
        } catch (e: Exception) { false }
    }

    suspend fun fetchRemoteAnimals(pkProductor: Int): List<AnimalEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<AnimalEntity>()
        try {
            val path = if (pkProductor != -1) "/rest/v1/animales?fk_productor=eq.$pkProductor&select=*" else "/rest/v1/animales?select=*"
            val r = buildRequest(path, "GET")
            client.newCall(r).execute().use { response ->
                if (response.isSuccessful) {
                    val array = JSONArray(response.body?.string() ?: "[]")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(AnimalEntity(
                            id = obj.getString("pk_animal"), name = obj.getString("nombre"),
                            tagNumber = "Remoto", type = "Bovino", breed = "N/A",
                            birthDate = obj.getString("fecha_nacimiento"),
                            weight = obj.optDouble("peso_actual", 0.0),
                            gender = if (obj.optString("sexo") == "M") "Macho" else "Hembra",
                            userId = configManager.userId.value, syncStatus = com.example.data.local.entities.SyncStatus.SYNCED
                        ))
                    }
                }
            }
        } catch (e: Exception) { }
        list
    }

    suspend fun fetchRemoteMedicines(): List<MedicineEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MedicineEntity>()
        try {
            val r = buildRequest("/rest/v1/insumos_medicos?select=*", "GET")
            client.newCall(r).execute().use { response ->
                if (response.isSuccessful) {
                    val array = JSONArray(response.body?.string() ?: "[]")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        list.add(MedicineEntity(
                            id = obj.getString("id_insumo"), name = obj.optString("nombre", "Insumo"),
                            activeIngredient = obj.optString("categoria", ""), stock = obj.optDouble("stock_actual", 0.0),
                            dosageUnit = "ml", userId = configManager.userId.value, syncStatus = com.example.data.local.entities.SyncStatus.SYNCED
                        ))
                    }
                }
            }
        } catch (e: Exception) { }
        list
    }
}
