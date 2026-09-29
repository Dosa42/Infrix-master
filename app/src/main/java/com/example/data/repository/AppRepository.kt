package com.example.data.repository

import android.content.Context
import com.example.data.ai.AIHarnessCategory
import com.example.data.ai.AIHarnessEngine
import com.example.data.ai.HarnessPromptConfiguration
import com.example.data.auth.ChatGPTAuthManager
import com.example.data.auth.ChatGPTModelInfo
import com.example.data.auth.ChatGPTSession
import com.example.data.auth.ChatMessage
import com.example.data.backend.CalendarBackendService
import com.example.data.backend.CloudSyncStatus
import com.example.data.backend.DeviceCapabilitiesManager
import com.example.data.backend.DevicePermissionStatus
import com.example.data.backend.EmailBackendService
import com.example.data.backend.EmailDispatchResult
import com.example.data.backend.GeoLocationData
import com.example.data.backend.GoogleMapsIntegrationService
import com.example.data.backend.RouteCalculationResult
import com.example.data.backend.SearchIntegrationService
import com.example.data.backend.SearchResultItem
import com.example.data.backend.ServerStorageManager
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.CalendarEventEntity
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WorkLogEntity
import com.example.data.security.SecurityManager
import com.example.data.security.SecurityResult
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    private val userDao = database.userDao()
    private val planningDao = database.planningDao()
    private val workLogDao = database.workLogDao()
    private val serviceRequestDao = database.serviceRequestDao()
    private val auditLogDao = database.auditLogDao()
    private val calendarDao = database.calendarDao()

    // Backend Services
    val deviceCapabilitiesManager = DeviceCapabilitiesManager(context)
    val emailBackendService = EmailBackendService(context)
    val googleMapsIntegrationService = GoogleMapsIntegrationService(context)
    val searchIntegrationService = SearchIntegrationService(context)
    val serverStorageManager = ServerStorageManager(database)
    val calendarBackendService = CalendarBackendService(context)
    val chatGPTAuthManager = ChatGPTAuthManager(context, CoroutineScope(Dispatchers.IO))
    val aiHarnessEngine = AIHarnessEngine()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Flows
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allTasks: Flow<List<PlanningTaskEntity>> = planningDao.getAllTasks()
    val allWorkLogs: Flow<List<WorkLogEntity>> = workLogDao.getAllWorkLogs()
    val allServiceRequests: Flow<List<ServiceRequestEntity>> = serviceRequestDao.getAllRequests()
    val recentAuditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getRecentLogs(200)
    val allCalendarEvents: Flow<List<CalendarEventEntity>> = calendarDao.getAllEvents()
    val chatGPTSession: StateFlow<ChatGPTSession?> = chatGPTAuthManager.sessionState
    val chatGPTModels: StateFlow<List<ChatGPTModelInfo>> = chatGPTAuthManager.models

    fun getCalendarEventsForDate(date: String): Flow<List<CalendarEventEntity>> {
        return calendarDao.getEventsForDate(date)
    }

    fun getCalendarEventsForWorker(workerUsername: String): Flow<List<CalendarEventEntity>> {
        return calendarDao.getEventsForWorker(workerUsername)
    }

    fun getCalendarEventsForClient(clientUsername: String): Flow<List<CalendarEventEntity>> {
        return calendarDao.getEventsForClient(clientUsername)
    }

    fun getTasksForWorker(workerUsername: String): Flow<List<PlanningTaskEntity>> {
        return planningDao.getTasksForWorker(workerUsername)
    }

    fun getTasksForClient(clientUsername: String): Flow<List<PlanningTaskEntity>> {
        return planningDao.getTasksForClient(clientUsername)
    }

    fun getWorkLogsForWorker(workerUsername: String): Flow<List<WorkLogEntity>> {
        return workLogDao.getWorkLogsForWorker(workerUsername)
    }

    fun getRequestsForClient(clientUsername: String): Flow<List<ServiceRequestEntity>> {
        return serviceRequestDao.getRequestsForClient(clientUsername)
    }

    suspend fun login(
        usernameInput: String,
        passwordInput: String,
        selectedRole: UserRole
    ): SecurityResult<UserEntity> = withContext(Dispatchers.IO) {
        AppDatabase.ensureAdminAccount(database)

        val cleanUsername = usernameInput.trim()
        val key = cleanUsername.ifBlank { "anonymous" }

        val user = userDao.getUserByUsername(cleanUsername)
        if (user == null) {
            val log = SecurityManager.createAuditLog(
                actor = null,
                actionType = "LOGIN_FAILED",
                details = "Inloggen mislukt: Gebruikersnaam '$cleanUsername' bestaat niet in het systeem.",
                severity = "WARNING"
            )
            auditLogDao.insertLog(log)
            return@withContext SecurityResult.Denied(
                reason = "Ongeldige gebruikersnaam of wachtwoord.",
                requiredLevel = 1,
                actualLevel = 0,
                violationCode = "USER_NOT_FOUND"
            )
        }

        // Lockout Check
        if (user.isLocked) {
            val alert = SecurityManager.createAuditLog(
                actor = user,
                actionType = "LOGIN_BLOCKED_LOCKED",
                details = "Inlogpoging op vergrendeld account '$cleanUsername' geblokkeerd. Lockout vereist Admin ontgrendeling.",
                severity = "SECURITY_ALERT"
            )
            auditLogDao.insertLog(alert)
            return@withContext SecurityResult.Denied(
                reason = "Dit account is vergrendeld wegens beveiligingsredenen. Neem contact op met beheerder Infrix-dev.",
                requiredLevel = 1,
                actualLevel = 0,
                violationCode = "ACCOUNT_LOCKED"
            )
        }

        // Verify role selection matches account role
        if (user.role != selectedRole) {
            val updatedUser = user.copy(failedAttempts = user.failedAttempts + 1)
            userDao.updateUser(updatedUser)
            val log = SecurityManager.createAuditLog(
                actor = user,
                actionType = "ROLE_MISMATCH",
                details = "Gekozen type '${selectedRole.displayName}' komt niet overeen met geregistreerde rol '${user.role.displayName}'.",
                severity = "WARNING"
            )
            auditLogDao.insertLog(log)
            return@withContext SecurityResult.Denied(
                reason = "Gekozen type '${selectedRole.displayName}' komt niet overeen met de rol van dit account (${user.role.displayName}).",
                requiredLevel = selectedRole.authorityLevel,
                actualLevel = user.role.authorityLevel,
                violationCode = "ROLE_MISMATCH"
            )
        }

        // Verify password hash
        val isPasswordCorrect = SecurityManager.verifyPassword(passwordInput, user.salt, user.passwordHash)
        if (!isPasswordCorrect) {
            val newFailed = user.failedAttempts + 1
            val shouldLock = newFailed >= 5 && !user.username.equals("Infrix-dev", ignoreCase = true)
            val updatedUser = user.copy(
                failedAttempts = newFailed,
                isLocked = if (shouldLock) true else user.isLocked
            )
            userDao.updateUser(updatedUser)

            val log = SecurityManager.createAuditLog(
                actor = user,
                actionType = if (shouldLock) "ACCOUNT_AUTO_LOCKED" else "LOGIN_FAILED",
                details = if (shouldLock) "Account '${user.username}' automatisch vergrendeld na $newFailed foute inlogpogingen." else "Ongeldig wachtwoord voor '${user.username}' (Fout #$newFailed).",
                severity = if (shouldLock) "SECURITY_ALERT" else "WARNING"
            )
            auditLogDao.insertLog(log)

            val msg = if (shouldLock) "Account is nu vergrendeld na 5 foutieve pogingen. Alleen Admin kan ontgrendelen." else "Ongeldige gebruikersnaam of wachtwoord."
            return@withContext SecurityResult.Denied(
                reason = msg,
                requiredLevel = 1,
                actualLevel = 0,
                violationCode = "INVALID_CREDENTIALS"
            )
        }

        // Check Goedkeuring
        if (!user.isApproved) {
            val log = SecurityManager.createAuditLog(
                actor = user,
                actionType = "LOGIN_PENDING_APPROVAL",
                details = "Inlogpoging door '${user.username}' geweigerd: Wacht op goedkeuring van Admin Infrix-dev.",
                severity = "WARNING"
            )
            auditLogDao.insertLog(log)
            return@withContext SecurityResult.Denied(
                reason = "Dit account is nog in afwachting van goedkeuring door de beheerder (Infrix-dev).",
                requiredLevel = 1,
                actualLevel = 0,
                violationCode = "NOT_APPROVED"
            )
        }

        // Check Actief
        if (!user.isActive) {
            val log = SecurityManager.createAuditLog(
                actor = user,
                actionType = "LOGIN_BLOCKED",
                details = "Inlogpoging op inactief account '$cleanUsername' geweigerd.",
                severity = "WARNING"
            )
            auditLogDao.insertLog(log)
            return@withContext SecurityResult.Denied(
                reason = "Dit account is gedeactiveerd door beheerder Infrix-dev.",
                requiredLevel = 1,
                actualLevel = 0,
                violationCode = "ACCOUNT_INACTIVE"
            )
        }

        // Login success: Update session token, reset failed attempts & set lastLoginAt
        val newSessionToken = UUID.randomUUID().toString()
        val loggedInUser = user.copy(
            failedAttempts = 0,
            lastLoginAt = System.currentTimeMillis(),
            sessionToken = newSessionToken
        )
        userDao.updateUser(loggedInUser)
        _currentUser.value = loggedInUser

        val successLog = SecurityManager.createAuditLog(
            actor = loggedInUser,
            actionType = "LOGIN_SUCCESS",
            details = "Succesvol ingelogd als '${loggedInUser.username}' (${loggedInUser.role.displayName}). Sessie ID: ${newSessionToken.take(8)}...",
            severity = "INFO"
        )
        auditLogDao.insertLog(successLog)

        SecurityResult.Success(loggedInUser)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user != null) {
            userDao.updateUser(user.copy(sessionToken = ""))
            auditLogDao.insertLog(
                SecurityManager.createAuditLog(
                    actor = user,
                    actionType = "LOGOUT",
                    details = "Sessie beëindigd door '${user.username}'.",
                    severity = "INFO"
                )
            )
        }
        _currentUser.value = null
    }

    // ==========================================
    // DE 5 PIJLERS VAN VOLLEDIG ACCOUNTBEHEER
    // ==========================================

    // 1. Profiel- & Gegevensbewerking
    suspend fun updateAccountProfile(
        targetUser: UserEntity,
        fullName: String,
        email: String,
        phone: String,
        departmentOrCompany: String,
        jobTitle: String,
        hourlyRate: Double
    ): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Profielgegevens bewerken")
        if (guard is SecurityResult.Denied) return@withContext guard

        val updated = targetUser.copy(
            fullName = fullName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            departmentOrCompany = departmentOrCompany.trim(),
            jobTitle = jobTitle.trim(),
            hourlyRate = hourlyRate
        )
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "PROFILE_UPDATED",
                "Profiel van '${targetUser.username}' bijgewerkt door beheerder (Functie: $jobTitle, Tarief: €$hourlyRate/u).",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    // 2. Rolwijziging / Promotie
    suspend fun changeUserRole(targetUser: UserEntity, newRole: UserRole): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Gebruikersrol wijzigen")
        if (guard is SecurityResult.Denied) return@withContext guard

        if (targetUser.username.equals("Infrix-dev", ignoreCase = true) && newRole != UserRole.ADMIN) {
            return@withContext SecurityResult.Denied(
                reason = "Het beheerderaccount Infrix-dev kan niet worden gedegradeerd.",
                requiredLevel = UserRole.ADMIN.authorityLevel,
                actualLevel = actor?.role?.authorityLevel ?: 0,
                violationCode = "CANNOT_DEMOTE_ADMIN"
            )
        }

        val updated = targetUser.copy(role = newRole)
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "ROLE_CHANGED",
                "Rol van '${targetUser.username}' gewijzigd van '${targetUser.role.displayName}' naar '${newRole.displayName}' door beheerder.",
                "WARNING"
            )
        )
        SecurityResult.Success(Unit)
    }

    // 3. Beveiligings- & Sessiecontrole (Force Logout & Ontgrendelen)
    suspend fun unlockAccount(targetUser: UserEntity): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Account ontgrendelen")
        if (guard is SecurityResult.Denied) return@withContext guard

        val updated = targetUser.copy(isLocked = false, failedAttempts = 0)
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "ACCOUNT_UNLOCKED",
                "Account '${targetUser.username}' handmatig ontgrendeld en foutieve teller gereset door beheerder.",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    suspend fun forceRemoteLogout(targetUser: UserEntity): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Sessie beëindigen op afstand")
        if (guard is SecurityResult.Denied) return@withContext guard

        val updated = targetUser.copy(sessionToken = "")
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "REMOTE_LOGOUT_FORCED",
                "Actieve sessie van '${targetUser.username}' op afstand beëindigd door beheerder Infrix-dev.",
                "WARNING"
            )
        )
        SecurityResult.Success(Unit)
    }

    // 4. Granulaire Rechten & Toegangsfilters
    suspend fun updateGranularPermissions(
        targetUser: UserEntity,
        canCompleteTasks: Boolean,
        canLogHours: Boolean,
        canSubmitRequests: Boolean
    ): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Rechten configureren")
        if (guard is SecurityResult.Denied) return@withContext guard

        val updated = targetUser.copy(
            canCompleteTasks = canCompleteTasks,
            canLogHours = canLogHours,
            canSubmitRequests = canSubmitRequests
        )
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "PERMISSIONS_UPDATED",
                "Rechten van '${targetUser.username}' gewijzigd (Uren boeken: $canLogHours, Afronden: $canCompleteTasks, Aanvragen: $canSubmitRequests).",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    // 5. Koppeling & Vaste Toewijzing
    suspend fun assignClientOrPartner(targetUser: UserEntity, assignedEntity: String): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Vaste koppeling toewijzen")
        if (guard is SecurityResult.Denied) return@withContext guard

        val updated = targetUser.copy(assignedClientOrPartner = assignedEntity.trim())
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "PARTNER_ASSIGNED",
                "Vaste toewijzing/koppeling van '${targetUser.username}' gezet op '$assignedEntity'.",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    // Standaard beheer acties
    suspend fun createUserByAdmin(
        username: String,
        passwordPlain: String,
        role: UserRole,
        fullName: String,
        email: String,
        phone: String = "",
        departmentOrCompany: String = "",
        jobTitle: String = "",
        hourlyRate: Double = 0.0,
        assignedPartner: String = "",
        autoApprove: Boolean = true
    ): SecurityResult<Long> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Nieuw account aanmaken")
        if (guard is SecurityResult.Denied) return@withContext guard

        val cleanUsername = username.trim()
        val existing = userDao.getUserByUsername(cleanUsername)
        if (existing != null) {
            return@withContext SecurityResult.Denied(
                reason = "Gebruikersnaam '$cleanUsername' is al in gebruik.",
                requiredLevel = UserRole.ADMIN.authorityLevel,
                actualLevel = actor?.role?.authorityLevel ?: 0,
                violationCode = "USERNAME_EXISTS"
            )
        }

        val salt = SecurityManager.generateSalt()
        val newUser = UserEntity(
            username = cleanUsername,
            passwordHash = SecurityManager.hashPassword(passwordPlain, salt),
            salt = salt,
            role = role,
            fullName = fullName.trim().ifBlank { cleanUsername },
            email = email.trim().ifBlank { "$cleanUsername@bedrijf.local" },
            phone = phone.trim(),
            departmentOrCompany = departmentOrCompany.trim(),
            jobTitle = jobTitle.trim(),
            hourlyRate = hourlyRate,
            assignedClientOrPartner = assignedPartner.trim(),
            isApproved = autoApprove,
            isActive = true,
            isLocked = false,
            approvedBy = actor?.username ?: "Infrix-dev",
            approvedAt = System.currentTimeMillis()
        )
        val id = userDao.insertUser(newUser)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "USER_CREATED_BY_ADMIN",
                "Account '${newUser.username}' aangemaakt als '${role.displayName}'.",
                "INFO"
            )
        )
        SecurityResult.Success(id)
    }

    suspend fun approveUser(user: UserEntity): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Account goedkeuren")
        if (guard is SecurityResult.Denied) return@withContext guard

        val updated = user.copy(
            isApproved = true,
            approvedBy = actor?.username ?: "Infrix-dev",
            approvedAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "USER_APPROVED",
                "Account '${user.username}' goedgekeurd door beheerder.",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    suspend fun toggleUserActive(user: UserEntity): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Gebruiker status wijzigen")
        if (guard is SecurityResult.Denied) return@withContext guard

        if (actor?.id == user.id || user.username.equals("Infrix-dev", ignoreCase = true)) {
            return@withContext SecurityResult.Denied(
                reason = "U kunt uw eigen beheerderaccount (Infrix-dev) niet deactiveren.",
                requiredLevel = UserRole.ADMIN.authorityLevel,
                actualLevel = actor?.role?.authorityLevel ?: 0,
                violationCode = "CANNOT_DEACTIVATE_SELF"
            )
        }

        val updated = user.copy(isActive = !user.isActive)
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "USER_STATUS_CHANGED",
                "Status van '${user.username}' gewijzigd naar: ${if (updated.isActive) "Actief" else "Gedeactiveerd"}.",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    suspend fun resetUserPassword(user: UserEntity, newPasswordPlain: String): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Wachtwoord resetten")
        if (guard is SecurityResult.Denied) return@withContext guard

        val newSalt = SecurityManager.generateSalt()
        val updated = user.copy(
            passwordHash = SecurityManager.hashPassword(newPasswordPlain, newSalt),
            salt = newSalt,
            isLocked = false,
            failedAttempts = 0
        )
        userDao.updateUser(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "PASSWORD_RESET_BY_ADMIN",
                "Wachtwoord van '${user.username}' gereset door beheerder.",
                "WARNING"
            )
        )
        SecurityResult.Success(Unit)
    }

    suspend fun deleteUser(user: UserEntity): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Gebruiker verwijderen")
        if (guard is SecurityResult.Denied) return@withContext guard

        if (actor?.id == user.id || user.username.equals("Infrix-dev", ignoreCase = true)) {
            return@withContext SecurityResult.Denied(
                reason = "Het hoofdbeheerderaccount (Infrix-dev) kan niet worden verwijderd.",
                requiredLevel = UserRole.ADMIN.authorityLevel,
                actualLevel = actor?.role?.authorityLevel ?: 0,
                violationCode = "CANNOT_DELETE_ADMIN"
            )
        }

        userDao.deleteUser(user)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "USER_DELETED",
                "Account '${user.username}' permanent verwijderd door beheerder.",
                "WARNING"
            )
        )
        SecurityResult.Success(Unit)
    }

    suspend fun deleteTask(task: PlanningTaskEntity): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Taak permanent verwijderen")
        if (guard is SecurityResult.Denied) return@withContext guard

        planningDao.deleteTask(task)
        calendarDao.deleteEventByTaskId(task.id)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "TASK_DELETED",
                "Planningstaak '${task.title}' verwijderd (inclusief gekoppelde kalender-afspraak).",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    // Planning & Taken met Default Kalender Synchronisatie
    suspend fun createPlanningTask(task: PlanningTaskEntity): SecurityResult<Long> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.WERKER, "Planningstaak aanmaken")
        if (guard is SecurityResult.Denied) return@withContext guard

        val id = planningDao.insertTask(task)
        val createdTask = task.copy(id = id)

        // Automatische Default Synchronisatie met Backend Kalender
        val calendarEvent = calendarBackendService.syncPlanningTaskToCalendar(createdTask)
        calendarDao.insertEvent(calendarEvent)

        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "TASK_CREATED",
                "Planningstaak '${task.title}' aangemaakt voor werker '${task.assignedWorkerUsername}' en automatisch synchroon in de kalender geplaatst.",
                "INFO"
            )
        )
        SecurityResult.Success(id)
    }

    suspend fun updateTaskStatus(
        taskId: Long,
        newStatus: String,
        actualHours: Double? = null,
        notes: String? = null
    ): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.WERKER, "Taakstatus wijzigen")
        if (guard is SecurityResult.Denied) return@withContext guard

        // Check individuele permissie
        if (actor?.role == UserRole.WERKER && newStatus == "Afgerond" && !actor.canCompleteTasks) {
            return@withContext SecurityResult.Denied(
                reason = "Uw account heeft geen bevoegdheid om taken definitief af te ronden.",
                requiredLevel = 2,
                actualLevel = 2,
                violationCode = "PERMISSION_DENIED_COMPLETE_TASK"
            )
        }

        val existing = planningDao.getTaskById(taskId)
            ?: return@withContext SecurityResult.Denied("Taak niet gevonden", 2, actor?.role?.authorityLevel ?: 0, "NOT_FOUND")

        val clientFriendly = when (newStatus) {
            "In uitvoering" -> "In uitvoering"
            "Gepauzeerd" -> "Tijdelijk gepauzeerd"
            "Afgerond" -> "Succesvol afgerond"
            else -> "In planning"
        }

        val updated = existing.copy(
            status = newStatus,
            clientVisibleStatus = clientFriendly,
            actualHours = actualHours ?: existing.actualHours,
            internalNotes = notes ?: existing.internalNotes,
            updatedAt = System.currentTimeMillis()
        )
        planningDao.updateTask(updated)

        // Kalender event synchroniseren
        val existingEvent = calendarDao.getEventByTaskId(taskId)
        if (existingEvent != null) {
            val updatedEvent = existingEvent.copy(
                description = "${updated.description}\n\nStatus: ${updated.status}\nGeschatte uren: ${updated.estimatedHours}u",
                priority = updated.priority,
                updatedAt = System.currentTimeMillis()
            )
            calendarDao.updateEvent(updatedEvent)
        }

        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "TASK_STATUS_UPDATED",
                "Taak '${updated.title}' gewijzigd naar '$newStatus' en kalender bijgewerkt.",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    suspend fun logWorkHours(
        taskId: Long,
        taskTitle: String,
        hours: Double,
        activity: String
    ): SecurityResult<Long> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.WERKER, "Uren registreren")
        if (guard is SecurityResult.Denied) return@withContext guard

        if (actor?.role == UserRole.WERKER && !actor.canLogHours) {
            return@withContext SecurityResult.Denied(
                reason = "Urenregistratie is uitgeschakeld voor uw account door de beheerder.",
                requiredLevel = 2,
                actualLevel = 2,
                violationCode = "PERMISSION_DENIED_LOG_HOURS"
            )
        }

        val log = WorkLogEntity(
            taskId = taskId,
            taskTitle = taskTitle,
            workerUsername = actor?.username ?: "onbekend",
            workerName = actor?.fullName ?: "Onbekende werker",
            hoursSpent = hours,
            activityDescription = activity
        )
        val id = workLogDao.insertWorkLog(log)

        val task = planningDao.getTaskById(taskId)
        if (task != null) {
            planningDao.updateTask(task.copy(actualHours = task.actualHours + hours, updatedAt = System.currentTimeMillis()))
        }

        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "HOURS_LOGGED",
                "$hours uur geregistreerd voor taak '$taskTitle'.",
                "INFO"
            )
        )
        SecurityResult.Success(id)
    }

    // Client aanvragen met Default Kalender Synchronisatie
    suspend fun submitServiceRequest(
        title: String,
        description: String,
        preferredDate: String,
        urgency: String
    ): SecurityResult<Long> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        if (actor == null) return@withContext SecurityResult.Denied("Geen sessie", 1, 0, "UNAUTHENTICATED")

        if (actor.role == UserRole.KLANT && !actor.canSubmitRequests) {
            return@withContext SecurityResult.Denied(
                reason = "Het indienen van aanvragen is voor uw account geblokkeerd door de beheerder.",
                requiredLevel = 1,
                actualLevel = 1,
                violationCode = "PERMISSION_DENIED_REQUESTS"
            )
        }

        val request = ServiceRequestEntity(
            clientUsername = actor.username,
            clientName = actor.fullName,
            title = title.trim(),
            description = description.trim(),
            preferredDate = preferredDate.trim(),
            urgency = urgency,
            status = "Nieuw ingediend"
        )
        val id = serviceRequestDao.insertRequest(request)
        val createdRequest = request.copy(id = id)

        // Automatisch als kalender-item synchroon inschieten
        val calendarEvent = calendarBackendService.syncServiceRequestToCalendar(createdRequest)
        calendarDao.insertEvent(calendarEvent)

        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "REQUEST_SUBMITTED",
                "Klant '${actor.username}' heeft serviceaanvraag '$title' ingediend en gesynchroniseerd in de kalender.",
                "INFO"
            )
        )
        SecurityResult.Success(id)
    }

    suspend fun updateRequestStatus(
        requestId: Long,
        newStatus: String
    ): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.WERKER, "Aanvraag status beoordelen")
        if (guard is SecurityResult.Denied) return@withContext guard

        serviceRequestDao.updateRequestStatus(requestId, newStatus)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "REQUEST_STATUS_UPDATED",
                "Serviceaanvraag #$requestId gewijzigd naar '$newStatus'.",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    // ==========================================
    // BACKEND KALENDER & VOLLEDIGE SYNCHRONISATIE
    // ==========================================

    suspend fun createCalendarEvent(
        title: String,
        description: String,
        eventDate: String,
        startTime: String,
        endTime: String,
        location: String,
        workerUsername: String,
        workerName: String,
        clientUsername: String,
        clientName: String,
        priority: String = "Normaal",
        calendarColorHex: String = "#38BDF8"
    ): SecurityResult<Long> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.WERKER, "Kalenderafspraak inplannen")
        if (guard is SecurityResult.Denied) return@withContext guard

        val mapsUrl = calendarBackendService.generateDirectionsUrl(location)
        val searchQuery = calendarBackendService.generateGoogleSearchQuery(title, clientName, location)

        val event = CalendarEventEntity(
            title = title.trim(),
            description = description.trim(),
            eventDate = eventDate.trim(),
            startTime = startTime.trim(),
            endTime = endTime.trim(),
            location = location.trim(),
            workerUsername = workerUsername.trim(),
            workerName = workerName.trim(),
            clientUsername = clientUsername.trim(),
            clientName = clientName.trim(),
            googleSearchQuery = searchQuery,
            googleMapsUrl = mapsUrl,
            syncStatus = "SYNCHRONIZED",
            isSyncedWithGoogleCalendar = true,
            priority = priority,
            calendarColorHex = calendarColorHex
        )

        val id = calendarDao.insertEvent(event)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "CALENDAR_EVENT_CREATED",
                "Nieuwe kalenderafspraak '$title' aangemaakt voor datum $eventDate met Maps & Search koppeling.",
                "INFO"
            )
        )
        SecurityResult.Success(id)
    }

    suspend fun updateCalendarEvent(event: CalendarEventEntity): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.WERKER, "Kalenderafspraak bewerken")
        if (guard is SecurityResult.Denied) return@withContext guard

        val mapsUrl = if (event.location.isNotBlank()) calendarBackendService.generateDirectionsUrl(event.location) else event.googleMapsUrl
        val searchQuery = calendarBackendService.generateGoogleSearchQuery(event.title, event.clientName, event.location)

        val updated = event.copy(
            googleMapsUrl = mapsUrl,
            googleSearchQuery = searchQuery,
            updatedAt = System.currentTimeMillis()
        )
        calendarDao.updateEvent(updated)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "CALENDAR_EVENT_UPDATED",
                "Kalenderafspraak '${event.title}' bijgewerkt.",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    suspend fun deleteCalendarEvent(id: Long): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.WERKER, "Kalenderafspraak verwijderen")
        if (guard is SecurityResult.Denied) return@withContext guard

        calendarDao.deleteEventById(id)
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "CALENDAR_EVENT_DELETED",
                "Kalenderafspraak #$id verwijderd.",
                "INFO"
            )
        )
        SecurityResult.Success(Unit)
    }

    /**
     * Synchroniseert in één klik alle Planning Taken, Service Aanvragen, Werker & Klant allocaties
     * met Google Maps en Google Search in de centrale kalender.
     */
    suspend fun syncAllEntitiesToCalendar(): SecurityResult<Int> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "Centrale kalender auto-synchronisatie")
        if (guard is SecurityResult.Denied) return@withContext guard

        val tasks = planningDao.getAllTasks().first()
        val requests = serviceRequestDao.getAllRequests().first()

        var syncCount = 0

        for (task in tasks) {
            val existing = calendarDao.getEventByTaskId(task.id)
            val generated = calendarBackendService.syncPlanningTaskToCalendar(task)
            if (existing != null) {
                calendarDao.updateEvent(generated.copy(id = existing.id, createdAt = existing.createdAt))
            } else {
                calendarDao.insertEvent(generated)
            }
            syncCount++
        }

        for (req in requests) {
            val existing = calendarDao.getEventByRequestId(req.id)
            val generated = calendarBackendService.syncServiceRequestToCalendar(req)
            if (existing != null) {
                calendarDao.updateEvent(generated.copy(id = existing.id, createdAt = existing.createdAt))
            } else {
                calendarDao.insertEvent(generated)
            }
            syncCount++
        }

        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "CALENDAR_FULL_SYNC",
                "Volledige synchronisatie uitgevoerd: $syncCount items gesynchroniseerd met werkers, klanten, Maps & Search.",
                "INFO"
            )
        )
        SecurityResult.Success(syncCount)
    }

    // ==========================================
    // CHATGPT AUTH TOKEN & 3-TIER AI HARNESS
    // ==========================================

    /**
     * Handmatig injecteren / uploaden van een ChatGPT Bearer Token of Sessie JSON.
     * Geen aanvraag procedure in RoleVault. Indien ongeldig -> Direct fout (Geen fallback).
     */
    suspend fun injectChatGPTToken(rawTokenOrJson: String): SecurityResult<ChatGPTSession> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "ChatGPT Token Configureren")
        if (guard is SecurityResult.Denied) return@withContext guard

        val res = chatGPTAuthManager.injectManualToken(rawTokenOrJson)
        if (res.isSuccess) {
            val session = res.getOrThrow()
            auditLogDao.insertLog(
                SecurityManager.createAuditLog(
                    actor,
                    "CHATGPT_TOKEN_INJECTED",
                    "Nieuwe ChatGPT Bearer Token succesvol geïnjecteerd voor account '${session.email}'.",
                    "INFO"
                )
            )
            SecurityResult.Success(session)
        } else {
            val err = res.exceptionOrNull()?.message ?: "Ongeldige token"
            SecurityResult.Denied(
                reason = "Token Validatie Mislukt: $err (Geen Fallback toegestaan)",
                requiredLevel = 3,
                actualLevel = actor?.role?.authorityLevel ?: 0,
                violationCode = "INVALID_TOKEN_NO_FALLBACK"
            )
        }
    }

    suspend fun clearChatGPTSession(): SecurityResult<Unit> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        val guard = SecurityManager.checkPermission(actor, UserRole.ADMIN, "ChatGPT Sessie Wissen")
        if (guard is SecurityResult.Denied) return@withContext guard

        chatGPTAuthManager.clearSession()
        auditLogDao.insertLog(
            SecurityManager.createAuditLog(
                actor,
                "CHATGPT_SESSION_CLEARED",
                "ChatGPT Auth Sessie en Bearer Token gewist.",
                "WARNING"
            )
        )
        SecurityResult.Success(Unit)
    }

    /**
     * Voert een real-time AI prompt uit via het bijbehorende Harnas (Klant, Werker of Admin).
     * De Admin AI treedt op als de exclusieve brug die de context en parameters filtert.
     */
    suspend fun executeHarnessStream(
        category: AIHarnessCategory,
        messages: List<ChatMessage>,
        userPrompt: String,
        onChunk: (String) -> Unit,
        onStatus: (String) -> Unit
    ): SecurityResult<String> = withContext(Dispatchers.IO) {
        val actor = _currentUser.value
        if (actor == null) return@withContext SecurityResult.Denied("Geen actieve sessie", 1, 0, "UNAUTHENTICATED")

        // Role-based harness guard
        if (actor.role.authorityLevel < category.roleAllowed.authorityLevel) {
            return@withContext SecurityResult.Denied(
                reason = "Onvoldoende rechten voor dit AI Harnas (${category.title}).",
                requiredLevel = category.roleAllowed.authorityLevel,
                actualLevel = actor.role.authorityLevel,
                violationCode = "HARNESS_ACCESS_DENIED"
            )
        }

        val session = chatGPTAuthManager.sessionState.value ?: chatGPTAuthManager.loadSessionFromDisk()
        if (session == null || !session.isValid) {
            return@withContext SecurityResult.Denied(
                reason = "ChatGPT Auth Token is niet aanwezig of verlopen. Voer eerst een geldige token in via het Admin Dashboard. (Geen Fallback)",
                requiredLevel = 1,
                actualLevel = actor.role.authorityLevel,
                violationCode = "NO_VALID_AUTH_TOKEN"
            )
        }

        val tasks = planningDao.getAllTasks().first()
        val requests = serviceRequestDao.getAllRequests().first()
        val users = userDao.getAllUsers().first()

        val config = aiHarnessEngine.resolveHarnessConfig(
            category = category,
            currentUser = actor,
            activeTasks = tasks,
            activeRequests = requests,
            allUsers = users
        )

        try {
            val responseText = chatGPTAuthManager.streamResponses(
                model = config.allowedModel,
                messages = messages,
                userPrompt = userPrompt,
                systemInstructions = config.systemPrompt,
                toolsArray = config.tools,
                onChunk = onChunk,
                onStatus = onStatus
            )
            SecurityResult.Success(responseText)
        } catch (e: Exception) {
            SecurityResult.Denied(
                reason = "AI Uitvoering mislukt: ${e.message} (Geen Fallback)",
                requiredLevel = category.roleAllowed.authorityLevel,
                actualLevel = actor.role.authorityLevel,
                violationCode = "AI_EXECUTION_ERROR"
            )
        }
    }
}

