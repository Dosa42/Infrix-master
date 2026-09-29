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
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.WorkLogEntity
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
    val tabs = listOf("Mijn Planning", "Algemene Planning", "Klantverzoeken", "Urenregistratie")

    var selectedTaskForLog by remember { mutableStateOf<PlanningTaskEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F4))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F4C81))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Werker Dashboard",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Ingelogd: ${currentUser?.fullName ?: "Werker"} (Toegang planning)",
                    color = Color(0xFFD0E1F9),
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                modifier = Modifier.testTag("worker_logout_button")
            ) {
                Text("Uitloggen", fontSize = 12.sp)
            }
        }

        // Authority notice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3EDF7))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Autoriteitsniveau: WERKER (Toegang tot operationele planning & uren).",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B365D)
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
                0 -> WorkerMyTasksTab(
                    tasks = myTasks,
                    onStatusChange = { taskId, status -> viewModel.updateTaskStatus(taskId, status) },
                    onLogHoursClick = { task -> selectedTaskForLog = task }
                )
                1 -> WorkerAllTasksTab(tasks = allTasks)
                2 -> WorkerServiceRequestsTab(
                    requests = allServiceRequests,
                    onUpdateStatus = { reqId, status -> viewModel.updateRequestStatus(reqId, status) }
                )
                3 -> WorkerWorkLogsTab(logs = myLogs)
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
    onStatusChange: (Long, String) -> Unit,
    onLogHoursClick: (PlanningTaskEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Toegewezen Taken aan Mij (${tasks.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Geen actieve taken toegewezen aan uw account.", color = Color.Gray)
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
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = task.status,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when (task.status) {
                                        "In uitvoering" -> Color(0xFF2980B9)
                                        "Afgerond" -> Color(0xFF27AE60)
                                        else -> Color(0xFFE67E22)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = task.description, fontSize = 13.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Klant: ${task.clientName} | Locatie: ${task.location}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Datum: ${task.scheduledDate} | Geschat: ${task.estimatedHours}u | Gewerkt: ${task.actualHours}u",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { onStatusChange(task.id, "In uitvoering") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("Start", fontSize = 11.sp) }
                                    OutlinedButton(
                                        onClick = { onStatusChange(task.id, "Gepauzeerd") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("Pauze", fontSize = 11.sp) }
                                    OutlinedButton(
                                        onClick = { onStatusChange(task.id, "Afgerond") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("Afronden", fontSize = 11.sp) }
                                }

                                Button(
                                    onClick = { onLogHoursClick(task) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F4C81)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+ Uren Boeken", fontSize = 11.sp)
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
private fun WorkerAllTasksTab(tasks: List<PlanningTaskEntity>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Algemeen Planningsoverzicht (${tasks.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Overzicht van alle lopende projecten binnen het team",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Geen projecten aanwezig in de planning.", color = Color.Gray)
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
                                Text(text = task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = task.status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(text = "Werker: ${task.assignedWorkerName} | Klant: ${task.clientName}", fontSize = 12.sp, color = Color.Gray)
                            Text(text = "Datum: ${task.scheduledDate}", fontSize = 12.sp, color = Color.Gray)
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
            text = "Ingekomen Klantaanvragen (${requests.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Geen openstaande klantaanvragen.", color = Color.Gray)
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
                                Text(text = req.status, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2B5797))
                            }
                            Text(text = req.description, fontSize = 13.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Klant: ${req.clientName} | Voorkeursdatum: ${req.preferredDate}", fontSize = 12.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(req.id, "In Planning") },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("In Planning Nemen", fontSize = 11.sp) }

                                OutlinedButton(
                                    onClick = { onUpdateStatus(req.id, "Voltooid") },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("Afhandelen", fontSize = 11.sp) }
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
            text = "Geboekte Uren (${logs.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nog geen uren geboekt.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = log.taskTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "${log.hoursSpent} uur", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F4C81))
                            }
                            Text(text = log.activityDescription, fontSize = 12.sp, color = Color.DarkGray)
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
    var hoursText by remember { mutableStateOf("1.0") }
    var descText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uren Boeken: ${task.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Aantal uren (bijv. 2.0)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Werkbeschrijving") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursText.toDoubleOrNull() ?: 1.0
                    if (descText.isNotBlank()) {
                        onConfirm(h, descText)
                    }
                }
            ) {
                Text("Boeken")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
