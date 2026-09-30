package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import com.example.data.model.WorkLogEntity
import com.example.ui.theme.DarkAppBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkCardSurfaceVariant
import com.example.ui.theme.DarkTextField
import com.example.ui.theme.DarkTextFieldBorder
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

@Composable
fun WorkerDashboardScreen(
    viewModel: DashboardViewModel,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val myTasks by viewModel.workerTasks.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allServiceRequests by viewModel.allServiceRequests.collectAsState()
    val myLogs by viewModel.workerWorkLogs.collectAsState()
    val securityAlert by viewModel.securityAlert.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mijn Planning (${myTasks.size})", "Werker AI Co-Pilot", "Algemene Planning", "Klantverzoeken", "Urenregistratie")

    var selectedTaskForLog by remember { mutableStateOf<PlanningTaskEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkAppBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Werker Dashboard",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Ingelogd: ${currentUser?.fullName ?: "Werker"} (@${currentUser?.username})",
                    color = WerkerPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("worker_logout_button")
            ) {
                Text("Uitloggen", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Authority notice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCardSurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Autoriteitsniveau: WERKER (Toegang tot operationele planning & uren).",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF0F172A),
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = WerkerPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) WerkerPrimary else TextSecondary
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f).padding(12.dp)) {
            when (selectedTab) {
                0 -> WorkerMyTasksTab(
                    tasks = myTasks,
                    viewModel = viewModel,
                    onStatusChange = { taskId, status -> viewModel.updateTaskStatus(taskId, status) },
                    onLogHoursClick = { task -> selectedTaskForLog = task }
                )
                1 -> WorkerAICoPilotTab(
                    viewModel = viewModel
                )
                2 -> WorkerAllTasksTab(tasks = allTasks)
                3 -> WorkerServiceRequestsTab(
                    requests = allServiceRequests,
                    onUpdateStatus = { reqId, status -> viewModel.updateRequestStatus(reqId, status) }
                )
                4 -> WorkerWorkLogsTab(logs = myLogs)
            }
        }
    }

    // Modal: Uren Boeken
    selectedTaskForLog?.let { task ->
        LogHoursDialog(
            task = task,
            onDismiss = { selectedTaskForLog = null },
            onConfirm = { hours, desc ->
                viewModel.logHours(task.id, task.title, hours, desc) {
                    selectedTaskForLog = null
                }
            }
        )
    }

    // Security Alert
    securityAlert?.let { alert ->
        AccessDeniedDialog(alertData = alert, onDismiss = { viewModel.dismissSecurityAlert() })
    }
}

