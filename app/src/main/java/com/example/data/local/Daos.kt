package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditLogEntity
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import com.example.data.model.UserEntity
import com.example.data.model.WorkLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface PlanningDao {
    @Query("SELECT * FROM planning_tasks ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<PlanningTaskEntity>>

    @Query("SELECT * FROM planning_tasks WHERE LOWER(assignedWorkerUsername) = LOWER(:workerUsername) ORDER BY updatedAt DESC")
    fun getTasksForWorker(workerUsername: String): Flow<List<PlanningTaskEntity>>

    @Query("SELECT * FROM planning_tasks WHERE LOWER(clientUsername) = LOWER(:clientUsername) ORDER BY updatedAt DESC")
    fun getTasksForClient(clientUsername: String): Flow<List<PlanningTaskEntity>>

    @Query("SELECT * FROM planning_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): PlanningTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlanningTaskEntity): Long

    @Update
    suspend fun updateTask(task: PlanningTaskEntity)

    @Delete
    suspend fun deleteTask(task: PlanningTaskEntity)

    @Query("DELETE FROM planning_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long): Int

    @Query("DELETE FROM planning_tasks WHERE LOWER(assignedWorkerUsername) = LOWER(:workerUsername)")
    suspend fun deleteTasksByWorker(workerUsername: String): Int

    @Query("DELETE FROM planning_tasks WHERE LOWER(clientUsername) = LOWER(:clientUsername)")
    suspend fun deleteTasksByClient(clientUsername: String): Int
}

@Dao
interface WorkLogDao {
    @Query("SELECT * FROM work_logs ORDER BY loggedAt DESC")
    fun getAllWorkLogs(): Flow<List<WorkLogEntity>>

    @Query("SELECT * FROM work_logs WHERE LOWER(workerUsername) = LOWER(:workerUsername) ORDER BY loggedAt DESC")
    fun getWorkLogsForWorker(workerUsername: String): Flow<List<WorkLogEntity>>

    @Query("SELECT * FROM work_logs WHERE taskId = :taskId ORDER BY loggedAt DESC")
    fun getWorkLogsForTask(taskId: Long): Flow<List<WorkLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkLog(log: WorkLogEntity): Long

    @Query("DELETE FROM work_logs WHERE taskId = :taskId")
    suspend fun deleteWorkLogsByTaskId(taskId: Long): Int

    @Query("DELETE FROM work_logs WHERE LOWER(workerUsername) = LOWER(:workerUsername)")
    suspend fun deleteWorkLogsByWorker(workerUsername: String): Int
}

@Dao
interface ServiceRequestDao {
    @Query("SELECT * FROM service_requests ORDER BY submittedAt DESC")
    fun getAllRequests(): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE LOWER(clientUsername) = LOWER(:clientUsername) ORDER BY submittedAt DESC")
    fun getRequestsForClient(clientUsername: String): Flow<List<ServiceRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: ServiceRequestEntity): Long

    @Update
    suspend fun updateRequest(request: ServiceRequestEntity)

    @Query("UPDATE service_requests SET status = :status WHERE id = :id")
    suspend fun updateRequestStatus(id: Long, status: String)

    @Query("DELETE FROM service_requests WHERE id = :id")
    suspend fun deleteRequestById(id: Long): Int

    @Query("DELETE FROM service_requests WHERE LOWER(clientUsername) = LOWER(:clientUsername)")
    suspend fun deleteRequestsByClient(clientUsername: String): Int
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY startTimestampMillis ASC")
    fun getAllEvents(): Flow<List<com.example.data.model.CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE eventDate = :date ORDER BY startTimestampMillis ASC")
    fun getEventsForDate(date: String): Flow<List<com.example.data.model.CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE LOWER(workerUsername) = LOWER(:workerUsername) ORDER BY startTimestampMillis ASC")
    fun getEventsForWorker(workerUsername: String): Flow<List<com.example.data.model.CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE LOWER(clientUsername) = LOWER(:clientUsername) ORDER BY startTimestampMillis ASC")
    fun getEventsForClient(clientUsername: String): Flow<List<com.example.data.model.CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): com.example.data.model.CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE relatedTaskId = :taskId LIMIT 1")
    suspend fun getEventByTaskId(taskId: Long): com.example.data.model.CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE relatedRequestId = :requestId LIMIT 1")
    suspend fun getEventByRequestId(requestId: Long): com.example.data.model.CalendarEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: com.example.data.model.CalendarEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<com.example.data.model.CalendarEventEntity>)

    @Update
    suspend fun updateEvent(event: com.example.data.model.CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(event: com.example.data.model.CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM calendar_events WHERE relatedTaskId = :taskId")
    suspend fun deleteEventByTaskId(taskId: Long)

    @Query("DELETE FROM calendar_events WHERE relatedRequestId = :requestId")
    suspend fun deleteEventByRequestId(requestId: Long)

    @Query("DELETE FROM calendar_events WHERE LOWER(workerUsername) = LOWER(:workerUsername)")
    suspend fun deleteEventsByWorker(workerUsername: String): Int

    @Query("DELETE FROM calendar_events WHERE LOWER(clientUsername) = LOWER(:clientUsername)")
    suspend fun deleteEventsByClient(clientUsername: String): Int

    @Query("DELETE FROM calendar_events")
    suspend fun clearAllEvents()
}
