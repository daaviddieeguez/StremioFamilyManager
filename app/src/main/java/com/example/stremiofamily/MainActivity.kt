package com.example.stremiofamily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StremioFamilyApp(AccountRepository(applicationContext)) }
    }
}

@Composable
private fun SettingsInfo(icon: ImageVector, title: String, description: String, tint: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StremioFamilyApp(repository: AccountRepository) {
    val context = LocalContext.current
    var accounts by remember { mutableStateOf(repository.accounts()) }
    var selectedTargets by remember { mutableStateOf(emptySet<String>()) }
    var sourceId by remember { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var showLogin by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAddAddon by remember { mutableStateOf(false) }
    var addonUrl by remember { mutableStateOf("") }
    var showDisconnect by remember { mutableStateOf<SavedAccount?>(null) }
    var preview by remember { mutableStateOf<SyncPreview?>(null) }
    var details by remember { mutableStateOf<Pair<SavedAccount, AddonCollection>?>(null) }
    var pendingRemove by remember { mutableStateOf<Pair<SavedAccount, AddonDescriptor>?>(null) }
    var loginName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }
    var sourceMenu by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(context.getSharedPreferences("preferences", 0).getBoolean("dark_theme", true)) }
    val Ink = if (darkTheme) Color(0xFF11131A) else Color(0xFFF5F6FA)
    val Panel = if (darkTheme) Color(0xFF1B1E28) else Color.White
    val Muted = if (darkTheme) Color(0xFF9CA3B5) else Color(0xFF667085)
    val Purple = if (darkTheme) Color(0xFFB3A6FF) else Color(0xFF6750A4)
    val Mint = if (darkTheme) Color(0xFF66D9B3) else Color(0xFF087F68)
    var message by remember { mutableStateOf("Conecta tus cuentas de Stremio para consultar y sincronizar sus addons.") }
    val scope = rememberCoroutineScope()

    fun reloadAccounts() {
        accounts = repository.accounts()
        if (accounts.none { it.id == sourceId }) sourceId = accounts.firstOrNull()?.id.orEmpty()
        selectedTargets = selectedTargets.intersect(accounts.map { it.id }.toSet())
    }

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme(
        background = Ink, surface = Panel, primary = Purple,
        onBackground = Color.White, onSurface = Color.White, secondary = Mint
    ) else lightColorScheme(
        background = Ink, surface = Panel, primary = Purple,
        onBackground = Color(0xFF19171F), onSurface = Color(0xFF19171F), secondary = Mint
    )) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Stremio Family", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("GESTOR DE ADDONS", fontSize = 10.sp, color = Muted, letterSpacing = 2.sp)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Ink),
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Purple)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { if (accounts.isEmpty()) showLogin = true else showAddAddon = true }, containerColor = Purple, contentColor = if (darkTheme) Ink else Color.White) {
                    Icon(Icons.Default.Add, contentDescription = if (accounts.isEmpty()) "Conectar cuenta de Stremio" else "Añadir addon")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(shape = RoundedCornerShape(24.dp), color = Panel, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(46.dp).background(Purple.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CloudSync, null, tint = Purple, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Tus cuentas, en un solo sitio", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("${accounts.size} ${if (accounts.size == 1) "cuenta conectada" else "cuentas conectadas"}", color = Muted, fontSize = 12.sp)
                                }
                            }
                            Text("Cuenta de origen", color = Muted, fontSize = 12.sp)
                            Box {
                                OutlinedButton(onClick = { sourceMenu = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                                    Text(accounts.firstOrNull { it.id == sourceId }?.email ?: "Conecta una cuenta", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("⌄")
                                }
                                DropdownMenu(expanded = sourceMenu, onDismissRequest = { sourceMenu = false }) {
                                    accounts.forEach { account ->
                                        DropdownMenuItem(text = { Text(account.email) }, onClick = { sourceId = account.id; sourceMenu = false })
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    val targets = selectedTargets.filter { it != sourceId }
                                    busy = true
                                    scope.launch {
                                        try {
                                            preview = repository.preview(sourceId, targets)
                                            message = "Vista previa lista. No se ha cambiado ninguna cuenta."
                                        } catch (e: Exception) { message = e.message ?: "No se pudo consultar las listas de addons." }
                                        finally { busy = false }
                                    }
                                },
                                enabled = !busy && accounts.size > 1 && selectedTargets.any { it != sourceId },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(15.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = if (darkTheme) Ink else Color.White)
                            ) {
                                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.CloudSync, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Previsualizar sincronización", fontWeight = FontWeight.Bold)
                            }
                            if (accounts.isNotEmpty()) {
                                OutlinedButton(onClick = { showAddAddon = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Añadir addon por URL")
                                }
                            }
                            Text("Añadir instala el manifiesto en esta cuenta. Sincronizar replica la lista completa y puede quitar addons exclusivos del destino.", color = Mint, fontSize = 11.sp)
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PeopleAlt, null, tint = Purple)
                        Spacer(Modifier.width(8.dp))
                        Text("Cuentas familiares", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                items(accounts, key = { it.id }) { account ->
                    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = account.id in selectedTargets,
                                    enabled = account.id != sourceId,
                                    onCheckedChange = { checked -> selectedTargets = if (checked) selectedTargets + account.id else selectedTargets - account.id },
                                    colors = CheckboxDefaults.colors(checkedColor = Purple, uncheckedColor = Muted)
                                )
                                Box(Modifier.size(42.dp).background(if (darkTheme) Color(0xFF2B2B42) else Color(0xFFEAE5F4), CircleShape), contentAlignment = Alignment.Center) {
                                    Text(account.name.take(1).uppercase(), color = Purple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(account.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(account.email, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (account.id == sourceId) "Origen" else "Destino seleccionable", color = if (account.id == sourceId) Mint else Muted, fontSize = 10.sp)
                                }
                                IconButton(enabled = !busy, onClick = {
                                    busy = true
                                    scope.launch {
                                        try { details = account to repository.refresh(account.id) }
                                        catch (e: Exception) { message = e.message ?: "No se pudo leer la lista de addons." }
                                        finally { busy = false }
                                    }
                                }) { Icon(Icons.Default.Extension, contentDescription = "Ver addons", tint = Purple) }
                                IconButton(enabled = !busy, onClick = { showDisconnect = account }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Desconectar cuenta", tint = Muted)
                                }
                            }
                        }
                    }
                }
                item {
                    Surface(color = if (darkTheme) Color(0xFF202A2B) else Color(0xFFE8F4F0), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Security, null, tint = Mint)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Sesiones protegidas", fontWeight = FontWeight.SemiBold, color = Mint)
                                Text("La contraseña solo se usa para iniciar sesión y no se guarda. El token de sesión se cifra con Android Keystore en este dispositivo.", color = if (darkTheme) Color(0xFFCBD5D7) else Color(0xFF34413D), fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    Text(message, color = Muted, fontSize = 12.sp)
                    Text("Stremio Family Manager · beta", color = Color(0xFF555B6C), fontSize = 11.sp)
                }
            }
        }
    }

    if (showAddAddon) {
        AlertDialog(
            onDismissRequest = { if (!busy) showAddAddon = false },
            title = { Text("Añadir o actualizar addon") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pega la URL del manifiesto. Stremio la validará y añadirá a la cuenta seleccionada.", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(value = accounts.firstOrNull { it.id == sourceId }?.email.orEmpty(), onValueChange = {}, readOnly = true, label = { Text("Cuenta") }, enabled = accounts.isNotEmpty())
                    OutlinedTextField(value = addonUrl, onValueChange = { addonUrl = it.trim() }, label = { Text("URL del manifiesto") }, placeholder = { Text("https://addon.example/manifest.json") }, singleLine = true, enabled = !busy)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && accounts.isNotEmpty() && addonUrl.startsWith("http"), onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val fresh = repository.addAddon(sourceId, addonUrl)
                            details = (accounts.first { it.id == sourceId }) to fresh
                            message = "Addon añadido o actualizado en ${accounts.first { it.id == sourceId }.email}."
                            addonUrl = ""; showAddAddon = false
                        } catch (e: Exception) { message = e.message ?: "No se pudo instalar el addon." }
                        finally { busy = false }
                    }
                }) { Text(if (busy) "Instalando…" else "Instalar / actualizar") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { showAddAddon = false }) { Text("Cancelar") } }
        )
    }

    if (showLogin) {
        AlertDialog(
            onDismissRequest = { if (!busy) showLogin = false },
            title = { Text("Conectar cuenta de Stremio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = loginName, onValueChange = { loginName = it }, label = { Text("Nombre para identificarla") }, singleLine = true, enabled = !busy)
                    OutlinedTextField(value = email, onValueChange = { email = it; loginError = "" }, label = { Text("Correo") }, singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(value = password, onValueChange = { password = it; loginError = "" }, label = { Text("Contraseña de Stremio") }, singleLine = true, enabled = !busy, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    if (loginError.isNotBlank()) Text(loginError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Text("La contraseña se envía directamente a la API de Stremio por HTTPS y no se guarda en el teléfono ni se muestra en registros.", color = Muted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = !busy && email.isNotBlank() && password.isNotBlank(), onClick = {
                    val submittedEmail = email.trim()
                    val submittedPassword = password
                    val submittedName = loginName
                    password = ""
                    busy = true
                    scope.launch {
                        try {
                            val account = repository.connect(submittedName, submittedEmail, submittedPassword)
                            reloadAccounts()
                            selectedTargets = selectedTargets + account.id
                            if (sourceId.isBlank()) sourceId = account.id
                            message = "Cuenta ${account.email} conectada."
                            email = ""; loginName = ""; showLogin = false
                        } catch (e: Exception) { loginError = e.message ?: "No se pudo iniciar sesión." }
                        finally { busy = false }
                    }
                }) { Text(if (busy) "Conectando…" else "Iniciar sesión") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { showLogin = false; password = "" }) { Text("Cancelar") } }
        )
    }

    showDisconnect?.let { account ->
        AlertDialog(
            onDismissRequest = { showDisconnect = null },
            title = { Text("Desconectar cuenta") },
            text = { Text("Se cerrará esta sesión en Stremio y se borrará su token guardado en este dispositivo. No se eliminarán los addons de la cuenta.") },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnect = null; busy = true
                    scope.launch {
                        try {
                            repository.disconnect(account.id)
                            reloadAccounts()
                            message = "Cuenta desconectada de este dispositivo."
                        } catch (e: Exception) { message = e.message ?: "No se pudo desconectar la cuenta." }
                        finally { busy = false }
                    }
                }) { Text("Desconectar") }
            },
            dismissButton = { TextButton(onClick = { showDisconnect = null }) { Text("Cancelar") } }
        )
    }

    preview?.let { pending ->
        AlertDialog(
            onDismissRequest = { if (!busy) preview = null },
            title = { Text("Revisar cambios") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Origen: ${pending.source.email}", fontWeight = FontWeight.SemiBold)
                    Text("La operación reemplazará la lista completa de addons de estos destinos. Los addons que solo existan en un destino se quitarán.", color = Muted, fontSize = 13.sp)
                    pending.targets.forEach { target ->
                        Surface(color = if (darkTheme) Color(0xFF292D3A) else Color(0xFFF0EEF6), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Text(target.account.email, fontWeight = FontWeight.SemiBold)
                                Text("Añadir: ${target.additions} · Quitar: ${target.removals} · Quedarán: ${pending.sourceAddons.size}", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val results = repository.apply(pending)
                            message = "Sincronización completada: ${results.size} cuenta(s) actualizada(s)."
                            preview = null
                        } catch (e: StalePreviewException) {
                            preview = null
                            message = e.message ?: "La lista cambió. Genera una nueva vista previa."
                        } catch (e: PartialSyncException) {
                            message = "Se actualizaron ${e.completed.size} cuenta(s); se detuvo el proceso. ${e.cause?.message ?: "Error de red."}"
                            preview = null
                        } catch (e: Exception) { message = e.message ?: "No se pudo completar la sincronización." }
                        finally { busy = false }
                    }
                }) { Text(if (busy) "Aplicando…" else "Aplicar a destinos") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { preview = null }) { Text("Cancelar") } }
        )
    }

    details?.let { (account, collection) ->
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text("Addons · ${account.name}") },
            text = {
                if (collection.addons.isEmpty()) Text("Esta cuenta no devolvió addons instalados.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(collection.addons) { addon ->
                        Surface(color = if (darkTheme) Color(0xFF292D3A) else Color(0xFFF0EEF6), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                                    Text(addon.name.ifBlank { "Addon sin nombre" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(addon.transportUrl, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(enabled = !busy, onClick = { pendingRemove = account to addon }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Quitar addon", tint = Muted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { details = null }) { Text("Cerrar") } }
        )
    }

    pendingRemove?.let { (account, addon) ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingRemove = null },
            title = { Text("Quitar ${addon.name}?") },
            text = { Text("Se quitará de la lista de addons de ${account.email}. La acción no elimina ni modifica el addon en su servidor.", color = Muted) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    pendingRemove = null; busy = true
                    scope.launch {
                        try {
                            details = account to repository.removeAddon(account.id, addon.transportUrl)
                            message = "Addon quitado de ${account.email}."
                        } catch (e: Exception) { message = e.message ?: "No se pudo quitar el addon." }
                        finally { busy = false }
                    }
                }) { Text(if (busy) "Quitando…" else "Quitar addon") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { pendingRemove = null }) { Text("Cancelar") } }
        )
    }

        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                containerColor = Panel,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("Ajustes", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Personaliza la app y revisa cómo se protegen tus cuentas.", color = Muted, fontSize = 14.sp)
                    HorizontalDivider(color = Muted.copy(alpha = .2f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tema oscuro", fontWeight = FontWeight.SemiBold)
                            Text("Usar una apariencia oscura", color = Muted, fontSize = 12.sp)
                        }
                        Switch(checked = darkTheme, onCheckedChange = { enabled ->
                            darkTheme = enabled
                            context.getSharedPreferences("preferences", 0).edit().putBoolean("dark_theme", enabled).apply()
                        })
                    }
                    SettingsInfo(Icons.Default.Security, "Sesiones protegidas", "Las contraseñas no se guardan. Los tokens de sesión se cifran en este dispositivo.", Mint)
                    SettingsInfo(Icons.Default.CloudSync, "Sincronización", "La vista previa muestra qué addons se añadirán o quitarán. Aplicar reemplaza la lista completa de cada cuenta de destino.", Purple)
                    SettingsInfo(Icons.Default.Extension, "Añadir y actualizar", "Pega la URL del manifiesto del addon. La app lo instala en la cuenta de origen seleccionada y vuelve a cargar la lista.", Purple)
                    Text("Stremio Family Manager · 0.3.0", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
                }
            }
        }
}
