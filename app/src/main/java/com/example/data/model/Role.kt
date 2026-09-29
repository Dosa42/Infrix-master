package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class UserRole(
    val code: String,
    val displayName: String,
    val authorityLevel: Int,
    val badgeTitle: String,
    val description: String
) {
    ADMIN(
        code = "ADMIN",
        displayName = "Admin",
        authorityLevel = 3,
        badgeTitle = "Hoogste Autoriteit (L3)",
        description = "Volledige bevoegdheid: Gebruikersbeheer, master planning, audit-logs en systeeminstellingen."
    ),
    WERKER(
        code = "WERKER",
        displayName = "Werker",
        authorityLevel = 2,
        badgeTitle = "Uitvoerend & Planning (L2)",
        description = "Toegang tot operationele planning, statusupdates, werkbonnen en urenregistratie."
    ),
    KLANT(
        code = "KLANT",
        displayName = "Klant",
        authorityLevel = 1,
        badgeTitle = "Beperkte Leesrechten (L1)",
        description = "Beperkte rechten: Inzien van eigen projectstatus, planningsoverzicht en aanvragen indienen."
    );

    companion object {
        fun fromString(value: String): UserRole {
            return entries.firstOrNull { 
                it.code.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: KLANT
        }
    }
}
