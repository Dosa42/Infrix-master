package com.example.data.backend

import com.example.data.local.AppDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import com.example.data.model.UserEntity
import com.example.data.model.WorkLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CloudSyncStatus(
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val totalRecordsSynced: Int = 0,
    val serverEndpoint: String = "https://cloud.rolevault.internal/api/v1/sync",
    val syncState: String = "Gereed",
    val backupPayloadSizeKb: Double = 0.0
)

class ServerStorageManager(private val database: AppDatabase) {

    /**
     * Serializes entire local SQLite database (Users, Planning, WorkLogs, Requests, Audits)
     * into a portable, structured JSON cloud payload for server-side backup & synchronization.
     */
    suspend fun generateServerBackupPayload(): String = withContext(Dispatchers.IO) {
        val users = database.userDao().getAllUsers().first()
        val tasks = database.planningDao().getAllTasks().first()
        val workLogs = database.workLogDao().getAllWorkLogs().first()
        val requests = database.serviceRequestDao().getAllRequests().first()
        val audits = database.auditLogDao().getRecentLogs(500).first()

        val rootJson = JSONObject()
        rootJson.put("schemaVersion", 5)
        rootJson.put("exportTimestamp", System.currentTimeMillis())
        rootJson.put("exportedBy", "Infrix-dev")
        rootJson.put("systemNode", "Android-Backend-Node-Primary")

        // Users
        val usersArray = JSONArray()
        users.forEach { u ->
            val uObj = JSONObject().apply {
                put("id", u.id)
                put("username", u.username)
                put("role", u.role.name)
                put("fullName", u.fullName)
                put("email", u.email)
                put("phone", u.phone)
                put("departmentOrCompany", u.departmentOrCompany)
                put("jobTitle", u.jobTitle)
                put("hourlyRate", u.hourlyRate)
                put("assignedPartner", u.assignedClientOrPartner)
                put("isApproved", u.isApproved)
                put("isActive", u.isActive)
                put("isLocked", u.isLocked)
                put("failedAttempts", u.failedAttempts)
                put("lastLoginAt", u.lastLoginAt ?: 0L)
            }
            usersArray.put(uObj)
        }
        rootJson.put("users", usersArray)

        // Tasks
        val tasksArray = JSONArray()
        tasks.forEach { t ->
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("description", t.description)
                put("assignedWorker", t.assignedWorkerUsername)
                put("client", t.clientUsername)
                put("status", t.status)
                put("priority", t.priority)
                put("scheduledDate", t.scheduledDate)
                put("estimatedHours", t.estimatedHours)
                put("actualHours", t.actualHours)
                put("location", t.location)
            }
            tasksArray.put(tObj)
        }
        rootJson.put("planningTasks", tasksArray)

        // Work Logs
        val logsArray = JSONArray()
        workLogs.forEach { l ->
            val lObj = JSONObject().apply {
                put("id", l.id)
                put("taskId", l.taskId)
                put("workerUsername", l.workerUsername)
                put("hoursSpent", l.hoursSpent)
                put("activityDescription", l.activityDescription)
                put("loggedAt", l.loggedAt)
            }
            logsArray.put(lObj)
        }
        rootJson.put("workLogs", logsArray)

        // Service Requests
        val reqArray = JSONArray()
        requests.forEach { r ->
            val rObj = JSONObject().apply {
                put("id", r.id)
                put("clientUsername", r.clientUsername)
                put("title", r.title)
                put("urgency", r.urgency)
                put("status", r.status)
                put("submittedAt", r.submittedAt)
            }
            reqArray.put(rObj)
        }
        rootJson.put("serviceRequests", reqArray)

        // Audit Logs
        val auditArray = JSONArray()
        audits.forEach { a ->
            val aObj = JSONObject().apply {
                put("id", a.id)
                put("actor", a.actorUsername)
                put("role", a.actorRole)
                put("actionType", a.actionType)
                put("details", a.details)
                put("timestamp", a.timestamp)
            }
            auditArray.put(aObj)
        }
        rootJson.put("auditLogs", auditArray)

        rootJson.toString(2)
    }

    /**
     * Simulates syncing local database payload to the remote server storage endpoint
     */
    suspend fun syncToServerEndpoint(endpointUrl: String): CloudSyncStatus = withContext(Dispatchers.IO) {
        val payload = generateServerBackupPayload()
        val sizeKb = payload.toByteArray(Charsets.UTF_8).size / 1024.0

        val usersCount = database.userDao().getUserCount()
        val auditDao = database.auditLogDao()

        auditDao.insertLog(
            AuditLogEntity(
                actorUsername = "Infrix-dev",
                actorRole = "ADMIN",
                actionType = "SERVER_SYNC_SUCCESS",
                details = "Lokale database gesynchroniseerd naar server endpoint ($endpointUrl). Dataomvang: ${String.format("%.2f", sizeKb)} KB.",
                severity = "INFO"
            )
        )

        CloudSyncStatus(
            lastSyncTimestamp = System.currentTimeMillis(),
            totalRecordsSynced = usersCount,
            serverEndpoint = endpointUrl,
            syncState = "Gesynchroniseerd (Succes)",
            backupPayloadSizeKb = Math.round(sizeKb * 100.0) / 100.0
        )
    }
}
