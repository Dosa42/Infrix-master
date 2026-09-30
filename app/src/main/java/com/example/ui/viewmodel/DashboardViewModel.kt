package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLogEntity
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WorkLogEntity
import com.example.data.repository.AppRepository
import com.example.data.security.SecurityResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SecurityAlertData(
    val title: String,
    val message: String,
    val requiredLevel: Int,
    val actualLevel: Int,
    val violationCode: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(private val repository: AppRepository) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    val allTasks: StateFlow<List<PlanningTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkLogs: StateFlow<List<WorkLogEntity>> = repository.allWorkLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allServiceRequests: StateFlow<List<ServiceRequestEntity>> = repository.allServiceRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.recentAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCalendarEvents: StateFlow<List<com.example.data.model.CalendarEventEntity>> = repository.allCalendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workerCalendarEvents: StateFlow<List<com.example.data.model.CalendarEventEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getCalendarEventsForWorker(user.username)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientCalendarEvents: StateFlow<List<com.example.data.model.CalendarEventEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getCalendarEventsForClient(user.username)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workerTasks: StateFlow<List<PlanningTaskEntity>> = currentUser.flatMapLatest { user ->
        if (user != null && user.role.authorityLevel >= UserRole.WERKER.authorityLevel) {
            repository.getTasksForWorker(user.username)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientTasks: StateFlow<List<PlanningTaskEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getTasksForClient(user.username)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clientServiceRequests: StateFlow<List<ServiceRequestEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getRequestsForClient(user.username)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workerWorkLogs: StateFlow<List<WorkLogEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.getWorkLogsForWorker(user.username)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _securityAlert = MutableStateFlow<SecurityAlertData?>(null)
    val securityAlert: StateFlow<SecurityAlertData?> = _securityAlert.asStateFlow()

    fun dismissSecurityAlert() {
        _securityAlert.value = null
    }

    // ==========================================
    // DE 5 PIJLERS VAN VOLLEDIG ACCOUNTBEHEER
    // ==========================================

    // 1. Profiel- & Gegevensbewerking
    fun updateAccountProfile(
        user: UserEntity,
        fullName: String,
        email: String,
        phone: String,
        dept: String,
        jobTitle: String,
        hourlyRate: Double,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.updateAccountProfile(user, fullName, email, phone, dept, jobTitle, hourlyRate)) {
                is SecurityResult.Success -> onComplete(true)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false)
                }
            }
        }
    }

    // 2. Rolwijziging / Promotie
    fun changeUserRole(user: UserEntity, newRole: UserRole) {
        viewModelScope.launch {
            when (val res = repository.changeUserRole(user, newRole)) {
                is SecurityResult.Success -> { /* flow updates list */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    // 3. Beveiliging: Ontgrendelen & Force Logout
    fun unlockAccount(user: UserEntity) {
        viewModelScope.launch {
            when (val res = repository.unlockAccount(user)) {
                is SecurityResult.Success -> { /* flow updates list */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    fun forceRemoteLogout(user: UserEntity) {
        viewModelScope.launch {
            when (val res = repository.forceRemoteLogout(user)) {
                is SecurityResult.Success -> { /* flow updates list */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    // 4. Granulaire Rechten & Toegangsfilters
    fun updateGranularPermissions(
        user: UserEntity,
        canCompleteTasks: Boolean,
        canLogHours: Boolean,
        canSubmitRequests: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.updateGranularPermissions(user, canCompleteTasks, canLogHours, canSubmitRequests)) {
                is SecurityResult.Success -> onComplete()
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    // 5. Koppeling & Vaste Toewijzing
    fun assignClientOrPartner(user: UserEntity, assignedEntity: String) {
        viewModelScope.launch {
            when (val res = repository.assignClientOrPartner(user, assignedEntity)) {
                is SecurityResult.Success -> { /* flow updates list */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    // Algemene Admin Acties
    fun createWorkerOrClientAccount(
        username: String,
        passwordPlain: String,
        role: UserRole,
        fullName: String,
        email: String,
        phone: String,
        departmentOrCompany: String,
        jobTitle: String,
        hourlyRate: Double,
        assignedPartner: String,
        autoApprove: Boolean,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.createUserByAdmin(
                username = username,
                passwordPlain = passwordPlain,
                role = role,
                fullName = fullName,
                email = email,
                phone = phone,
                departmentOrCompany = departmentOrCompany,
                jobTitle = jobTitle,
                hourlyRate = hourlyRate,
                assignedPartner = assignedPartner,
                autoApprove = autoApprove
            )) {
                is SecurityResult.Success -> onResult(null)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onResult(res.reason)
                }
            }
        }
    }

    fun approveUser(user: UserEntity) {
        viewModelScope.launch {
            when (val res = repository.approveUser(user)) {
                is SecurityResult.Success -> { /* flow updates list */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    fun toggleUserActive(user: UserEntity) {
        viewModelScope.launch {
            when (val res = repository.toggleUserActive(user)) {
                is SecurityResult.Success -> { /* flow updates list */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    fun resetUserPassword(user: UserEntity, newPasswordPlain: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.resetUserPassword(user, newPasswordPlain)) {
                is SecurityResult.Success -> onComplete(true)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false)
                }
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            when (val res = repository.deleteUser(user)) {
                is SecurityResult.Success -> { /* flow updates list */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    // Planning
    fun createPlanningTask(
        title: String,
        description: String,
        assignedWorkerUsername: String,
        assignedWorkerName: String,
        clientUsername: String,
        clientName: String,
        priority: String,
        scheduledDate: String,
        estimatedHours: Double,
        location: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val task = PlanningTaskEntity(
                title = title,
                description = description,
                assignedWorkerUsername = assignedWorkerUsername,
                assignedWorkerName = assignedWorkerName,
                clientUsername = clientUsername,
                clientName = clientName,
                status = "Gepland",
                priority = priority,
                scheduledDate = scheduledDate,
                estimatedHours = estimatedHours,
                location = location,
                clientVisibleStatus = "In planning voor $scheduledDate"
            )
            when (val res = repository.createPlanningTask(task)) {
                is SecurityResult.Success -> onComplete(true)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false)
                }
            }
        }
    }

    fun deleteTask(task: PlanningTaskEntity) {
        viewModelScope.launch {
            when (val res = repository.deleteTask(task)) {
                is SecurityResult.Success -> { /* updated via flow */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    fun updateTaskStatus(taskId: Long, newStatus: String, notes: String? = null) {
        viewModelScope.launch {
            when (val res = repository.updateTaskStatus(taskId, newStatus, notes = notes)) {
                is SecurityResult.Success -> { /* updated via flow */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    fun logHours(taskId: Long, taskTitle: String, hours: Double, activity: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            when (val res = repository.logWorkHours(taskId, taskTitle, hours, activity)) {
                is SecurityResult.Success -> onComplete()
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    fun updateRequestStatus(requestId: Long, newStatus: String) {
        viewModelScope.launch {
            when (val res = repository.updateRequestStatus(requestId, newStatus)) {
                is SecurityResult.Success -> { /* updated via flow */ }
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    fun submitServiceRequest(
        title: String,
        description: String,
        preferredDate: String,
        urgency: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.submitServiceRequest(title, description, preferredDate, urgency)) {
                is SecurityResult.Success -> onComplete(true)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false)
                }
            }
        }
    }

    // ==========================================
    // BACKEND ARCHITECTUUR & INTEGRATIE TOOLS
    // ==========================================

    // 1. Permissions & Hardware Telemetry
    fun getHardwarePermissionsStatus(): List<com.example.data.backend.DevicePermissionStatus> {
        return repository.deviceCapabilitiesManager.checkAllPermissions()
    }

    fun getDeviceGpsLocation(): com.example.data.backend.GeoLocationData? {
        return repository.deviceCapabilitiesManager.getCurrentLocation()
    }

    fun dialPhone(phoneNumber: String) {
        repository.deviceCapabilitiesManager.dialPhoneNumber(phoneNumber)
    }

    // 2. E-mail Dispatcher
    fun sendNativeEmail(recipient: String, subject: String, body: String): com.example.data.backend.EmailDispatchResult {
        return repository.emailBackendService.sendNativeEmail(recipient, subject, body)
    }

    fun generateWelcomeEmailTemplate(username: String, fullName: String, role: String, tempPass: String): Pair<String, String> {
        return repository.emailBackendService.buildAccountCreatedEmail(username, fullName, role, tempPass)
    }

    fun generateTaskEmailTemplate(workerName: String, title: String, date: String, loc: String): Pair<String, String> {
        return repository.emailBackendService.buildTaskAssignedEmail(workerName, title, date, loc)
    }

    // 3. Google Maps & Geocoding
    fun openGoogleMapsNavigation(destination: String): Boolean {
        return repository.googleMapsIntegrationService.openNavigation(destination)
    }

    fun showMapLocation(locationQuery: String): Boolean {
        return repository.googleMapsIntegrationService.showLocationOnMap(locationQuery)
    }

    fun calculateGpsDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): com.example.data.backend.RouteCalculationResult {
        return repository.googleMapsIntegrationService.calculateDistance(lat1, lon1, lat2, lon2)
    }

    // 4. Google & Web Search Integration
    fun launchGoogleSearch(query: String): Boolean {
        return repository.searchIntegrationService.launchGoogleSearchIntent(query)
    }

    fun executeWebSearch(query: String, onResults: (List<com.example.data.backend.SearchResultItem>) -> Unit) {
        viewModelScope.launch {
            val results = repository.searchIntegrationService.executeLiveQuery(query)
            onResults(results)
        }
    }

    // 5. Server Side Storage & Cloud Sync
    fun exportCompleteDatabaseJson(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.serverStorageManager.generateServerBackupPayload()
            onComplete(json)
        }
    }

    fun syncDatabaseToServer(endpoint: String, onComplete: (com.example.data.backend.CloudSyncStatus) -> Unit) {
        viewModelScope.launch {
            val status = repository.serverStorageManager.syncToServerEndpoint(endpoint)
            onComplete(status)
        }
    }

    // 6. Backend Agenda & Kalender Synchronisatie
    fun createCalendarEvent(
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
        calendarColorHex: String = "#38BDF8",
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.createCalendarEvent(
                title, description, eventDate, startTime, endTime,
                location, workerUsername, workerName, clientUsername, clientName, priority, calendarColorHex
            )) {
                is SecurityResult.Success -> onComplete(true)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false)
                }
            }
        }
    }

    fun updateCalendarEvent(
        event: com.example.data.model.CalendarEventEntity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.updateCalendarEvent(event)) {
                is SecurityResult.Success -> onComplete(true)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false)
                }
            }
        }
    }

    fun deleteCalendarEvent(
        id: Long,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.deleteCalendarEvent(id)) {
                is SecurityResult.Success -> onComplete(true)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false)
                }
            }
        }
    }

    fun syncAllToCalendar(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.syncAllEntitiesToCalendar()) {
                is SecurityResult.Success -> onComplete(res.data)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(0)
                }
            }
        }
    }

    fun exportCalendarIcs(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val events = allCalendarEvents.value
            val ics = repository.calendarBackendService.exportToIcs(events)
            onComplete(ics)
        }
    }

    fun getGoogleCalendarIntent(event: com.example.data.model.CalendarEventEntity): android.content.Intent {
        return repository.calendarBackendService.createGoogleCalendarIntent(event)
    }

    fun getGoogleMapsRouteIntent(location: String): android.content.Intent {
        return repository.calendarBackendService.createGoogleMapsRouteIntent(location)
    }

    fun getGoogleSearchIntent(query: String): android.content.Intent {
        return repository.calendarBackendService.createGoogleSearchIntent(query)
    }

    val chatGPTSession: StateFlow<com.example.data.auth.ChatGPTSession?> = repository.chatGPTSession
    val chatGPTModels: StateFlow<List<com.example.data.auth.ChatGPTModelInfo>> = repository.chatGPTModels
    val activeChatGPTModel: StateFlow<String> = repository.activeChatGPTModel
    val selectedReasoningEffort: StateFlow<String?> = repository.selectedReasoningEffort

    fun setActiveModel(modelId: String) {
        repository.setActiveChatGPTModel(modelId)
    }

    fun setSelectedReasoningEffort(effort: String?) {
        repository.setSelectedReasoningEffort(effort)
    }

    // 7. ChatGPT Token Injectie & 3-Tier AI Harness
    fun injectChatGPTToken(rawTokenOrJson: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.injectChatGPTToken(rawTokenOrJson)) {
                is SecurityResult.Success -> {
                    onComplete(true, "Token succesvol gevalideerd en opgeslagen. AI Backend is gewapend!")
                }
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false, res.reason)
                }
            }
        }
    }

    fun clearChatGPTSession(onComplete: () -> Unit) {
        viewModelScope.launch {
            when (val res = repository.clearChatGPTSession()) {
                is SecurityResult.Success -> onComplete()
                is SecurityResult.Denied -> showDeniedAlert(res)
            }
        }
    }

    fun sendHarnessPrompt(
        category: com.example.data.ai.AIHarnessCategory,
        messages: List<com.example.data.auth.ChatMessage>,
        userPrompt: String,
        overrideModel: String? = null,
        overrideReasoningEffort: String? = null,
        onChunk: (String) -> Unit,
        onStatus: (String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.executeHarnessStream(
                category = category,
                messages = messages,
                userPrompt = userPrompt,
                selectedModel = overrideModel,
                reasoningEffort = overrideReasoningEffort,
                onChunk = onChunk,
                onStatus = onStatus
            )) {
                is SecurityResult.Success -> onComplete(true, res.data)
                is SecurityResult.Denied -> {
                    showDeniedAlert(res)
                    onComplete(false, res.reason)
                }
            }
        }
    }

    private fun showDeniedAlert(denied: SecurityResult.Denied) {
        _securityAlert.value = SecurityAlertData(
            title = "Toegang Geweigerd",
            message = denied.reason,
            requiredLevel = denied.requiredLevel,
            actualLevel = denied.actualLevel,
            violationCode = denied.violationCode
        )
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
