package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backend.CloudSyncStatus
import com.example.data.backend.DevicePermissionStatus
import com.example.data.backend.GeoLocationData
import com.example.data.backend.SearchResultItem
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun AdminBackendToolsTab(
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    var permissionsList by remember { mutableStateOf<List<DevicePermissionStatus>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<GeoLocationData?>(null) }

    // Multi-Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionsList = viewModel.getHardwarePermissionsStatus()
        currentLocation = viewModel.getDeviceGpsLocation()
    }

    LaunchedEffect(Unit) {
        permissionsList = viewModel.getHardwarePermissionsStatus()
        currentLocation = viewModel.getDeviceGpsLocation()
    }

    // Google Maps State
    var mapDestination by remember { mutableStateOf("Keizersgracht 421, Amsterdam") }
    var routeOriginLat by remember { mutableStateOf("52.3676") }
    var routeOriginLon by remember { mutableStateOf("4.9041") }
    var routeDestLat by remember { mutableStateOf("52.0907") }
    var routeDestLon by remember { mutableStateOf("5.1214") }
    var calculatedRouteInfo by remember { mutableStateOf<String?>(null) }

    // E-mail State
    var emailRecipient by remember { mutableStateOf("werker@bedrijf.local") }
    var emailSubject by remember { mutableStateOf("Planning & Werkorder Instructie") }
    var emailBody by remember { mutableStateOf("Beste collega, hierbij de actuele werkordergegevens voor de komende opdracht.") }
    var emailStatusMsg by remember { mutableStateOf<String?>(null) }

    // Search State
    var searchQuery by remember { mutableStateOf("NEN 1010 elektra richtlijnen monteurs") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }

    // Cloud / Server Storage State
    var serverEndpoint by remember { mutableStateOf("https://cloud.rolevault.internal/api/v1/sync") }
    var cloudSyncStatus by remember { mutableStateOf<CloudSyncStatus?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var rawJsonDump by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -------------------------------------------------------------
        // 1. HARDWARE CAPABILITIES & PERMISSIONS CONSOLE
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "1. Device & Android Permissions Hub",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Camera, Microfoon, GPS/Locatie, Telefoon & Meldingen",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                    Manifest.permission.CALL_PHONE,
                                    Manifest.permission.READ_PHONE_STATE
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Vraag Alle Toestemmingen", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Permissions List Display
                permissionsList.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                p.permission.contains("LOCATION") -> Icons.Default.LocationOn
                                p.permission.contains("CAMERA") -> Icons.Default.CameraAlt
                                p.permission.contains("AUDIO") -> Icons.Default.Mic
                                p.permission.contains("NOTIFICATIONS") -> Icons.Default.Notifications
                                else -> Icons.Default.Phone
                            },
                            contentDescription = null,
                            tint = if (p.isGranted) Color(0xFF27AE60) else Color(0xFFE67E22),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = p.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = p.description, fontSize = 10.sp, color = Color.Gray)
                        }
                        Text(
                            text = if (p.isGranted) "Toegekend" else "Vereist actie",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (p.isGranted) Color(0xFF27AE60) else Color(0xFFC0392B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(8.dp))

                // Live Location Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Huidige GPS Telemetrie:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val loc = currentLocation
                        if (loc != null) {
                            Text(
                                text = "Lat: ${loc.latitude} | Lon: ${loc.longitude} (${loc.provider})",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            loc.readableAddress?.let { addr ->
                                Text(text = "Adres: $addr", fontSize = 11.sp, color = Color(0xFF0F4C81), fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text(text = "Locatie wordt opgehaald of toestemming vereist.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            currentLocation = viewModel.getDeviceGpsLocation()
                            permissionsList = viewModel.getHardwarePermissionsStatus()
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Ververs GPS", fontSize = 11.sp)
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. GOOGLE MAPS & NAVIGATION BACKEND
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, tint = Color(0xFFEA4335))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2. Google Maps & Navigatie Integratie",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Text(
                    text = "Start Google Maps navigatie, bekijk werklocaties en bereken afstanden",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mapDestination,
                    onValueChange = { mapDestination = it },
                    label = { Text("Bestemming (Adres, Klantlocatie of Coördinaten)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.openGoogleMapsNavigation(mapDestination) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Navigatie", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.showMapLocation(mapDestination) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Toon op Kaart", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Coordinate Distance Calculator
                Text(text = "Coördinaten Afstandscalculator:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = routeOriginLat,
                        onValueChange = { routeOriginLat = it },
                        label = { Text("Start Lat") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = routeOriginLon,
                        onValueChange = { routeOriginLon = it },
                        label = { Text("Start Lon") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = routeDestLat,
                        onValueChange = { routeDestLat = it },
                        label = { Text("Doel Lat") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = routeDestLon,
                        onValueChange = { routeDestLon = it },
                        label = { Text("Doel Lon") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val lat1 = routeOriginLat.toDoubleOrNull() ?: 52.3676
                        val lon1 = routeOriginLon.toDoubleOrNull() ?: 4.9041
                        val lat2 = routeDestLat.toDoubleOrNull() ?: 52.0907
                        val lon2 = routeDestLon.toDoubleOrNull() ?: 5.1214
                        val res = viewModel.calculateGpsDistance(lat1, lon1, lat2, lon2)
                        calculatedRouteInfo = "Hemelsbrede afstand: ${res.directDistanceKm} km | Geschatte reistijd: ~${res.estimatedDriveMinutes} min"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bereken Afstand & Reistijd", fontSize = 12.sp)
                }

                calculatedRouteInfo?.let { info ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = info, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF27AE60))
                }
            }
        }

        // -------------------------------------------------------------
        // 3. E-MAIL BACKEND INTEGRATIE
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF2B5797))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3. E-mail Backend & Notificatie Dispatcher",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Text(
                    text = "Systeemnotificaties, accountgegevens en planningen per e-mail versturen",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Snelle Templates
                Text(text = "Snel sjabloon laden:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val (sub, bod) = viewModel.generateWelcomeEmailTemplate("werker1", "Jan Jansen", "Werker", "tijdelijk123")
                            emailSubject = sub
                            emailBody = bod
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Account Template", fontSize = 10.sp) }

                    OutlinedButton(
                        onClick = {
                            val (sub, bod) = viewModel.generateTaskEmailTemplate("Jan Jansen", "Reparatie Hoofdleiding", "2026-10-05", "Utrecht")
                            emailSubject = sub
                            emailBody = bod
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Taak Template", fontSize = 10.sp) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emailRecipient,
                    onValueChange = { emailRecipient = it },
                    label = { Text("Ontvanger E-mailadres") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = emailSubject,
                    onValueChange = { emailSubject = it },
                    label = { Text("Onderwerp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = emailBody,
                    onValueChange = { emailBody = it },
                    label = { Text("Inhoud / Body") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val res = viewModel.sendNativeEmail(emailRecipient, emailSubject, emailBody)
                        emailStatusMsg = if (res.isSuccess) "E-mail verzendopdracht succesvol aangeboden aan mailclient." else res.message
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B5797)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verstuur Systeem E-mail", fontSize = 13.sp)
                }

                emailStatusMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = msg, fontSize = 12.sp, color = Color(0xFF27AE60), fontWeight = FontWeight.Medium)
                }
            }
        }

        // -------------------------------------------------------------
        // 4. GOOGLE & WEB SEARCH INTEGRATIE
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF0F9D58))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "4. Google & Web Search Integratie",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Text(
                    text = "Live zoekfunctie en documentatie opzoeken voor monteurs en beheerders",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Zoekopdracht (Google / Kennisbank)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isSearching = true
                            viewModel.executeWebSearch(searchQuery) { results ->
                                isSearching = false
                                searchResults = results
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), color = Color.White)
                        } else {
                            Text("In-App Zoeken", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.launchGoogleSearch(searchQuery) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Google Direct", fontSize = 12.sp)
                    }
                }

                // Results list
                if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Gevonden Resultaten (${searchResults.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    searchResults.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                                Text(text = item.snippet, fontSize = 11.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Bron: ${item.source}", fontSize = 10.sp, color = Color.Gray)
                                    TextButton(
                                        onClick = {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(browserIntent)
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text("Open Link", fontSize = 11.sp, color = Color(0xFF2563EB))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 5. SERVER SIDE STORAGE & CLOUD SYNCHRONISATIE
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF7C3AED))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5. Server Side Storage & Cloud Sync",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Text(
                    text = "Complete database back-up, server synchronisatie en JSON exports",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = serverEndpoint,
                    onValueChange = { serverEndpoint = it },
                    label = { Text("Server Synchronisatie Endpoint") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isSyncing = true
                            viewModel.syncDatabaseToServer(serverEndpoint) { status ->
                                isSyncing = false
                                cloudSyncStatus = status
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), color = Color.White)
                        } else {
                            Text("Start Cloud Sync", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.exportCompleteDatabaseJson { json ->
                                rawJsonDump = json
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export JSON Dump", fontSize = 12.sp)
                    }
                }

                cloudSyncStatus?.let { status ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Status: ${status.syncState}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                            Text(text = "Dataomvang: ${status.backupPayloadSizeKb} KB | Endpoint: ${status.serverEndpoint}", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // JSON Dump Viewer Modal
    rawJsonDump?.let { json ->
        AlertDialog(
            onDismissRequest = { rawJsonDump = null },
            title = { Text("Database JSON Backup Payload", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = json,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1E293B)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { rawJsonDump = null }) { Text("Sluiten") }
            }
        )
    }
}
