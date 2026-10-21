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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF11131A)
private val Panel = Color(0xFF1B1E28)
private val Muted = Color(0xFF9CA3B5)
private val Purple = Color(0xFF9B8AFB)
private val Mint = Color(0xFF66D9B3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StremioFamilyApp() }
    }
}

private data class AccountProfile(
    val id: Int,
    val name: String,
    val email: String,
    val selected: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StremioFamilyApp() {
    var accounts by remember { mutableStateOf(listOf(
        AccountProfile(1, "Mi cuenta", ""),
        AccountProfile(2, "Padre", ""),
        AccountProfile(3, "Madre", "")
    )) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Añade perfiles y selecciona a cuáles quieres aplicar los cambios.") }
    val selectedCount = accounts.count { it.selected }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Purple,
        onBackground = Color.White, onSurface = Color.White, secondary = Mint
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Ink)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Purple, contentColor = Ink) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir cuenta")
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
                                    Text("Gestiona los perfiles familiares", color = Muted, fontSize = 12.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill("${accounts.size}", "perfiles")
                                StatPill("$selectedCount", "seleccionados")
                            }
                            Button(
                                onClick = {
                                    message = "La sincronización remota aún no está disponible. No se ha iniciado sesión ni modificado ninguna cuenta de Stremio."
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(15.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Ink)
                            ) {
                                Icon(Icons.Default.CloudSync, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Sincronizar addons", fontWeight = FontWeight.Bold)
                            }
                            Text("PROTOTIPO · NO MODIFICA CUENTAS REALES", color = Mint, fontSize = 10.sp, letterSpacing = .6.sp)
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PeopleAlt, null, tint = Purple)
                        Spacer(Modifier.width(8.dp))
                        Text("Cuentas familiares", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            val allSelected = accounts.isNotEmpty() && accounts.all { it.selected }
                            accounts = accounts.map { it.copy(selected = !allSelected) }
                        }) { Text(if (accounts.isNotEmpty() && accounts.all { it.selected }) "Deseleccionar" else "Seleccionar todas") }
                    }
                }
                items(accounts, key = { it.id }) { account ->
                    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = account.selected,
                                onCheckedChange = { value -> accounts = accounts.map { if (it.id == account.id) it.copy(selected = value) else it } },
                                colors = CheckboxDefaults.colors(checkedColor = Purple, uncheckedColor = Muted)
                            )
                            Box(Modifier.size(42.dp).background(Color(0xFF2B2B42), CircleShape), contentAlignment = Alignment.Center) {
                                Text(account.name.take(1).uppercase(), color = Purple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(account.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(account.email.ifBlank { "Correo no configurado" }, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Extension, null, tint = Muted, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Perfil local · no conectado", color = Muted, fontSize = 10.sp)
                                }
                            }
                            IconButton(onClick = { accounts = accounts.filterNot { it.id == account.id } }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar perfil", tint = Muted)
                            }
                        }
                    }
                }
                item {
                    Surface(color = Color(0xFF202A2B), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Security, null, tint = Mint)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Privacidad primero", fontWeight = FontWeight.SemiBold, color = Mint)
                                Text("Esta versión no pide ni almacena contraseñas. La autenticación real debe implementarse y verificarse antes de conectar cuentas.", color = Color(0xFFCBD5D7), fontSize = 12.sp)
                            }
                        }
                    }
                }
                item {
                    Text(message, color = Muted, fontSize = 12.sp)
                    Text("Versión 0.1.0 · Prototipo de interfaz", color = Color(0xFF555B6C), fontSize = 11.sp)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Añadir perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nombre (p. ej. Hermana)") }, singleLine = true)
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Correo de Stremio (opcional)") }, singleLine = true)
                    Text("Se guarda únicamente como ficha local; no inicia sesión en Stremio.", color = Muted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        accounts = accounts + AccountProfile((accounts.maxOfOrNull { it.id } ?: 0) + 1, newName.trim(), newEmail.trim())
                        newName = ""; newEmail = ""; showAddDialog = false
                    }
                }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun StatPill(number: String, label: String) {
    Surface(color = Color(0xFF292D3A), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(number, color = Purple, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, color = Muted, fontSize = 12.sp)
        }
    }
}