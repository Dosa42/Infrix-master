package com.example.data.ai

import com.example.data.auth.ChatMessage
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import org.json.JSONArray
import org.json.JSONObject

enum class AIHarnessCategory(
    val title: String,
    val description: String,
    val roleAllowed: UserRole
) {
    KLANT(
        title = "Harnas 1: Klant AI Assistent",
        description = "Strikte sandbox. Beantwoordt uitsluitend klantvragen op basis van vooraf goedgekeurde kennis. Geen toegang tot werker- of admindata.",
        roleAllowed = UserRole.KLANT
    ),
    WERKER(
        title = "Harnas 2: Werker AI Co-Pilot",
        description = "Deterministische policy, operationele regels en taakinstructies. Rechterhand voor monteurs en werkuitvoering.",
        roleAllowed = UserRole.WERKER
    ),
    ADMIN(
        title = "Harnas 3: Admin AI Master Orchestrator",
        description = "Volledige autoriteit, dev tools, skills en centrale brug die automatisch Klant- en Werker AI configureert en beheert.",
        roleAllowed = UserRole.ADMIN
    )
}

data class HarnessPromptConfiguration(
    val systemPrompt: String,
    val tools: JSONArray,
    val allowedModel: String,
    val temperature: Double,
    val maxTokens: Int,
    val bridgeMetadata: String
)

class AIHarnessEngine {

    // Kennisbasis beheerd door Admin AI Brug
    private var customerFaqKnowledge: String = """
        - Bedrijfsnaam: RoleVault Diensten & Techniek
        - Openingstijden: Maandag t/m Vrijdag 08:00 - 18:00
        - Werkgebied: Heel Nederland en Vlaanderen
        - Spoedgevallen: 24/7 bereikbaar via spoedknop in het portaal
        - Garantie op werkzaamheden: Standaard 24 maanden fabrieks- en installatiegarantie
        - Offertes & Aanvragen: Reactie binnen 24 uur na indiening
    """.trimIndent()

    private var workerSopGuidelines: String = """
        - Veiligheidsnorm: NEN 3140 en NEN 1010 naleving verplicht bij elke elektra installatie
        - LMRA (Laatste Minuut Risico Analyse): Uitvoeren vóór aanvang van elke opdracht
        - Werkbonnen: Aftekening door klant verplicht vóór afronding
        - Urenregistratie: Binnen 24 uur na uitvoering boeken in het systeem
        - Materiaalbeheer: Gebruikte componenten direct noteren in taaknotities
    """.trimIndent()

