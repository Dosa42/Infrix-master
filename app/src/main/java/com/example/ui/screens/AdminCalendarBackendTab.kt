package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarEventEntity
import com.example.data.model.UserRole
import com.example.ui.theme.AdminPrimary
import com.example.ui.theme.DarkAppBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkCardSurfaceVariant
import com.example.ui.theme.DarkTextField
import com.example.ui.theme.DarkTextFieldBorder
import com.example.ui.theme.KlantPrimary
import com.example.ui.theme.PrimaryBlueGlow
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WerkerPrimary
import com.example.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminCalendarBackendTab(
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    val events by viewModel.allCalendarEvents.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    val workers = remember(allUsers) {
        allUsers.filter { it.role == UserRole.WERKER || it.role == UserRole.ADMIN }
    }
    val clients = remember(allUsers) {
        allUsers.filter { it.role == UserRole.KLANT }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedWorkerFilter by remember { mutableStateOf("Alle Werkers") }
    var selectedClientFilter by remember { mutableStateOf("Alle Klanten") }
    var selectedPriorityFilter by remember { mutableStateOf("Alle Prioriteiten") }

    var showAddDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var eventToDelete by remember { mutableStateOf<CalendarEventEntity?>(null) }
    var icsExportContent by remember { mutableStateOf<String?>(null) }
    var syncFeedbackMessage by remember { mutableStateOf<String?>(null) }

    val filteredEvents = remember(events, searchQuery, selectedWorkerFilter, selectedClientFilter, selectedPriorityFilter) {
        events.filter { event ->
            val matchesSearch = searchQuery.isBlank() ||
                event.title.contains(searchQuery, ignoreCase = true) ||
                event.description.contains(searchQuery, ignoreCase = true) ||
                event.location.contains(searchQuery, ignoreCase = true) ||
                event.workerName.contains(searchQuery, ignoreCase = true) ||
                event.clientName.contains(searchQuery, ignoreCase = true)

            val matchesWorker = selectedWorkerFilter == "Alle Werkers" ||
                event.workerUsername.equals(selectedWorkerFilter, ignoreCase = true) ||
                event.workerName.equals(selectedWorkerFilter, ignoreCase = true)

            val matchesClient = selectedClientFilter == "Alle Klanten" ||
                event.clientUsername.equals(selectedClientFilter, ignoreCase = true) ||
                event.clientName.equals(selectedClientFilter, ignoreCase = true)

            val matchesPriority = selectedPriorityFilter == "Alle Prioriteiten" ||
                event.priority.equals(selectedPriorityFilter, ignoreCase = true)

            matchesSearch && matchesWorker && matchesClient && matchesPriority
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkAppBackground),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner & Sync Engine Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueGlow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📅 Backend Agenda & Synchronisatie Engine",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Centrale verbinding tussen Werkers, Klanten, Google Maps Navigatie & Google Search Research",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Actieknoppen rij
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.syncAllToCalendar { count ->
                                    syncFeedbackMessage = "✅ Succesvol $count items synchroon gekoppeld aan de kalender!"
                                    Toast.makeText(context, "Gesynchroniseerd: $count items", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueGlow),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calendar_sync_all_button")
                        ) {
                            Text("⚡ Synchroniseer Alles", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calendar_add_event_button")
                        ) {
                            Text("+ Afspraak", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.exportCalendarIcs { ics ->
                                    icsExportContent = ics
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusInfo),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusInfo),
                            modifier = Modifier.testTag("calendar_export_ics_button")
                        ) {
                            Text(".ICS Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    syncFeedbackMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF064E3B).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .border(1.dp, StatusSuccess, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = msg, color = StatusSuccess, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // 2. Metrics & Status Kaarten
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Totaal Agenda Items",
                    value = "${events.size}",
                    color = PrimaryBlueGlow,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Werker Koppelingen",
                    value = "${events.count { it.workerUsername.isNotBlank() }}",
                    color = WerkerPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Google Maps & Search",
                    value = "${events.count { it.googleMapsUrl.isNotBlank() }}",
                    color = StatusSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Filters & Zoekbalk
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Filters & Zoekfunctie",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Zoek op taak, klant, werker of locatie...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlueGlow,
                            unfocusedBorderColor = DarkTextFieldBorder,
                            focusedContainerColor = DarkTextField,
                            unfocusedContainerColor = DarkTextField
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Worker filter dropdown
                        DropdownFilter(
                            label = "Werker",
                            selectedOption = selectedWorkerFilter,
                            options = listOf("Alle Werkers") + workers.map { it.fullName.ifBlank { it.username } },
                            onSelect = { selectedWorkerFilter = it },
                            modifier = Modifier.weight(1f)
                        )

                        // Client filter dropdown
                        DropdownFilter(
                            label = "Klant",
                            selectedOption = selectedClientFilter,
                            options = listOf("Alle Klanten") + clients.map { it.fullName.ifBlank { it.username } },
                            onSelect = { selectedClientFilter = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Lijst met Kalender Afspraken
        if (filteredEvents.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (events.isEmpty()) "Geen kalender-afspraken aanwezig" else "Geen afspraken die voldoen aan de zoekfilters",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Klik op '⚡ Synchroniseer Alles' om alle bestaande taken & aanvragen direct in te laden.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(filteredEvents, key = { it.id }) { event ->
                CalendarEventCard(
                    event = event,
                    onOpenGoogleCalendar = {
                        try {
                            val intent = viewModel.getGoogleCalendarIntent(event)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Agenda openen mislukt: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenGoogleMaps = {
                        try {
                            val intent = viewModel.getGoogleMapsRouteIntent(event.location.ifBlank { "Nederland" })
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Google Maps openen mislukt: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenGoogleSearch = {
                        try {
                            val query = event.googleSearchQuery.ifBlank { "${event.title} ${event.clientName}" }
                            val intent = viewModel.getGoogleSearchIntent(query)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Google Search openen mislukt: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEdit = { eventToEdit = event },
                    onDelete = { eventToDelete = event }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal: Nieuwe Afspraak Toevoegen
    if (showAddDialog) {
        AddEditCalendarEventDialog(
            existingEvent = null,
            workers = workers,
            clients = clients,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, date, start, end, loc, wUser, wName, cUser, cName, prio, color ->
                viewModel.createCalendarEvent(
                    title = title,
                    description = desc,
                    eventDate = date,
                    startTime = start,
                    endTime = end,
                    location = loc,
                    workerUsername = wUser,
                    workerName = wName,
                    clientUsername = cUser,
                    clientName = cName,
                    priority = prio,
                    calendarColorHex = color
                ) { ok ->
                    if (ok) {
                        showAddDialog = false
                        Toast.makeText(context, "Afspraak opgeslagen en gesynchroniseerd!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Modal: Afspraak Bewerken
    eventToEdit?.let { event ->
        AddEditCalendarEventDialog(
            existingEvent = event,
            workers = workers,
            clients = clients,
            onDismiss = { eventToEdit = null },
            onConfirm = { title, desc, date, start, end, loc, wUser, wName, cUser, cName, prio, color ->
                val updated = event.copy(
                    title = title,
                    description = desc,
                    eventDate = date,
                    startTime = start,
                    endTime = end,
                    location = loc,
                    workerUsername = wUser,
                    workerName = wName,
                    clientUsername = cUser,
                    clientName = cName,
                    priority = prio,
                    calendarColorHex = color
                )
                viewModel.updateCalendarEvent(updated) { ok ->
                    if (ok) {
                        eventToEdit = null
                        Toast.makeText(context, "Afspraak bijgewerkt!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Modal: Verwijderen Bevestiging
    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            containerColor = DarkCardSurface,
            title = {
                Text("Afspraak Verwijderen", color = StatusDanger, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Weet u zeker dat u '${event.title}' van datum ${event.eventDate} wilt verwijderen uit de backend kalender?",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCalendarEvent(event.id) { ok ->
                            eventToDelete = null
                            if (ok) Toast.makeText(context, "Afspraak verwijderd", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                ) {
                    Text("Verwijderen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Annuleren", color = TextSecondary)
                }
            }
        )
    }

    // Modal: .ICS Export Preview
    icsExportContent?.let { ics ->
        AlertDialog(
            onDismissRequest = { icsExportContent = null },
            containerColor = DarkCardSurface,
            title = {
                Text("📅 iCalendar (.ICS) Export", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Dit iCalendar formaat (RFC 5545) kan universeel worden geïmporteerd in Google Calendar, Apple Agenda, Microsoft Outlook en Mozilla Thunderbird.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = ics,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/calendar"
                                putExtra(Intent.EXTRA_SUBJECT, "RoleVault Agenda Export.ics")
                                putExtra(Intent.EXTRA_TEXT, ics)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Deel of exporteer .ICS kalender"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Delen mislukt: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        icsExportContent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueGlow)
                ) {
                    Text("Deel / Exporteer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { icsExportContent = null }) {
                    Text("Sluiten", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun CalendarEventCard(
    event: CalendarEventEntity,
    onOpenGoogleCalendar: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    onOpenGoogleSearch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = when (event.priority.lowercase(Locale.ROOT)) {
        "urgent" -> StatusDanger
        "hoog" -> StatusWarning
        else -> PrimaryBlueGlow.copy(alpha = 0.5f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Titel + Datum & Tijd badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 ${event.eventDate}",
                            color = PrimaryBlueGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "⏰ ${event.startTime} - ${event.endTime}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Prioriteit badge
                Box(
                    modifier = Modifier
                        .background(
                            when (event.priority.lowercase(Locale.ROOT)) {
                                "urgent" -> StatusDanger.copy(alpha = 0.2f)
                                "hoog" -> StatusWarning.copy(alpha = 0.2f)
                                else -> PrimaryBlueGlow.copy(alpha = 0.2f)
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = event.priority,
                        color = when (event.priority.lowercase(Locale.ROOT)) {
                            "urgent" -> StatusDanger
                            "hoog" -> StatusWarning
                            else -> PrimaryBlueGlow
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = event.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Chips: Werker, Klant, Locatie
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (event.workerName.isNotBlank() || event.workerUsername.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👨‍🔧 Toegewezen Werker: ", color = TextMuted, fontSize = 12.sp)
                        Text(
                            text = event.workerName.ifBlank { event.workerUsername },
                            color = WerkerPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (event.clientName.isNotBlank() || event.clientUsername.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏢 Klant / Relatie: ", color = TextMuted, fontSize = 12.sp)
                        Text(
                            text = event.clientName.ifBlank { event.clientUsername },
                            color = KlantPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (event.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📍 Locatie: ", color = TextMuted, fontSize = 12.sp)
                        Text(
                            text = event.location,
                            color = TextPrimary,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: 1. Google Maps, 2. Google Search, 3. Google Calendar, 4. Bewerken/Verwijderen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Google Maps knop
                Button(
                    onClick = onOpenGoogleMaps,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("event_google_maps_button_${event.id}")
                ) {
                    Text("🗺️ Maps Route", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Google Search knop
                Button(
                    onClick = onOpenGoogleSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("event_google_search_button_${event.id}")
                ) {
                    Text("🔍 Search Info", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Google Calendar knop
                Button(
                    onClick = onOpenGoogleCalendar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("event_google_calendar_button_${event.id}")
                ) {
                    Text("📅 Naar Agenda", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Text("✏️ Bewerken", color = StatusInfo, fontSize = 12.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("🗑️ Verwijderen", color = StatusDanger, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCalendarEventDialog(
    existingEvent: CalendarEventEntity?,
    workers: List<com.example.data.model.UserEntity>,
    clients: List<com.example.data.model.UserEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        desc: String,
        date: String,
        startTime: String,
        endTime: String,
        location: String,
        workerUsername: String,
        workerName: String,
        clientUsername: String,
        clientName: String,
        priority: String,
        colorHex: String
    ) -> Unit
) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var description by remember { mutableStateOf(existingEvent?.description ?: "") }
    var date by remember { mutableStateOf(existingEvent?.eventDate ?: today) }
    var startTime by remember { mutableStateOf(existingEvent?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(existingEvent?.endTime ?: "11:00") }
    var location by remember { mutableStateOf(existingEvent?.location ?: "Keizersgracht 421, Amsterdam") }

    var selectedWorkerUsername by remember { mutableStateOf(existingEvent?.workerUsername ?: if (workers.isNotEmpty()) workers.first().username else "") }
    var selectedWorkerName by remember { mutableStateOf(existingEvent?.workerName ?: if (workers.isNotEmpty()) workers.first().fullName else "") }

    var selectedClientUsername by remember { mutableStateOf(existingEvent?.clientUsername ?: if (clients.isNotEmpty()) clients.first().username else "") }
    var selectedClientName by remember { mutableStateOf(existingEvent?.clientName ?: if (clients.isNotEmpty()) clients.first().fullName else "") }

    var priority by remember { mutableStateOf(existingEvent?.priority ?: "Normaal") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = {
            Text(
                text = if (existingEvent == null) "📅 Nieuwe Kalender Afspraak" else "✏️ Afspraak Bewerken",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel / Onderwerp *", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlueGlow,
                        unfocusedBorderColor = DarkTextFieldBorder,
                        focusedContainerColor = DarkTextField,
                        unfocusedContainerColor = DarkTextField
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_calendar_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Omschrijving & Werkzaamheden", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlueGlow,
                        unfocusedBorderColor = DarkTextFieldBorder,
                        focusedContainerColor = DarkTextField,
                        unfocusedContainerColor = DarkTextField
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Datum (jjjj-mm-dd) *", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlueGlow,
                            unfocusedBorderColor = DarkTextFieldBorder,
                            focusedContainerColor = DarkTextField,
                            unfocusedContainerColor = DarkTextField
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Van (uu:mm)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlueGlow,
                            unfocusedBorderColor = DarkTextFieldBorder,
                            focusedContainerColor = DarkTextField,
                            unfocusedContainerColor = DarkTextField
                        ),
                        modifier = Modifier.weight(0.6f)
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Tot (uu:mm)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlueGlow,
                            unfocusedBorderColor = DarkTextFieldBorder,
                            focusedContainerColor = DarkTextField,
                            unfocusedContainerColor = DarkTextField
                        ),
                        modifier = Modifier.weight(0.6f)
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Locatie / Adres voor Google Maps *", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlueGlow,
                        unfocusedBorderColor = DarkTextFieldBorder,
                        focusedContainerColor = DarkTextField,
                        unfocusedContainerColor = DarkTextField
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Selectie Toegewezen Werker
                Text("👨‍🔧 Toegewezen Werker:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    workers.take(3).forEach { worker ->
                        val isSelected = selectedWorkerUsername == worker.username
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) WerkerPrimary.copy(alpha = 0.2f) else DarkTextField,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) WerkerPrimary else DarkTextFieldBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedWorkerUsername = worker.username
                                    selectedWorkerName = worker.fullName.ifBlank { worker.username }
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = worker.fullName.ifBlank { worker.username },
                                color = if (isSelected) WerkerPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Selectie Klant
                Text("🏢 Gekoppelde Klant:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    clients.take(3).forEach { client ->
                        val isSelected = selectedClientUsername == client.username
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) KlantPrimary.copy(alpha = 0.2f) else DarkTextField,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) KlantPrimary else DarkTextFieldBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedClientUsername = client.username
                                    selectedClientName = client.fullName.ifBlank { client.username }
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = client.fullName.ifBlank { client.username },
                                color = if (isSelected) KlantPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Prioriteit
                Text("Urgentie / Prioriteit:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Normaal", "Hoog", "Urgent").forEach { prio ->
                        val isSelected = priority == prio
                        val color = when (prio) {
                            "Urgent" -> StatusDanger
                            "Hoog" -> StatusWarning
                            else -> PrimaryBlueGlow
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) color.copy(alpha = 0.2f) else DarkTextField,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) color else DarkTextFieldBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { priority = prio }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio,
                                color = if (isSelected) color else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            title,
                            description,
                            date,
                            startTime,
                            endTime,
                            location,
                            selectedWorkerUsername,
                            selectedWorkerName,
                            selectedClientUsername,
                            selectedClientName,
                            priority,
                            "#38BDF8"
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueGlow),
                modifier = Modifier.testTag("dialog_calendar_submit_button")
            ) {
                Text(
                    text = if (existingEvent == null) "Afspraak Inplannen" else "Wijzigingen Opslaan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuleren", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(
    label: String,
    selectedOption: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextMuted, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryBlueGlow,
                unfocusedBorderColor = DarkTextFieldBorder,
                focusedContainerColor = DarkTextField,
                unfocusedContainerColor = DarkTextField
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkCardSurface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextPrimary, fontSize = 12.sp) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
