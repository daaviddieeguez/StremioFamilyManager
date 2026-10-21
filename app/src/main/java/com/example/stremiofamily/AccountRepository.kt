package com.example.stremiofamily

import android.content.Context
import java.util.UUID

data class TargetPreview(
    val account: SavedAccount,
    val current: List<AddonDescriptor>,
    val additions: Int,
    val removals: Int
)

data class SyncPreview(
    val source: SavedAccount,
    val sourceAddons: List<AddonDescriptor>,
    val targets: List<TargetPreview>
)

data class SyncResult(val account: SavedAccount, val addonsWritten: Int)

class StalePreviewException : Exception("La lista de addons cambió desde la vista previa. Revísala de nuevo antes de aplicar.")
class PartialSyncException(val completed: List<SyncResult>, cause: Exception) :
    Exception("La sincronización se detuvo después de actualizar ${completed.size} cuenta(s): ${cause.message}", cause)

class AccountRepository(
    context: Context,
    private val api: StremioApi = StremioApi(),
    private val store: SecureSessionStore = SecureSessionStore(context)
) {
    fun accounts(): List<SavedAccount> = store.accounts()

    suspend fun connect(name: String, email: String, password: String): SavedAccount {
        val session = api.login(email.trim(), password)
        val account = SavedAccount(UUID.randomUUID().toString(), name.trim().ifBlank { email.trim() }, session.email, session.userId, true)
        store.save(account, session.authKey)
        return account
    }

    suspend fun refresh(accountId: String): AddonCollection = api.getAddonCollection(requireToken(accountId))

    suspend fun preview(sourceId: String, targetIds: List<String>): SyncPreview {
        require(targetIds.isNotEmpty()) { "Selecciona al menos una cuenta de destino." }
        require(sourceId !in targetIds) { "La cuenta de origen no puede ser también un destino." }
        val source = account(sourceId)
        val sourceAddons = api.getAddonCollection(requireToken(sourceId)).addons
        val targets = targetIds.distinct().map { id ->
            val target = account(id)
            val current = api.getAddonCollection(requireToken(id)).addons
            val difference = addonDifference(sourceAddons, current)
            TargetPreview(target, current, difference.additions, difference.removals)
        }
        return SyncPreview(source, sourceAddons, targets)
    }

    /** Re-reads each collection before writing and aborts if anything changed since confirmation. */
    suspend fun apply(preview: SyncPreview): List<SyncResult> {
        val freshSource = api.getAddonCollection(requireToken(preview.source.id)).addons
        if (fingerprint(freshSource) != fingerprint(preview.sourceAddons)) throw StalePreviewException()
        val results = mutableListOf<SyncResult>()
        for (target in preview.targets) {
            try {
                val token = requireToken(target.account.id)
                val freshTarget = api.getAddonCollection(token).addons
                if (fingerprint(freshTarget) != fingerprint(target.current)) throw StalePreviewException()
                api.setAddonCollection(token, preview.sourceAddons.map { it.copyDescriptor() })
                results += SyncResult(target.account, preview.sourceAddons.size)
            } catch (e: Exception) {
                if (results.isNotEmpty()) throw PartialSyncException(results.toList(), e)
                throw e
            }
        }
        return results
    }

    suspend fun disconnect(accountId: String) {
        val token = store.authKey(accountId)
        if (token != null) runCatching { api.logout(token) }
        store.remove(accountId)
    }

    private fun account(id: String) = store.accounts().firstOrNull { it.id == id }
        ?: throw StremioApiException("No se encontró esa cuenta guardada.")

    private fun requireToken(id: String) = store.authKey(id)
        ?: throw StremioApiException("La sesión de esta cuenta caducó. Inicia sesión de nuevo.")

    private fun fingerprint(addons: List<AddonDescriptor>): List<String> = addons
        .map { it.transportUrl + "\u0000" + it.value.toString() }
        .sorted()
}
