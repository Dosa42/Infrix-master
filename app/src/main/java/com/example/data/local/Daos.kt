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
}

@Dao
interface WorkLogDao {
    @Query("SELECT * FROM work_logs ORDER BY loggedAt DESC")
    fun getAllWorkLogs(): Flow<List<WorkLogEntity>>

    @Query("SELECT * FROM work_logs WHERE LOWER(workerUsername) = LOWER(:workerUsername) ORDER BY loggedAt DESC")
    fun getWorkLogsForWorker(workerUsername: String): Flow<List<WorkLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkLog(log: WorkLogEntity): Long
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
