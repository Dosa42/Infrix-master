package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val role: UserRole,
    val fullName: String,
    val email: String,
    val phone: String = "",
    val departmentOrCompany: String = "",
    val jobTitle: String = "",             // Functietitel
    val hourlyRate: Double = 0.0,          // Uurtarief
    val assignedClientOrPartner: String = "", // Vaste koppeling (bijv. vaste klant of toegewezen werker)
    val isApproved: Boolean = true,
    val isActive: Boolean = true,
    val isLocked: Boolean = false,         // Beveiligingslockout
    val failedAttempts: Int = 0,           // Mislukte inlogpogingen teller
    val lastLoginAt: Long? = null,         // Datum/tijd laatste succesvolle inlog
    val sessionToken: String = "",         // Actieve sessie token (voor remote force logout)
    // Granulaire permissies per account:
    val canCompleteTasks: Boolean = true,
    val canLogHours: Boolean = true,
    val canSubmitRequests: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val approvedBy: String = "Infrix-dev",
    val approvedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "planning_tasks")
data class PlanningTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val assignedWorkerUsername: String,
    val assignedWorkerName: String,
    val clientUsername: String,
    val clientName: String,
    val status: String, // "Gepland", "In uitvoering", "Gepauzeerd", "Afgerond"
    val priority: String, // "Normaal", "Hoog", "Urgent"
    val scheduledDate: String,
    val estimatedHours: Double,
    val actualHours: Double = 0.0,
    val location: String = "Locatie hoofdkantoor / werkplaats",
    val internalNotes: String = "",
    val clientVisibleStatus: String = "In planning",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "work_logs")
data class WorkLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val taskTitle: String,
    val workerUsername: String,
    val workerName: String,
    val hoursSpent: Double,
    val activityDescription: String,
    val loggedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "service_requests")
data class ServiceRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientUsername: String,
    val clientName: String,
    val title: String,
    val description: String,
    val preferredDate: String,
    val urgency: String = "Normaal",
    val status: String = "Nieuw ingediend",
    val submittedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actorUsername: String,
    val actorRole: String,
    val actionType: String,
    val details: String,
    val severity: String // "INFO", "WARNING", "SECURITY_ALERT"
)

@Entity(
    tableName = "calendar_events",
    indices = [
        Index(value = ["eventDate"]),
        Index(value = ["workerUsername"]),
        Index(value = ["clientUsername"]),
        Index(value = ["relatedTaskId"]),
        Index(value = ["relatedRequestId"])
    ]
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val eventDate: String,                 // formaat: yyyy-MM-dd of dd-MM-yyyy
    val startTime: String = "09:00",       // formaat: HH:mm
    val endTime: String = "11:00",         // formaat: HH:mm
    val startTimestampMillis: Long = System.currentTimeMillis(),
    val endTimestampMillis: Long = System.currentTimeMillis() + 7200000L,
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val workerUsername: String = "",
    val workerName: String = "",
    val clientUsername: String = "",
    val clientName: String = "",
    val relatedTaskId: Long? = null,
    val relatedRequestId: Long? = null,
    val googleSearchQuery: String = "",
    val googleMapsUrl: String = "",
    val syncStatus: String = "SYNCHRONIZED", // "SYNCHRONIZED", "PENDING_SYNC", "LOCAL_ONLY"
    val isSyncedWithGoogleCalendar: Boolean = true,
    val calendarColorHex: String = "#38BDF8", // Kleurcode voor weergave
    val priority: String = "Normaal",       // "Normaal", "Hoog", "Urgent"
    val reminderMinutes: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