@Composable
private fun WorkerMyTasksTab(
    tasks: List<PlanningTaskEntity>,
    viewModel: DashboardViewModel,
    onStatusChange: (Long, String) -> Unit,
    onLogHoursClick: (PlanningTaskEntity) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Toegewezen Taken aan Mij (${tasks.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Geen actieve taken toegewezen aan uw account.", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = task.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = task.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (task.status) {
                                        "In uitvoering" -> StatusInfo
                                        "Afgerond" -> StatusSuccess
                                        "Gepauzeerd" -> StatusWarning
                                        else -> PrimaryBlueGlow
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = task.description, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Klant: ${task.clientName} | Locatie: ${task.location}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "Datum: ${task.scheduledDate} | Geschat: ${task.estimatedHours}u | Gewerkt: ${task.actualHours}u",
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Google Maps & Google Search 1-click Navigatie
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = viewModel.getGoogleMapsRouteIntent(task.location.ifBlank { "Nederland" })
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Maps openen mislukt", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🗺️ Maps Route", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val intent = viewModel.getGoogleSearchIntent("${task.title} ${task.location}")
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Search openen mislukt", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🔍 Search Info", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = DarkCardBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { onStatusChange(task.id, "In uitvoering") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("Start", fontSize = 11.sp, color = TextPrimary) }
                                    OutlinedButton(
                                        onClick = { onStatusChange(task.id, "Gepauzeerd") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("Pauze", fontSize = 11.sp, color = TextPrimary) }
                                    OutlinedButton(
                                        onClick = { onStatusChange(task.id, "Afgerond") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("Afronden", fontSize = 11.sp, color = TextPrimary) }
                                }

                                Button(
                                    onClick = { onLogHoursClick(task) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("+ Uren Boeken", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerAllTasksTab(
    tasks: List<PlanningTaskEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Algemeen Planning Overzicht (${tasks.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Geen planningstaken gevonden.", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = task.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = task.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (task.status) {
                                        "In uitvoering" -> StatusInfo
                                        "Afgerond" -> StatusSuccess
                                        else -> StatusWarning
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Toegewezen aan: ${task.assignedWorkerName} | Klant: ${task.clientName}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Datum: ${task.scheduledDate} | Locatie: ${task.location}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerServiceRequestsTab(
    requests: List<ServiceRequestEntity>,
    onUpdateStatus: (Long, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Ingediende Klantaanvragen (${requests.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Geen openstaande serviceaanvragen.", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests, key = { it.id }) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = req.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = req.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (req.status) {
                                        "In Behandeling" -> StatusInfo
                                        "Voltooid" -> StatusSuccess
                                        else -> StatusWarning
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = req.description, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Klant: ${req.clientName} | Gewenste datum: ${req.preferredDate} | Urgentie: ${req.urgency}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = DarkCardBorder)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(req.id, "In Behandeling") },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("In Behandeling", fontSize = 11.sp, color = TextPrimary) }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onUpdateStatus(req.id, "Voltooid") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("Voltooid", fontSize = 11.sp, color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerWorkLogsTab(
    logs: List<WorkLogEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Mijn Geregistreerde Werkuren (${logs.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        val total = logs.sumOf { it.hoursSpent }
        Text(
            text = "Totaal geboekt op dit account: $total uur",
            fontSize = 13.sp,
            color = WerkerPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nog geen werkuren geboekt.", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.taskTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${log.hoursSpent} uur",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PrimaryBlueGlow
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = log.activityDescription, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogHoursDialog(
    task: PlanningTaskEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var hoursStr by remember { mutableStateOf("1.0") }
    var activityDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = { Text("Uren Boeken: ${task.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DarkTextField(value = hoursStr, onValueChange = { hoursStr = it }, label = "Aantal uren gewerkt (bijv. 2.5)")
                Spacer(modifier = Modifier.height(6.dp))
                DarkTextField(value = activityDesc, onValueChange = { activityDesc = it }, label = "Uitgevoerde werkzaamheden")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = hoursStr.toDoubleOrNull() ?: 1.0
                    if (activityDesc.isNotBlank()) {
                        onConfirm(hours, activityDesc.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Boeken", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren", color = TextSecondary) }
        }
    )
}

@Composable
private fun WorkerAICoPilotTab(
    viewModel: DashboardViewModel
) {
    val session by viewModel.chatGPTSession.collectAsState()
    val chatMessages = remember { androidx.compose.runtime.mutableStateListOf<com.example.data.auth.ChatMessage>() }
    var promptInput by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamStatusText by remember { mutableStateOf("") }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkAppBackground)
    ) {
        // Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
            border = BorderStroke(1.dp, WerkerPrimary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🛠️ Werker AI Co-Pilot (Harnas 2)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Deterministische ondersteuning voor werkorders, NEN-veiligheid, materiaaladvies en probleemoplossing.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (session?.isValid == true) {
                        Box(
                            modifier = Modifier
                                .background(StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .border(1.dp, StatusSuccess, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("● Actief", color = StatusSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(StatusDanger.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .border(1.dp, StatusDanger, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("● Geen Token", color = StatusDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Vraag uw AI Co-Pilot om technische hulp:\n• 'Wat is het aansluitschema voor NEN 1010?'\n• 'Hoe los ik foutcode E-04 op bij een omvormer?'\n• 'Wat zijn de veiligheidsstappen voor mijn huidige taak?'",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            items(chatMessages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                val align = if (isUser) Alignment.End else Alignment.Start
                val bg = if (isUser) Color(0xFF0369A1) else DarkCardSurfaceVariant
                val borderColor = if (isUser) WerkerPrimary else DarkCardBorder

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = align
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bg),
                        border = BorderStroke(1.dp, borderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "U (Monteur)" else "Werker Co-Pilot",
                                color = if (isUser) WerkerPrimary else Color(0xFF67E8F9),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (!isUser && msg.text.contains("```tool_call")) {
                                RichAIToolMessageView(fullText = msg.text)
                            } else {
                                Text(
                                    text = msg.text.ifBlank { "..." },
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isStreaming) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = WerkerPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = streamStatusText.ifBlank { "Co-Pilot denkt na..." }, color = WerkerPrimary, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Input
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("Vraag aan Werker Co-Pilot...", color = TextMuted, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = WerkerPrimary,
                        unfocusedBorderColor = DarkTextFieldBorder,
                        focusedContainerColor = DarkTextField,
                        unfocusedContainerColor = DarkTextField
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (promptInput.isNotBlank() && !isStreaming) {
                            val userText = promptInput
                            promptInput = ""
                            val userMsg = com.example.data.auth.ChatMessage(role = "user", text = userText)
                            chatMessages.add(userMsg)

                            val assistantMsgId = java.util.UUID.randomUUID().toString()
                            chatMessages.add(com.example.data.auth.ChatMessage(id = assistantMsgId, role = "assistant", text = ""))

                            isStreaming = true
                            streamStatusText = "Co-Pilot raadplegen..."

                            viewModel.sendHarnessPrompt(
                                category = com.example.data.ai.AIHarnessCategory.WERKER,
                                messages = chatMessages.dropLast(1),
                                userPrompt = userText,
                                onChunk = { chunk ->
                                    val idx = chatMessages.indexOfFirst { it.id == assistantMsgId }
                                    if (idx != -1) {
                                        val existing = chatMessages[idx]
                                        chatMessages[idx] = existing.copy(text = existing.text + chunk)
                                    }
                                },
                                onStatus = { status ->
                                    streamStatusText = status
                                },
                                onComplete = { success, finalOutput ->
                                    isStreaming = false
                                    streamStatusText = ""
                                    if (!success) {
                                        val idx = chatMessages.indexOfFirst { it.id == assistantMsgId }
                                        if (idx != -1) {
                                            chatMessages[idx] = com.example.data.auth.ChatMessage(
                                                id = assistantMsgId,
                                                role = "assistant",
                                                text = "⚠️ $finalOutput"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WerkerPrimary)
                ) {
                    Text("Vraag", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

