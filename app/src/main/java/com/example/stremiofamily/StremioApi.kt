package com.example.stremiofamily

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

data class StremioSession(val authKey: String, val userId: String, val email: String)

data class AddonDescriptor(val value: JSONObject) {
    val transportUrl: String get() = value.optString("transportUrl")
    val name: String get() = value.optJSONObject("manifest")?.optString("name")
        ?.takeIf { it.isNotBlank() } ?: transportUrl
    fun copyDescriptor() = AddonDescriptor(JSONObject(value.toString()))
}

data class AddonCollection(val addons: List<AddonDescriptor>, val lastModified: String? = null)

class StremioApi(
    private val endpoint: String = DEFAULT_ENDPOINT,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun login(email: String, password: String): StremioSession = withContext(Dispatchers.IO) {
        val result = request("login", null, JSONObject().put("email", email).put("password", password))
        val user = result.optJSONObject("user") ?: throw StremioApiException("Stremio devolvió una respuesta de usuario inválida.")
        val authKey = result.optString("authKey")
        if (authKey.isBlank()) throw StremioApiException("Stremio no devolvió una sesión válida.")
        StremioSession(authKey, user.optString("_id"), user.optString("email", email))
    }

    suspend fun getAddonCollection(authKey: String): AddonCollection = withContext(Dispatchers.IO) {
        val result = request("addonCollectionGet", authKey, JSONObject()
            .put("update", true)
            .put("addFromURL", JSONArray()))
        val array = result.optJSONArray("addons")
            ?: throw StremioApiException("Stremio devolvió una lista de addons inválida.")
        val addons = buildList {
            for (i in 0 until array.length()) array.optJSONObject(i)?.let { add(AddonDescriptor(JSONObject(it.toString()))) }
        }
        AddonCollection(addons, result.optString("lastModified").takeIf { it.isNotBlank() })
    }

    /** Adds or refreshes an addon from its manifest URL using Stremio\'s collection API. */
    suspend fun addAddonFromUrl(authKey: String, manifestUrl: String) = withContext(Dispatchers.IO) {
        val normalized = manifestUrl.trim()
        require(normalized.startsWith("https://") || normalized.startsWith("http://")) {
            "Introduce una URL http(s) válida del manifiesto del addon."
        }
        request("addonCollectionGet", authKey, JSONObject()
            .put("update", true)
            .put("addFromURL", JSONArray().put(normalized)))
    }

    suspend fun setAddonCollection(authKey: String, addons: List<AddonDescriptor>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        addons.forEach { array.put(JSONObject(it.value.toString())) }
        request("addonCollectionSet", authKey, JSONObject().put("addons", array))
    }

    suspend fun logout(authKey: String) = withContext(Dispatchers.IO) {
        request("logout", authKey, JSONObject())
    }

    private fun request(method: String, authKey: String?, params: JSONObject): JSONObject {
        val body = JSONObject(params.toString())
        if (authKey != null) body.put("authKey", authKey)
        val request = Request.Builder()
            .url(endpoint.trimEnd('/') + "/api/" + method)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .build()
        val response = try { client.newCall(request).execute() }
        catch (e: IOException) { throw StremioApiException("No se pudo conectar con Stremio. Comprueba la conexión e inténtalo de nuevo.") }
        response.use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw StremioApiException("Stremio respondió con el código ${res.code}.")
            val envelope = try { JSONObject(text) } catch (e: Exception) {
                throw StremioApiException("Stremio devolvió una respuesta que no se pudo leer.")
            }
            val error = envelope.opt("error")
            if (error != null && error != JSONObject.NULL) {
                val message = if (error is JSONObject) error.optString("message") else error.toString()
                throw StremioApiException(message.take(180).ifBlank { "Stremio rechazó la solicitud." })
            }
            val result = envelope.opt("result")
            if (result == null || result == JSONObject.NULL || result == false || result == 0 || result == "") {
                throw StremioApiException("Stremio devolvió una respuesta incompleta.")
            }
            return result as? JSONObject ?: JSONObject()
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.strem.io"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

class StremioApiException(message: String) : Exception(message)
