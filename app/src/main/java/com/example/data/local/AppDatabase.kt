package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.CalendarEventEntity
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WorkLogEntity
import com.example.data.security.SecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        PlanningTaskEntity::class,
        WorkLogEntity::class,
        ServiceRequestEntity::class,
        AuditLogEntity::class,
        CalendarEventEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(RoleConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun planningDao(): PlanningDao
    abstract fun workLogDao(): WorkLogDao
    abstract fun serviceRequestDao(): ServiceRequestDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun calendarDao(): CalendarDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rolevault_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    ensureAdminAccount(database)
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    ensureAdminAccount(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun ensureAdminAccount(database: AppDatabase) {
            val userDao = database.userDao()
            val auditLogDao = database.auditLogDao()

            val existing = userDao.getUserByUsername("Infrix-dev")
            if (existing == null) {
                val salt = SecurityManager.generateSalt()
                val adminUser = UserEntity(
                    username = "Infrix-dev",
                    passwordHash = SecurityManager.hashPassword("infrix-yakup1903@", salt),
                    salt = salt,
                    role = UserRole.ADMIN,
                    fullName = "Infrix-dev",
                    email = "infrix-dev@admin.local",
                    phone = "Interne directielijn",
                    departmentOrCompany = "Centraal Beheer & Directie",
                    jobTitle = "Chief Administrator",
                    hourlyRate = 0.0,
                    isApproved = true,
                    isActive = true,
                    isLocked = false,
                    canCompleteTasks = true,
                    canLogHours = true,
                    canSubmitRequests = true,
                    approvedBy = "SYSTEM",
                    approvedAt = System.currentTimeMillis()
                )
                userDao.insertUser(adminUser)

                auditLogDao.insertLog(
                    AuditLogEntity(
                        actorUsername = "SYSTEM",
                        actorRole = "SYSTEM",
                        actionType = "ADMIN_PROVISIONED",
                        details = "Hoogste authority Admin account 'Infrix-dev' geconfigureerd met volledige rechten.",
                        severity = "INFO"
                    )
                )
            }
        }
    }
}
