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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.DarkAppBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkCardSurfaceVariant
import com.example.ui.theme.KlantPrimary
import com.example.ui.theme.PrimaryBlueGlow
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun ClientDashboardScreen(
    viewModel: DashboardViewModel,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val myTasks by viewModel.clientTasks.collectAsState()
    val myRequests by viewModel.clientServiceRequests.collectAsState()
    val securityAlert by viewModel.securityAlert.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mijn Werken & Status (${myTasks.size})", "Mijn Aanvragen (${myRequests.size})", "Klant AI Assistent")

    var showNewRequestDialog by remember { mutableStateOf(false) }

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
                    text = "Klant Portaal",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Ingelogd: ${currentUser?.fullName ?: "Klant"} (@${currentUser?.username})",
                    color = KlantPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier.testTag("client_logout_button")
            ) {
                Text("Uitloggen", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Security level notice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCardSurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Autoriteitsniveau: KLANT (Alleen toegang tot eigen projecten en verzoeken).",
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
                    color = KlantPrimary
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
                            color = if (selectedTab == index) KlantPrimary else TextSecondary
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f).padding(12.dp)) {
            when (selectedTab) {
                0 -> ClientTasksTab(tasks = myTasks)
                1 -> ClientRequestsTab(
                    requests = myRequests,
                    onNewRequestClick = { showNewRequestDialog = true }
                )
                2 -> ClientAIAssistantTab(
                    viewModel = viewModel
                )
            }
        }
    }

    // Modal: Nieuwe Aanvraag Indienen
    if (showNewRequestDialog) {
        CreateServiceRequestDialog(
            onDismiss = { showNewRequestDialog = false },
            onConfirm = { title, desc, date, urgency ->
                viewModel.submitServiceRequest(title, desc, date, urgency) { success ->
                    if (success) showNewRequestDialog = false
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
private fun ClientTasksTab(
    tasks: List<PlanningTaskEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Lopende Werken & Status (${tasks.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary
        )
        Text(
            text = "Overzicht van planning en actuele voortgang van uw opdrachten",
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Er zijn momenteel geen actieve werkorders voor uw account.", color = TextMuted)
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
                                    text = task.clientVisibleStatus,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (task.status) {
                                        "In uitvoering" -> StatusInfo
                                        "Afgerond" -> StatusSuccess
                                        else -> StatusWarning
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = task.description, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Toegewezen Monteur: ${task.assignedWorkerName}",
                                fontSize = 12.sp,
                                color = KlantPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Geplande datum: ${task.scheduledDate} | Locatie: ${task.location}",
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
private fun ClientRequestsTab(
    requests: List<ServiceRequestEntity>,
    onNewRequestClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mijn Serviceaanvragen (${requests.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Button(
                onClick = onNewRequestClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                modifier = Modifier.testTag("client_new_request_button")
            ) {
                Text("+ Nieuwe Aanvraag", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("U heeft nog geen serviceaanvragen ingediend.", color = TextMuted)
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
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = req.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text(
                                    text = req.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (req.status) {
                                        "Voltooid" -> StatusSuccess
                                        "In Behandeling" -> StatusInfo
                                        else -> StatusWarning
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = req.description, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Gewenste datum: ${req.preferredDate} | Urgentie: ${req.urgency}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateServiceRequestDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Normaal") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardSurface,
        title = { Text("Nieuwe Serviceaanvraag", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DarkTextField(value = title, onValueChange = { title = it }, label = "Titel van de aanvraag")
                Spacer(modifier = Modifier.height(8.dp))
                DarkTextField(value = desc, onValueChange = { desc = it }, label = "Omschrijving van het probleem/verzoek")
                Spacer(modifier = Modifier.height(8.dp))
                DarkTextField(value = date, onValueChange = { date = it }, label = "Voorkeursdatum")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && desc.isNotBlank()) {
                        onConfirm(title, desc, date, urgency)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
            ) {
                Text("Indienen", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren", color = TextSecondary) }
        }
    )
}

@Composable
private fun ClientAIAssistantTab(
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
            border = BorderStroke(1.dp, KlantPrimary.copy(alpha = 0.5f)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "💬 Klant AI Assistent (Harnas 1)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Veilige klantenservice voor vragen over diensten, tarieven, garantie en uw eigen aanvragen.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    if (session?.isValid == true) {
                        Box(
                            modifier = Modifier
                                .background(StatusSuccess.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .border(1.dp, StatusSuccess, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("● Actief", color = StatusSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(StatusDanger.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .border(1.dp, StatusDanger, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
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
                                text = "Stel een vraag aan onze klantassistent:\n• 'Wat is de status van mijn lopende aanvraag?'\n• 'Welke garantie geldt er op installaties?'\n• 'Hoe vraag ik een spoedopdracht aan?'",
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
                val bg = if (isUser) Color(0xFF0F766E) else DarkCardSurfaceVariant
                val borderColor = if (isUser) KlantPrimary else DarkCardBorder

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = align
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bg),
                        border = BorderStroke(1.dp, borderColor),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "U (Klant)" else "Klantenservice AI",
                                color = if (isUser) KlantPrimary else Color(0xFF5EEAD4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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

        Spacer(modifier = Modifier.height(8.dp))

        if (isStreaming) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = KlantPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = streamStatusText.ifBlank { "Klantassistent typt..." }, color = KlantPrimary, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Input
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = { Text("Stel een vraag aan de klantassistent...", color = TextMuted, fontSize = 13.sp) },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = KlantPrimary,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkCardSurfaceVariant,
                        unfocusedContainerColor = DarkCardSurfaceVariant
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
                            streamStatusText = "Antwoord genereren..."

                            viewModel.sendHarnessPrompt(
                                category = com.example.data.ai.AIHarnessCategory.KLANT,
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
                    colors = ButtonDefaults.buttonColors(containerColor = KlantPrimary)
                ) {
                    Text("Verzend", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

