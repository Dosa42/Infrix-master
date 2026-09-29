package com.example.data.backend

import android.content.Context
import android.content.Intent
import android.net.Uri

data class EmailDispatchResult(
    val isSuccess: Boolean,
    val message: String,
    val recipient: String,
    val subject: String
)

class EmailBackendService(private val context: Context) {

    /**
     * Dispatches an email via Android native mail intent (ACTION_SENDTO / mailto)
     */
    fun sendNativeEmail(
        recipient: String,
        subject: String,
        body: String,
        cc: List<String> = emptyList()
    ): EmailDispatchResult {
        return try {
            val mailtoUri = Uri.parse("mailto:${Uri.encode(recipient.trim())}")
            val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                if (cc.isNotEmpty()) {
                    putExtra(Intent.EXTRA_CC, cc.toTypedArray())
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            EmailDispatchResult(
                isSuccess = true,
                message = "E-mailclient succesvol gestart met vooraf ingevulde parameters.",
                recipient = recipient,
                subject = subject
            )
        } catch (e: Exception) {
            EmailDispatchResult(
                isSuccess = false,
                message = "Fout bij opstarten e-mailclient: ${e.localizedMessage ?: "Geen geschikte e-mailapp gevonden"}",
                recipient = recipient,
                subject = subject
            )
        }
    }

    /**
     * Formats an automated system notification email template
     */
    fun buildAccountCreatedEmail(username: String, fullName: String, role: String, tempPass: String): Pair<String, String> {
        val subject = "Welkom bij het Portaal - Uw accountgegevens"
        val body = """
            Beste $fullName,
            
            Uw account is succesvol aangemaakt door de beheerder (Infrix-dev).
            
            Uw inloggegevens:
            - Rol: $role
            - Gebruikersnaam: $username
            - Wachtwoord: $tempPass
            
            Log in via de mobiele app om uw taken en werkorders in te zien.
            
            Met vriendelijke groet,
            Infrix-dev Centraal Beheer
        """.trimIndent()
        return Pair(subject, body)
    }

    fun buildTaskAssignedEmail(workerName: String, taskTitle: String, date: String, location: String): Pair<String, String> {
        val subject = "Nieuwe Taak Toegewezen: $taskTitle"
        val body = """
            Beste $workerName,
            
            Er is een nieuwe planningstaak aan u toegewezen:
            
            - Taak: $taskTitle
            - Geplande datum: $date
            - Locatie: $location
            
            Bekijk de details en start uw urenregistratie in de app.
            
            Met vriendelijke groet,
            Planning & Coördinatie
        """.trimIndent()
        return Pair(subject, body)
    }

    fun buildSecurityAlertEmail(adminEmail: String, alertDetail: String): Pair<String, String> {
        val subject = "[BEVEILIGINGSALERT] Onregelmatigheid Gedetecteerd"
        val body = """
            Aan de beheerder van Infrix-dev,
            
            Er is een beveiligingsincident of lockout geregistreerd:
            
            $alertDetail
            
            Tijdstip: ${java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            
            Open het beheerderdashboard om de status van het account te controleren.
        """.trimIndent()
        return Pair(subject, body)
    }
}
