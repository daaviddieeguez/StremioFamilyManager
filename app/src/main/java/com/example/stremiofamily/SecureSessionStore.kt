package com.example.stremiofamily

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SavedAccount(
    val id: String,
    val name: String,
    val email: String,
    val userId: String,
    val connected: Boolean,
    val selected: Boolean = true
)

class SecureSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("stremio_accounts", Context.MODE_PRIVATE)
    private val keyAlias = "stremio_family_session_key"

    @Synchronized fun accounts(): List<SavedAccount> {
        val array = try { JSONArray(prefs.getString(KEY_ACCOUNTS, "[]")) } catch (_: Exception) { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isNotBlank()) add(SavedAccount(id, item.optString("name"), item.optString("email"), item.optString("userId"), prefs.contains(tokenKey(id))))
            }
        }
    }

    @Synchronized fun save(account: SavedAccount, authKey: String) {
        require(authKey.isNotBlank())
        val array = metadataArray()
        var found = false
        for (i in 0 until array.length()) if (array.optJSONObject(i)?.optString("id") == account.id) {
            array.put(i, metadata(account)); found = true; break
        }
        if (!found) array.put(metadata(account))
        val encrypted = encrypt(authKey)
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).putString(tokenKey(account.id), encrypted).apply()
    }

    @Synchronized fun authKey(accountId: String): String? = prefs.getString(tokenKey(accountId), null)?.let(::decrypt)

    @Synchronized fun remove(accountId: String) {
        val old = metadataArray()
        val next = JSONArray()
        for (i in 0 until old.length()) old.optJSONObject(i)?.takeIf { it.optString("id") != accountId }?.let(next::put)
        prefs.edit().putString(KEY_ACCOUNTS, next.toString()).remove(tokenKey(accountId)).apply()
    }

    private fun metadataArray() = try { JSONArray(prefs.getString(KEY_ACCOUNTS, "[]")) } catch (_: Exception) { JSONArray() }
    private fun metadata(a: SavedAccount) = JSONObject().put("id", a.id).put("name", a.name).put("email", a.email).put("userId", a.userId)
    private fun tokenKey(id: String) = "session_$id"

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > GCM_IV_BYTES) { "Invalid encrypted session" }
        val iv = payload.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = payload.copyOfRange(GCM_IV_BYTES, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        return generator.generateKey()
    }

    companion object {
        private const val KEY_ACCOUNTS = "accounts"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
    }
}
