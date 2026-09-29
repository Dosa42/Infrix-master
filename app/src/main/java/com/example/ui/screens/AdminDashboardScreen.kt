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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.example.data.model.AuditLogEntity
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WorkLogEntity
import com.example.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    viewModel: DashboardViewModel,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allWorkLogs by viewModel.allWorkLogs.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val securityAlert by viewModel.securityAlert.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Accounts Beheer (${allUsers.size})",
        "Live Monitoring & Sessies",
        "Planning Beheer (${allTasks.size})",
        "Backend Architectuur & Tools",
        "Audit Trail & Beveiliging"
    )

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToEditProfile by remember { mutableStateOf<UserEntity?>(null) }
    var userToEditPermissions by remember { mutableStateOf<UserEntity?>(null) }
    var userToResetPassword by remember { mutableStateOf<UserEntity?>(null) }
    var userToAssignPartner by remember { mutableStateOf<UserEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F4))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Admin Centraal Beheer",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Beheerder: ${currentUser?.username ?: "Infrix-dev"} (Hoogste authority)",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                modifier = Modifier.testTag("admin_logout_button")
            ) {
                Text("Uitloggen", fontSize = 12.sp)
            }
        }

        // Sub Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Volledig Accountbeheer: Profielen, Rollen, Sessies/Lockouts, Rechten en Koppelingen",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }

        // Tabs
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // Content
        Box(modifier = Modifier.weight(1f).padding(12.dp)) {
            when (selectedTab) {
                0 -> AdminUsersComprehensiveTab(
                    users = allUsers,
                    onAddUserClick = { showAddUserDialog = true },
                    onEditProfile = { userToEditProfile = it },
                    onChangeRole = { user, role -> viewModel.changeUserRole(user, role) },
                    onEditPermissions = { userToEditPermissions = it },
                    onAssignPartner = { userToAssignPartner = it },
                    onApprove = { viewModel.approveUser(it) },
                    onToggleActive = { viewModel.toggleUserActive(it) },
                    onUnlock = { viewModel.unlockAccount(it) },
                    onForceLogout = { viewModel.forceRemoteLogout(it) },
                    onResetPassword = { userToResetPassword = it },
                    onDeleteUser = { viewModel.deleteUser(it) }
                )
                1 -> AdminLiveMonitoringTab(
                    users = allUsers,
                    tasks = allTasks,
                    workLogs = allWorkLogs,
                    onForceLogout = { viewModel.forceRemoteLogout(it) },
                    onUnlock = { viewModel.unlockAccount(it) }
                )
                2 -> AdminPlanningTab(
                    tasks = allTasks,
                    onAddTaskClick = { showAddTaskDialog = true },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onStatusChange = { taskId, status -> viewModel.updateTaskStatus(taskId, status) }
                )
                3 -> AdminBackendToolsTab(
                    viewModel = viewModel
                )
                4 -> AdminAuditTab(logs = auditLogs)
            }
        }
    }

    // Modal: 1. Profiel & Gegevens bewerken
    userToEditProfile?.let { user ->
        EditUserProfileDialog(
            user = user,
            onDismiss = { userToEditProfile = null },
            onConfirm = { fullName, email, phone, dept, jobTitle, hourlyRate ->
                viewModel.updateAccountProfile(user, fullName, email, phone, dept, jobTitle, hourlyRate) { ok ->
                    if (ok) userToEditProfile = null
                }
            }
        )
    }

    // Modal: 4. Granulaire Rechten & Toegangsfilters
    userToEditPermissions?.let { user ->
        EditUserPermissionsDialog(
            user = user,
            onDismiss = { userToEditPermissions = null },
            onConfirm = { canComplete, canLogHours, canSubmitRequests ->
                viewModel.updateGranularPermissions(user, canComplete, canLogHours, canSubmitRequests) {
                    userToEditPermissions = null
                }
            }
        )
    }

    // Modal: 5. Vaste Koppeling / Partner toewijzen
    userToAssignPartner?.let { user ->
        AssignPartnerDialog(
            user = user,
            allUsers = allUsers,
            onDismiss = { userToAssignPartner = null },
            onConfirm = { partner ->
                viewModel.assignClientOrPartner(user, partner)
                userToAssignPartner = null
            }
        )
    }

    // Modal: Wachtwoord resetten
    userToResetPassword?.let { user ->
        ResetPasswordDialog(
            targetUser = user,
            onDismiss = { userToResetPassword = null },
            onConfirm = { newPass ->
                viewModel.resetUserPassword(user, newPass) { ok ->
                    if (ok) userToResetPassword = null
                }
            }
        )
    }

    // Modal: Nieuw account aanmaken
    if (showAddUserDialog) {
        CreateFullAccountDialog(
            allUsers = allUsers,
            onDismiss = { showAddUserDialog = false },
            onConfirm = { username, password, role, fullName, email, phone, dept, jobTitle, hourlyRate, partner, autoApprove ->
                viewModel.createWorkerOrClientAccount(
                    username, password, role, fullName, email, phone, dept, jobTitle, hourlyRate, partner, autoApprove
                ) { error ->
                    if (error == null) showAddUserDialog = false
                }
            }
        )
    }

    // Modal: Taak toevoegen
    if (showAddTaskDialog) {
        CreateTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, workerUser, workerName, clientUser, clientName, priority, date, hours, loc ->
                viewModel.createPlanningTask(
                    title, desc, workerUser, workerName, clientUser, clientName, priority, date, hours, loc
                ) { success ->
                    if (success) showAddTaskDialog = false
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
private fun AdminUsersComprehensiveTab(
    users: List<UserEntity>,
    onAddUserClick: () -> Unit,
    onEditProfile: (UserEntity) -> Unit,
    onChangeRole: (UserEntity, UserRole) -> Unit,
    onEditPermissions: (UserEntity) -> Unit,
    onAssignPartner: (UserEntity) -> Unit,
    onApprove: (UserEntity) -> Unit,
    onToggleActive: (UserEntity) -> Unit,
    onUnlock: (UserEntity) -> Unit,
    onForceLogout: (UserEntity) -> Unit,
    onResetPassword: (UserEntity) -> Unit,
    onDeleteUser: (UserEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Geregistreerde Accounts (${users.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Button(
                onClick = onAddUserClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B5797)),
                modifier = Modifier.testTag("admin_add_user_button")
            ) {
                Text("+ Nieuw Account", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(users, key = { it.id }) { user ->
                val isMainAdmin = user.username.equals("Infrix-dev", ignoreCase = true)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header Rij
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[${user.role.displayName}]",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (user.role) {
                                            UserRole.ADMIN -> Color(0xFF7C3AED)
                                            UserRole.WERKER -> Color(0xFF0284C7)
                                            UserRole.KLANT -> Color(0xFF0D9488)
                                        }
                                    )
                                }
                                if (user.jobTitle.isNotBlank()) {
                                    Text(text = user.jobTitle, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            }

                            // Status Badges
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (user.isLocked) {
                                    Text(
                                        text = "Vergrendeld",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                } else if (!user.isApproved) {
                                    Text(
                                        text = "Wacht Goedkeuring",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE67E22)
                                    )
                                } else {
                                    Text(
                                        text = if (user.isActive) "Actief" else "Gedeactiveerd",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.isActive) Color(0xFF27AE60) else Color.Red
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Details Grid
                        Text(
                            text = "Gebruikersnaam: ${user.username} | E-mail: ${user.email} | Tel: ${user.phone.ifBlank { "-" }}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Afdeling/Bedrijf: ${user.departmentOrCompany.ifBlank { "-" }} | Tarief: €${user.hourlyRate}/u",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        if (user.assignedClientOrPartner.isNotBlank()) {
                            Text(
                                text = "Vaste koppeling met: ${user.assignedClientOrPartner}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F4C81)
                            )
                        }

                        // Rechten overzicht
                        Text(
                            text = "Rechten: Taken afronden [${if (user.canCompleteTasks) "JA" else "NEE"}] | Uren boeken [${if (user.canLogHours) "JA" else "NEE"}] | Aanvragen [${if (user.canSubmitRequests) "JA" else "NEE"}]",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Actieknoppen Rij 1: Primaire mutaties
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onEditProfile(user) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("1. Bewerk Profiel", fontSize = 11.sp) }

                            if (!isMainAdmin) {
                                OutlinedButton(
                                    onClick = {
                                        val targetRole = if (user.role == UserRole.WERKER) UserRole.KLANT else UserRole.WERKER
                                        onChangeRole(user, targetRole)
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (user.role == UserRole.WERKER) "2. Rol -> Klant" else "2. Rol -> Werker",
                                        fontSize = 11.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onEditPermissions(user) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("4. Rechten", fontSize = 11.sp) }

                                OutlinedButton(
                                    onClick = { onAssignPartner(user) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("5. Koppel", fontSize = 11.sp) }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Actieknoppen Rij 2: Beveiliging, Goedkeuren & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!user.isApproved) {
                                    Button(
                                        onClick = { onApprove(user) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) { Text("Goedkeuren", fontSize = 11.sp) }
                                }

                                if (user.isLocked) {
                                    Button(
                                        onClick = { onUnlock(user) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) { Text("3. Ontgrendel", fontSize = 11.sp) }
                                }

                                if (!isMainAdmin) {
                                    OutlinedButton(
                                        onClick = { onToggleActive(user) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) { Text(if (user.isActive) "Deactiveer" else "Activeer", fontSize = 11.sp) }

                                    OutlinedButton(
                                        onClick = { onForceLogout(user) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) { Text("3. Force Logout", fontSize = 11.sp) }

                                    OutlinedButton(
                                        onClick = { onResetPassword(user) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) { Text("Reset WW", fontSize = 11.sp) }
                                }
                            }

                            if (!isMainAdmin) {
                                Button(
                                    onClick = { onDeleteUser(user) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) { Text("Wis", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLiveMonitoringTab(
    users: List<UserEntity>,
    tasks: List<PlanningTaskEntity>,
    workLogs: List<WorkLogEntity>,
    onForceLogout: (UserEntity) -> Unit,
    onUnlock: (UserEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Live Account & Sessie Monitoring",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Toezicht op actieve loginsessies, lockouts, en activiteitsbelasting",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Samenvatting Kaarten
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Actieve Sessies", fontSize = 12.sp, color = Color.Gray)
                    val activeSessions = users.count { it.sessionToken.isNotBlank() }
                    Text("$activeSessions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF27AE60))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Vergrendeld", fontSize = 12.sp, color = Color.Gray)
                    val locked = users.count { it.isLocked }
                    Text("$locked", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (locked > 0) Color.Red else Color.DarkGray)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Geboekt Totaal", fontSize = 12.sp, color = Color.Gray)
                    val hours = workLogs.sumOf { it.hoursSpent }
                    Text("${hours}u", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Overzicht per Account (Laatste inlog & Sessies):",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        users.forEach { u ->
            val hasActiveSession = u.sessionToken.isNotBlank()
            val lastLoginStr = u.lastLoginAt?.let { dateFormat.format(Date(it)) } ?: "Nooit"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${u.fullName} (@${u.username})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hasActiveSession) "Sessie Actief" else "Geen actieve sessie",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasActiveSession) Color(0xFF27AE60) else Color.Gray
                            )
                            if (u.isLocked) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "[LOCKOUT]", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Laatste inlog: $lastLoginStr | Mislukte inlogpogingen: ${u.failedAttempts} | Rol: ${u.role.displayName}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )

                    if (hasActiveSession && !u.username.equals("Infrix-dev", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onForceLogout(u) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("Force Logout Sessie", fontSize = 10.sp) }
                        }
                    }
                    if (u.isLocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onUnlock(u) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("Ontgrendel Account", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODALS VOOR DE 5 PIJLERS VAN COMPLEET ACCOUNTBEHEER
// -------------------------------------------------------------

// 1. Profiel & Gegevensbewerking Dialoog
@Composable
private fun EditUserProfileDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Double) -> Unit
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var dept by remember { mutableStateOf(user.departmentOrCompany) }
    var jobTitle by remember { mutableStateOf(user.jobTitle) }
    var hourlyRateStr by remember { mutableStateOf(user.hourlyRate.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profiel Bewerken: @${user.username}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Volledige Naam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mailadres") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefoonnummer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = dept,
                    onValueChange = { dept = it },
                    label = { Text("Afdeling of Bedrijfsnaam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Functietitel (bijv. Senior Monteur)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = hourlyRateStr,
                    onValueChange = { hourlyRateStr = it },
                    label = { Text("Uurtarief (€)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = hourlyRateStr.toDoubleOrNull() ?: user.hourlyRate
                    onConfirm(fullName, email, phone, dept, jobTitle, rate)
                }
            ) { Text("Opslaan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

// 4. Granulaire Rechten Dialoog
@Composable
private fun EditUserPermissionsDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean, Boolean) -> Unit
) {
    var canComplete by remember { mutableStateOf(user.canCompleteTasks) }
    var canLogHours by remember { mutableStateOf(user.canLogHours) }
    var canSubmitRequests by remember { mutableStateOf(user.canSubmitRequests) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rechten Configureren: @${user.username}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Stel specifieke bevoegdheden in voor dit account:", fontSize = 13.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = canComplete, onCheckedChange = { canComplete = it })
                    Text("Mag taken definitief afronden", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = canLogHours, onCheckedChange = { canLogHours = it })
                    Text("Mag werkuren boeken en registreren", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = canSubmitRequests, onCheckedChange = { canSubmitRequests = it })
                    Text("Mag service-aanvragen indienen", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(canComplete, canLogHours, canSubmitRequests) }) { Text("Toepassen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

// 5. Koppeling Dialoog
@Composable
private fun AssignPartnerDialog(
    user: UserEntity,
    allUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val candidates = allUsers.filter { it.username != user.username }
    var selectedPartner by remember { mutableStateOf(user.assignedClientOrPartner) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vaste Koppeling voor @${user.username}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Koppel deze ${user.role.displayName} aan een vaste klant of contactpersoon:", fontSize = 13.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))

                candidates.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPartner == c.username,
                            onClick = { selectedPartner = c.username }
                        )
                        Text("${c.fullName} (@${c.username} - ${c.role.displayName})", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedPartner) }) { Text("Koppeling Opslaan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

// Volledig Account Aanmaken Dialoog
@Composable
private fun CreateFullAccountDialog(
    allUsers: List<UserEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, UserRole, String, String, String, String, String, Double, String, Boolean) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var hourlyRateStr by remember { mutableStateOf("0.0") }
    var selectedRole by remember { mutableStateOf(UserRole.WERKER) }
    var partner by remember { mutableStateOf("") }
    var autoApprove by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nieuw Account Registreren", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Kies Rol:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row {
                    listOf(UserRole.WERKER, UserRole.KLANT).forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r }
                            )
                            Text(r.displayName, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Gebruikersnaam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Passwoord") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Volledige Naam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mailadres") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefoonnummer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = dept,
                    onValueChange = { dept = it },
                    label = { Text(if (selectedRole == UserRole.WERKER) "Afdeling" else "Bedrijfsnaam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Functietitel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = hourlyRateStr,
                    onValueChange = { hourlyRateStr = it },
                    label = { Text("Uurtarief (€)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = autoApprove, onClick = { autoApprove = !autoApprove })
                    Text("Direct goedkeuren voor inloggen", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        val rate = hourlyRateStr.toDoubleOrNull() ?: 0.0
                        onConfirm(
                            username.trim(),
                            password.trim(),
                            selectedRole,
                            fullName.trim(),
                            email.trim(),
                            phone.trim(),
                            dept.trim(),
                            jobTitle.trim(),
                            rate,
                            partner.trim(),
                            autoApprove
                        )
                    }
                }
            ) { Text("Creëren") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

@Composable
private fun ResetPasswordDialog(
    targetUser: UserEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wachtwoord Resetten voor ${targetUser.username}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Voer het nieuwe wachtwoord in voor dit account:", fontSize = 13.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nieuw Wachtwoord") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.isNotBlank()) {
                        onConfirm(newPassword.trim())
                    }
                }
            ) { Text("Opslaan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

@Composable
private fun AdminPlanningTab(
    tasks: List<PlanningTaskEntity>,
    onAddTaskClick: () -> Unit,
    onDeleteTask: (PlanningTaskEntity) -> Unit,
    onStatusChange: (Long, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alle Planningstaken (${tasks.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Button(
                onClick = onAddTaskClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B5797)),
                modifier = Modifier.testTag("admin_add_task_button")
            ) {
                Text("+ Nieuwe Taak Inplannen", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Geen taken gevonden in de planning.", color = Color.Gray)
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
                                text = "Werker: ${task.assignedWorkerName} | Klant: ${task.clientName}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Datum: ${task.scheduledDate} | Geschat: ${task.estimatedHours}u | Geregistreerd: ${task.actualHours}u",
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
                                        onClick = { onStatusChange(task.id, "Gepland") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("Gepland", fontSize = 11.sp) }
                                    OutlinedButton(
                                        onClick = { onStatusChange(task.id, "In uitvoering") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("In uitvoering", fontSize = 11.sp) }
                                    OutlinedButton(
                                        onClick = { onStatusChange(task.id, "Afgerond") },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("Afgerond", fontSize = 11.sp) }
                                }

                                Button(
                                    onClick = { onDeleteTask(task) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) { Text("Verwijderen", fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAuditTab(
    logs: List<AuditLogEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Centraal Beveiligings- & Auditlogboek (${logs.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = "Overzicht van acties, inlogpogingen, rolwisselingen en accountmutaties",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Geen logs aanwezig.", color = Color.Gray)
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
                                Text(
                                    text = log.actionType,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (log.severity == "SECURITY_ALERT" || log.severity == "WARNING") Color.Red else Color.Black
                                )
                                Text(
                                    text = "${log.actorUsername} (${log.actorRole})",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = log.details,
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var workerUser by remember { mutableStateOf("") }
    var workerName by remember { mutableStateOf("") }
    var clientUser by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normaal") }
    var date by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("1.0") }
    var loc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nieuwe Taak Inplannen", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Taaktitel") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Omschrijving") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = workerUser,
                    onValueChange = { workerUser = it },
                    label = { Text("Toegewezen Werker gebruikersnaam") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = clientUser,
                    onValueChange = { clientUser = it },
                    label = { Text("Klant gebruikersnaam") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Geplande datum/tijd") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Geschatte uren") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedHours = hours.toDoubleOrNull() ?: 1.0
                        onConfirm(title, desc, workerUser, workerName, clientUser, clientName, priority, date, parsedHours, loc)
                    }
                }
            ) { Text("Opslaan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
