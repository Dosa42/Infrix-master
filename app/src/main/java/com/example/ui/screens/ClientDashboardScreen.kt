package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
    val tabs = listOf("Mijn Werken & Status", "Mijn Aanvragen")

    var showNewRequestDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F4))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F766E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Klant Portaal",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Ingelogd: ${currentUser?.fullName ?: "Klant"} (Beperkte leesrechten)",
                    color = Color(0xFFCCFBF1),
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                modifier = Modifier.testTag("client_logout_button")
            ) {
                Text("Uitloggen", fontSize = 12.sp)
            }
        }

        // Security level notice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE6FFFA))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Autoriteitsniveau: KLANT (Beperkte leesrechten. U kunt alleen uw eigen status inzien).",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0D5F58)
            )
        }

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
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
            }
        }
    }

    // Modal: Aanvraag indienen
    if (showNewRequestDialog) {
        CreateServiceRequestDialog(
            onDismiss = { showNewRequestDialog = false },
            onConfirm = { title, desc, date, urg ->
                viewModel.submitServiceRequest(title, desc, date, urg) { ok ->
                    if (ok) showNewRequestDialog = false
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
            text = "Mijn Opdrachten & Voortgang (${tasks.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("U heeft momenteel geen actieve werkorders.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = task.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (task.status) {
                                        "In uitvoering" -> Color(0xFF0F766E)
                                        "Afgerond" -> Color(0xFF27AE60)
                                        else -> Color(0xFFD97706)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = task.description, fontSize = 13.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Huidige status: ${task.clientVisibleStatus}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F766E)
                            )
                            Text(
                                text = "Toegewezen Monteur: ${task.assignedWorkerName}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Geplande datum: ${task.scheduledDate}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Alleen-lezen weergave",
                                fontSize = 11.sp,
                                color = Color.Gray
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
                text = "Mijn Ingediende Aanvragen (${requests.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Button(
                onClick = onNewRequestClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                modifier = Modifier.testTag("client_new_request_button")
            ) {
                Text("+ Aanvraag Indienen", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("U heeft nog geen serviceaanvragen ingediend.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests, key = { it.id }) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = req.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = req.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F766E)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = req.description, fontSize = 13.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Gewenste datum: ${req.preferredDate} | Urgentie: ${req.urgency}", fontSize = 12.sp, color = Color.Gray)
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
        title = { Text("Nieuwe Serviceaanvraag", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel van de aanvraag") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Omschrijving van het probleem/verzoek") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Voorkeursdatum") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && desc.isNotBlank()) {
                        onConfirm(title, desc, date, urgency)
                    }
                }
            ) {
                Text("Indienen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
