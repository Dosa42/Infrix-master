package com.example.data.security

import com.example.data.model.AuditLogEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import java.security.MessageDigest
import java.util.UUID

sealed class SecurityResult<out T> {
    data class Success<out T>(val data: T) : SecurityResult<T>()
    data class Denied(
        val reason: String,
        val requiredLevel: Int,
        val actualLevel: Int,
        val violationCode: String
    ) : SecurityResult<Nothing>()
}

object SecurityManager {

    // Brute force protection tracker: username/ip -> (failedAttempts, lockUntil)
    private val failedAttemptsMap = mutableMapOf<String, Pair<Int, Long>>()

    fun generateSalt(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }

    fun hashPassword(password: String, salt: String): String {
        val input = "$salt:$password:rolevault_secure_pepper"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(candidate: String, salt: String, storedHash: String): Boolean {
        val candidateHash = hashPassword(candidate, salt)
        return candidateHash == storedHash
    }

    fun checkLoginRateLimit(key: String): Pair<Boolean, Long> {
        val now = System.currentTimeMillis()
        val entry = failedAttemptsMap[key.lowercase()] ?: return Pair(true, 0L)
        val (attempts, lockUntil) = entry
        if (now < lockUntil) {
            val remainingSec = (lockUntil - now) / 1000
            return Pair(false, remainingSec)
        }
        return Pair(true, 0L)
    }

    fun recordLoginFailure(key: String): Int {
        val now = System.currentTimeMillis()
        val current = failedAttemptsMap[key.lowercase()]?.first ?: 0
        val newCount = current + 1
        val lockUntil = if (newCount >= 4) {
            now + (20 * 1000L) // 20 second cooldown after 4 failed attempts
        } else {
            0L
        }
        failedAttemptsMap[key.lowercase()] = Pair(newCount, lockUntil)
        return newCount
    }

    fun recordLoginSuccess(key: String) {
        failedAttemptsMap.remove(key.lowercase())
    }

    /**
     * Enforces Role-Based Access Control (RBAC) hierarchy.
     * Level 3 (ADMIN) > Level 2 (WERKER) > Level 1 (KLANT)
     */
    fun checkPermission(
        actor: UserEntity?,
        requiredRole: UserRole,
        actionDescription: String
    ): SecurityResult<Unit> {
        if (actor == null) {
            return SecurityResult.Denied(
                reason = "Geen actieve sessie gevonden. Log opnieuw in.",
                requiredLevel = requiredRole.authorityLevel,
                actualLevel = 0,
                violationCode = "UNAUTHENTICATED"
            )
        }

        if (!actor.isActive) {
            return SecurityResult.Denied(
                reason = "Gebruikersaccount is gedeactiveerd door een beheerder.",
                requiredLevel = requiredRole.authorityLevel,
                actualLevel = 0,
                violationCode = "ACCOUNT_INACTIVE"
            )
        }

        if (actor.role.authorityLevel >= requiredRole.authorityLevel) {
            return SecurityResult.Success(Unit)
        }

        val reason = "Toegang geweigerd voor rol '${actor.role.displayName}'. Deze actie vereist minimaal autorisatieniveau ${requiredRole.badgeTitle}."
        return SecurityResult.Denied(
            reason = reason,
            requiredLevel = requiredRole.authorityLevel,
            actualLevel = actor.role.authorityLevel,
            violationCode = "INSUFFICIENT_AUTHORITY"
        )
    }

    fun createAuditLog(
        actor: UserEntity?,
        actionType: String,
        details: String,
        severity: String = "INFO"
    ): AuditLogEntity {
        return AuditLogEntity(
            timestamp = System.currentTimeMillis(),
            actorUsername = actor?.username ?: "ANONYMOUS",
            actorRole = actor?.role?.code ?: "NONE",
            actionType = actionType,
            details = details,
            severity = severity
        )
    }
}
