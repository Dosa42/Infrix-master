package com.example.data.backend

import com.example.data.local.AppDatabase
import com.example.data.model.AuditLogEntity
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class CloudSyncStatus(
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val totalRecordsSynced: Int = 0,
    val serverEndpoint: String = "https://cloud.rolevault.internal/api/v1/sync",
    val syncState: String = "Gereed",
    val backupPayloadSizeKb: Double = 0.0,
    val httpStatusCode: Int? = null,
    val responseBodySummary: String = ""
)

class ServerStorageManager(private val database: AppDatabase) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Serializes entire local SQLite database (Users, Planning, WorkLogs, Requests, Audits, CalendarEvents)
     * into a portable, structured JSON cloud payload for server-side backup & synchronization.
     * Schema version matches current Room database version (6).
     */
    suspend fun generateServerBackupPayload(currentActor: String? = null): String = withContext(Dispatchers.IO) {
        val users = database.userDao().getAllUsers().first()
        val tasks = database.planningDao().getAllTasks().first()
        val workLogs = database.workLogDao().getAllWorkLogs().first()
        val requests = database.serviceRequestDao().getAllRequests().first()
        val audits = database.auditLogDao().getRecentLogs(500).first()
        val calendarEvents = database.calendarDao().getAllEvents().first()

        val rootJson = JSONObject()
        rootJson.put("schemaVersion", 6)
        rootJson.put("exportTimestamp", System.currentTimeMillis())
        rootJson.put("exportedBy", currentActor ?: "Infrix-dev")
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
                put("canCompleteTasks", u.canCompleteTasks)
                put("canLogHours", u.canLogHours)
                put("canSubmitRequests", u.canSubmitRequests)
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
                put("assignedWorkerName", t.assignedWorkerName)
                put("client", t.clientUsername)
                put("clientName", t.clientName)
                put("status", t.status)
                put("priority", t.priority)
                put("scheduledDate", t.scheduledDate)
                put("estimatedHours", t.estimatedHours)
                put("actualHours", t.actualHours)
                put("location", t.location)
                put("internalNotes", t.internalNotes)
                put("clientVisibleStatus", t.clientVisibleStatus)
                put("createdAt", t.createdAt)
                put("updatedAt", t.updatedAt)
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
                put("taskTitle", l.taskTitle)
                put("workerUsername", l.workerUsername)
                put("workerName", l.workerName)
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
                put("clientName", r.clientName)
                put("title", r.title)
                put("description", r.description)
                put("preferredDate", r.preferredDate)
                put("urgency", r.urgency)
                put("status", r.status)
                put("submittedAt", r.submittedAt)
            }
            reqArray.put(rObj)
        }
        rootJson.put("serviceRequests", reqArray)

        // Calendar Events
        val calArray = JSONArray()
        calendarEvents.forEach { c ->
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("description", c.description)
                put("eventDate", c.eventDate)
                put("startTime", c.startTime)
                put("endTime", c.endTime)
                put("startTimestampMillis", c.startTimestampMillis)
                put("endTimestampMillis", c.endTimestampMillis)
                put("location", c.location)
                put("latitude", c.latitude)
                put("longitude", c.longitude)
                put("workerUsername", c.workerUsername)
                put("workerName", c.workerName)
                put("clientUsername", c.clientUsername)
                put("clientName", c.clientName)
                put("relatedTaskId", c.relatedTaskId ?: JSONObject.NULL)
                put("relatedRequestId", c.relatedRequestId ?: JSONObject.NULL)
                put("syncStatus", c.syncStatus)
                put("priority", c.priority)
                put("createdAt", c.createdAt)
                put("updatedAt", c.updatedAt)
            }
            calArray.put(cObj)
        }
        rootJson.put("calendarEvents", calArray)

        // Audit Logs
        val auditArray = JSONArray()
        audits.forEach { a ->
            val aObj = JSONObject().apply {
                put("id", a.id)
                put("actor", a.actorUsername)
                put("role", a.actorRole)
                put("actionType", a.actionType)
                put("details", a.details)
                put("severity", a.severity)
                put("timestamp", a.timestamp)
            }
            auditArray.put(aObj)
        }
        rootJson.put("auditLogs", auditArray)

        rootJson.toString(2)
    }

    /**
     * Executes real HTTP POST network synchronization of the local database payload to the remote server endpoint.
     */
    suspend fun syncToServerEndpoint(
        endpointUrl: String,
        actorUsername: String = "Infrix-dev",
        actorRole: String = "ADMIN"
    ): CloudSyncStatus = withContext(Dispatchers.IO) {
        val payload = generateServerBackupPayload(currentActor = actorUsername)
        val sizeKb = payload.toByteArray(Charsets.UTF_8).size / 1024.0
        val totalRecords = database.userDao().getUserCount() +
            database.planningDao().getAllTasks().first().size +
            database.calendarDao().getAllEvents().first().size

        val auditDao = database.auditLogDao()

        // Validate endpoint URL format
        val cleanUrl = endpointUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            val errMsg = "Ongeldige server URL: moet starten met http:// of https://"
            auditDao.insertLog(
                AuditLogEntity(
                    actorUsername = actorUsername,
                    actorRole = actorRole,
                    actionType = "SERVER_SYNC_ERROR",
                    details = "Synchronisatie mislukt: $errMsg ($cleanUrl)",
                    severity = "WARNING"
                )
            )
            return@withContext CloudSyncStatus(
                lastSyncTimestamp = System.currentTimeMillis(),
                totalRecordsSynced = 0,
                serverEndpoint = cleanUrl,
                syncState = "Fout: Ongeldige URL",
                backupPayloadSizeKb = Math.round(sizeKb * 100.0) / 100.0,
                httpStatusCode = null,
                responseBodySummary = errMsg
            )
        }

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(cleanUrl)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("User-Agent", "infrix-mobile-sync/1.0")
                .header("X-Sync-Schema-Version", "6")
                .header("X-Sync-Actor", actorUsername)
                .header("ngrok-skip-browser-warning", "true")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string()?.take(300) ?: ""

            if (response.isSuccessful) {
                auditDao.insertLog(
                    AuditLogEntity(
                        actorUsername = actorUsername,
                        actorRole = actorRole,
                        actionType = "SERVER_SYNC_SUCCESS",
                        details = "Lokale database succesvol gesynchroniseerd naar server endpoint ($cleanUrl). HTTP $responseCode. Dataomvang: ${String.format("%.2f", sizeKb)} KB ($totalRecords records).",
                        severity = "INFO"
                    )
                )

                CloudSyncStatus(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    totalRecordsSynced = totalRecords,
                    serverEndpoint = cleanUrl,
                    syncState = "Gesynchroniseerd (HTTP $responseCode)",
                    backupPayloadSizeKb = Math.round(sizeKb * 100.0) / 100.0,
                    httpStatusCode = responseCode,
                    responseBodySummary = responseBody.ifBlank { "OK" }
                )
            } else {
                auditDao.insertLog(
                    AuditLogEntity(
                        actorUsername = actorUsername,
                        actorRole = actorRole,
                        actionType = "SERVER_SYNC_HTTP_ERROR",
                        details = "Server antwoordde met HTTP status $responseCode op endpoint ($cleanUrl). Respons: ${responseBody.take(100)}",
                        severity = "WARNING"
                    )
                )

                CloudSyncStatus(
                    lastSyncTimestamp = System.currentTimeMillis(),
                    totalRecordsSynced = 0,
                    serverEndpoint = cleanUrl,
                    syncState = "Server Fout (HTTP $responseCode)",
                    backupPayloadSizeKb = Math.round(sizeKb * 100.0) / 100.0,
                    httpStatusCode = responseCode,
                    responseBodySummary = responseBody
                )
            }
        } catch (e: IOException) {
            val netError = e.localizedMessage ?: e.message ?: "Netwerkverbinding time-out / niet bereikbaar"
            auditDao.insertLog(
                AuditLogEntity(
                    actorUsername = actorUsername,
                    actorRole = actorRole,
                    actionType = "SERVER_SYNC_FAILED",
                    details = "Netwerkfout bij synchroniseren naar ($cleanUrl): $netError",
                    severity = "WARNING"
                )
            )

            CloudSyncStatus(
                lastSyncTimestamp = System.currentTimeMillis(),
                totalRecordsSynced = 0,
                serverEndpoint = cleanUrl,
                syncState = "Offline / Niet Bereikbaar ($netError)",
                backupPayloadSizeKb = Math.round(sizeKb * 100.0) / 100.0,
                httpStatusCode = null,
                responseBodySummary = netError
            )
        } catch (e: Exception) {
            val err = e.localizedMessage ?: "Onverwachte synchronisatiefout"
            auditDao.insertLog(
                AuditLogEntity(
                    actorUsername = actorUsername,
                    actorRole = actorRole,
                    actionType = "SERVER_SYNC_ERROR",
                    details = "Fout bij synchroniseren: $err",
                    severity = "WARNING"
                )
            )

            CloudSyncStatus(
                lastSyncTimestamp = System.currentTimeMillis(),
                totalRecordsSynced = 0,
                serverEndpoint = cleanUrl,
                syncState = "Fout: $err",
                backupPayloadSizeKb = Math.round(sizeKb * 100.0) / 100.0,
                httpStatusCode = null,
                responseBodySummary = err
            )
        }
    }
}