    /**
     * Genereert de strikte configuratie voor het opgegeven harnas.
     * De Admin AI treedt op als de exclusieve brug die de context en parameters filtert.
     */
    fun resolveHarnessConfig(
        category: AIHarnessCategory,
        currentUser: UserEntity?,
        activeTasks: List<PlanningTaskEntity> = emptyList(),
        activeRequests: List<ServiceRequestEntity> = emptyList(),
        allUsers: List<UserEntity> = emptyList()
    ): HarnessPromptConfiguration {
        return when (category) {
            AIHarnessCategory.KLANT -> {
                // Harnas 1: Klant AI
                val clientName = currentUser?.fullName ?: "Gewaardeerde Klant"
                val clientUsername = currentUser?.username ?: ""

                val myRequests = activeRequests.filter { it.clientUsername.equals(clientUsername, ignoreCase = true) }
                val requestSummaries = if (myRequests.isNotEmpty()) {
                    myRequests.joinToString("\n") { "- Aanvraag #${it.id}: '${it.title}' (Status: ${it.status}, Datum: ${it.preferredDate})" }
                } else {
                    "Geen actieve service-aanvragen."
                }

                val systemPrompt = """
                    Je bent de Klantenservice AI van RoleVault, toegewezen aan klant '$clientName'.
                    
                    STRIKTE VEILIGHEIDS- EN PRIVACYREGELS:
                    1. Je mag UITSLUITEND vragen beantwoorden over de diensten van het bedrijf, algemene productinformatie, en de status van de EIGEN aanvragen van deze specifieke klant.
                    2. Je mag ONDER GEEN BEDING interne gegevens over monteurs/werkers (zoals uurlonen, privégegevens, interne planningen) delen.
                    3. Je mag ONDER GEEN BEDING beheerdersinformatie, databasegegevens of systeeminfrastructuur tonen.
                    4. Blijf altijd beleefd, hulpvaardig, professioneel en bondig.
                    
                    GOEDGEKEURDE BEDRIJFSKENNIS:
                    $customerFaqKnowledge
                    
                    ACTUELE STATUS VAN UW AANVRAGEN:
                    $requestSummaries
                """.trimIndent()

                HarnessPromptConfiguration(
                    systemPrompt = systemPrompt,
                    tools = JSONArray(), // Geen tools voor klanten
                    allowedModel = "gpt-4o-mini",
                    temperature = 0.3,
                    maxTokens = 2048,
                    bridgeMetadata = "Geregeerd door Admin AI Brug - Sandbox Actief"
                )
            }

            AIHarnessCategory.WERKER -> {
                // Harnas 2: Werker AI Co-Pilot
                val workerName = currentUser?.fullName ?: "Monteur / Werker"
                val workerUsername = currentUser?.username ?: ""

                val myTasks = activeTasks.filter { it.assignedWorkerUsername.equals(workerUsername, ignoreCase = true) }
                val taskSummaries = if (myTasks.isNotEmpty()) {
                    myTasks.joinToString("\n") { task ->
                        "- Werkorder #${task.id}: '${task.title}' | Klant: ${task.clientName} | Locatie: ${task.location} | Status: ${task.status} | Datum: ${task.scheduledDate}"
                    }
                } else {
                    "Geen openstaande taken toegewezen."
                }

                val systemPrompt = """
                    Je bent de Werker AI Co-Pilot (Technische Rechterhand) voor monteur '$workerName'.
                    
                    DOEL & POLICY:
                    1. Je helpt de monteur met technische richtlijnen, stappenplannen, materiaaladvies en efficiënte werkuitvoering.
                    2. Je ondersteunt bij veiligheidsvoorschriften en processtappen volgens de bedrijfsstandaarden.
                    3. Je hebt inzicht in de opdrachten van deze specifieke werker en de relevante klantcontactgegevens voor de opdracht.
                    4. Je hebt GEEN toegang tot Admin audits, accountbeheer van collega's, of systeemconfiguraties.
                    
                    OPERATIONELE RICHTLIJNEN & VEILIGHEID:
                    $workerSopGuidelines
                    
                    ACTUELE WERKORDERS VAN DEZE MONTEUR:
                    $taskSummaries
                """.trimIndent()

                val workerTools = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "function")
                        put("name", "search_technical_specs")
                        put("description", "Zoek technische specificaties, NEN-normen of installatiehandleidingen op")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("query", JSONObject().put("type", "string").put("description", "Technische zoekterm"))
                            })
                            put("required", JSONArray().put("query"))
                        })
                    })
                }

                HarnessPromptConfiguration(
                    systemPrompt = systemPrompt,
                    tools = workerTools,
                    allowedModel = "gpt-4o",
                    temperature = 0.2,
                    maxTokens = 4096,
                    bridgeMetadata = "Geregeerd door Admin AI Brug - Werker Co-Pilot Actief"
                )
            }

            AIHarnessCategory.ADMIN -> {
                // Harnas 3: Admin AI Master Orchestrator & Brug
                val adminName = currentUser?.username ?: "Infrix-dev"

                val systemPrompt = """
                    Je bent de Admin Master AI van RoleVault voor hoofdbeheerder '$adminName'.
                    
                    JE ROL ALS CENTRALE BRUG & ORCHESTRATOR:
                    1. Je hebt de hoogste autoriteit binnen de applicatie en beschikt over alle diagnostische, ontwikkel- en beheerinstrumenten.
                    2. Jij bent de ENIGE brug die de configuratie, kennis en veiligheidspolicies beheert voor de 'Klant AI' en 'Werker AI'.
                    3. De menselijke beheerder hoeft niet elke AI afzonderlijk te configureren; jij vertaalt beheerinstructies automatisch naar geüpdatete policies voor de monteurs en klanten.
                    4. Je bewaakt de strikte scheiding van data tussen Klant, Werker en Systeem.
                    
                    SYSTEEM OVERZICHT:
                    - Totaal gebruikers in database: ${allUsers.size} (${allUsers.count { it.role == UserRole.WERKER }} werkers, ${allUsers.count { it.role == UserRole.KLANT }} klanten)
                    - Totaal lopende planningstaken: ${activeTasks.size}
                    - Totaal service-aanvragen: ${activeRequests.size}
                """.trimIndent()

                val adminTools = getFullAdminToolsArray()

                HarnessPromptConfiguration(
                    systemPrompt = systemPrompt,
                    tools = adminTools,
                    allowedModel = "gpt-4o",
                    temperature = 0.2,
                    maxTokens = 8192,
                    bridgeMetadata = "Admin Master Controller - Volledige Rechten & AI Brug Actief"
                )
            }
        }
    }

    /**
     * Admin AI kan de kennis en richtlijnen voor Klant en Werker dynamisch bijwerken.
     */
    fun updateCustomerKnowledgeByAdminAI(newKnowledge: String) {
        if (newKnowledge.isNotBlank()) {
            customerFaqKnowledge = newKnowledge.trim()
        }
    }

    fun updateWorkerSopByAdminAI(newSop: String) {
        if (newSop.isNotBlank()) {
            workerSopGuidelines = newSop.trim()
        }
    }

    private fun getFullAdminToolsArray(): JSONArray {
        val tools = JSONArray()

        fun addTool(name: String, description: String, props: Map<String, String>, required: List<String>) {
            val parameters = JSONObject().apply {
                put("type", "object")
                val propsObj = JSONObject()
                props.forEach { (k, v) ->
                    propsObj.put(k, JSONObject().apply {
                        put("type", "string")
                        put("description", v)
                    })
                }
                put("properties", propsObj)
                val reqArr = JSONArray()
                required.forEach { reqArr.put(it) }
                put("required", reqArr)
            }
            tools.put(JSONObject().apply {
                put("type", "function")
                put("name", name)
                put("description", description)
                put("parameters", parameters)
            })
        }

        addTool(
            name = "inspect_system_audit_logs",
            description = "Haal de recente beveiligings- en auditlogs op uit de database",
            props = mapOf("severity_filter" to "Filter op severity (INFO, WARNING, SECURITY_ALERT)"),
            required = emptyList()
        )

        addTool(
            name = "sync_all_calendar_events",
            description = "Forceer een volledige automatische synchronisatie van taken, werkers, klanten, Maps routes en Search data naar de backend kalender",
            props = emptyMap(),
            required = emptyList()
        )

        addTool(
            name = "update_client_ai_policy",
            description = "Werk de goedgekeurde kennis en antwoordrichtlijnen voor de Klant AI Assistent bij",
            props = mapOf("new_knowledge" to "Nieuwe goedgekeurde FAQ en bedrijfsinformatie"),
            required = listOf("new_knowledge")
        )

        addTool(
            name = "update_worker_ai_policy",
            description = "Werk de veiligheidsnormen en SOP werkinstructies voor de Werker AI Co-Pilot bij",
            props = mapOf("new_sop" to "Nieuwe werkinstructies en veiligheidsrichtlijnen"),
            required = listOf("new_sop")
        )

        addTool(
            name = "get_telemetry_metrics",
            description = "Bekijk actuele Android hardware- en geheugenstatistieken van het apparaat",
            props = emptyMap(),
            required = emptyList()
        )

        return tools
    }
}
