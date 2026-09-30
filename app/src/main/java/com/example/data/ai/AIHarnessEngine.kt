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
        description = "Volledige autoriteit, dev tools, skills en centrale brug die dynamische modellen, reasoning efforts en Klant/Werker AI configureert.",
        roleAllowed = UserRole.ADMIN
    )
}

data class HarnessPromptConfiguration(
    val systemPrompt: String,
    val tools: JSONArray,
    val allowedModel: String,
    val reasoningEffort: String? = null,
    val temperature: Double = 0.2,
    val maxTokens: Int = 8192,
    val bridgeMetadata: String = ""
)

class AIHarnessEngine {

    // Dynamisch geconfigureerde modellen per harnas (geen hardcoded static modellen)
    private var dynamicAdminModel: String = ""
    private var dynamicWorkerModel: String = ""
    private var dynamicClientModel: String = ""
    private var dynamicAdminReasoningEffort: String? = "medium"
    private var dynamicWorkerReasoningEffort: String? = "low"

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

    fun setDynamicModels(adminModel: String, workerModel: String, clientModel: String) {
        if (adminModel.isNotBlank()) dynamicAdminModel = adminModel
        if (workerModel.isNotBlank()) dynamicWorkerModel = workerModel
        if (clientModel.isNotBlank()) dynamicClientModel = clientModel
    }

    fun setReasoningEffort(adminReasoning: String?, workerReasoning: String?) {
        dynamicAdminReasoningEffort = adminReasoning
        dynamicWorkerReasoningEffort = workerReasoning
    }

    /**
     * Genereert de strikte configuratie voor het opgegeven harnas.
     * Modellen worden dynamisch geselecteerd uit de live provider endpoints.
     */
    fun resolveHarnessConfig(
        category: AIHarnessCategory,
        currentUser: UserEntity?,
        activeTasks: List<PlanningTaskEntity> = emptyList(),
        activeRequests: List<ServiceRequestEntity> = emptyList(),
        allUsers: List<UserEntity> = emptyList(),
        overrideModel: String? = null,
        overrideReasoningEffort: String? = null
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

                val resolvedModel = overrideModel?.ifBlank { null }
                    ?: dynamicClientModel.ifBlank { dynamicAdminModel }

                HarnessPromptConfiguration(
                    systemPrompt = systemPrompt,
                    tools = JSONArray(), // Geen tools voor klanten
                    allowedModel = resolvedModel,
                    reasoningEffort = null, // Klant gebruikt snelle non-reasoning of default
                    temperature = 0.3,
                    maxTokens = 2048,
                    bridgeMetadata = if (resolvedModel.isNotBlank()) "Geregeerd door Admin AI Brug - Live OpenAI Model: $resolvedModel" else "Geregeerd door Admin AI Brug - Live OpenAI Model"
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
                        put("description", "Zoek technische specificaties, NEN-normen of installatiehandleidingen op via dynamische tools")
                        put("parameters", JSONObject().apply {
                            put("type", "object")
                            put("properties", JSONObject().apply {
                                put("query", JSONObject().put("type", "string").put("description", "Technische zoekterm"))
                            })
                            put("required", JSONArray().put("query"))
                        })
                    })
                }

                val resolvedModel = overrideModel?.ifBlank { null }
                    ?: dynamicWorkerModel.ifBlank { dynamicAdminModel }

                val resolvedEffort = overrideReasoningEffort ?: dynamicWorkerReasoningEffort

                HarnessPromptConfiguration(
                    systemPrompt = systemPrompt,
                    tools = workerTools,
                    allowedModel = resolvedModel,
                    reasoningEffort = resolvedEffort,
                    temperature = 0.2,
                    maxTokens = 4096,
                    bridgeMetadata = if (resolvedModel.isNotBlank()) "Geregeerd door Admin AI Brug - Live OpenAI Model: $resolvedModel (Reasoning: ${resolvedEffort ?: "default"})" else "Geregeerd door Admin AI Brug - Live OpenAI Model (Reasoning: ${resolvedEffort ?: "default"})"
                )
            }

            AIHarnessCategory.ADMIN -> {
                // Harnas 3: Admin AI Master Orchestrator & Brug
                val adminName = currentUser?.username ?: "Infrix-dev"

                val systemPrompt = """
                    Je bent de Admin Master AI van RoleVault voor hoofdbeheerder '$adminName'.
                    
                    JE ROL ALS CENTRALE BRUG & ORCHESTRATOR:
                    1. Je hebt de hoogste autoriteit binnen de applicatie en beschikt over alle diagnostische, ontwikkel- en beheerinstrumenten.
                    2. Jij bent de ENIGE brug die de dynamische modellen, reasoning efforts, actieve endpoints en veiligheidspolicies beheert voor de 'Klant AI' en 'Werker AI'.
                    3. De menselijke beheerder hoeft niet elke AI afzonderlijk te configureren; jij regelt modelselecties, tool calls en richtlijnen direct.
                    4. Je bewaakt de strikte scheiding van data tussen Klant, Werker en Systeem.
                    
                    SYSTEEM OVERZICHT:
                    - Totaal gebruikers in database: ${allUsers.size} (${allUsers.count { it.role == UserRole.WERKER }} werkers, ${allUsers.count { it.role == UserRole.KLANT }} klanten)
                    - Totaal lopende planningstaken: ${activeTasks.size}
                    - Totaal service-aanvragen: ${activeRequests.size}
                """.trimIndent()

                val adminTools = getFullAdminToolsArray()

                val resolvedModel = overrideModel?.ifBlank { null }
                    ?: dynamicAdminModel

                val resolvedEffort = overrideReasoningEffort ?: dynamicAdminReasoningEffort

                HarnessPromptConfiguration(
                    systemPrompt = systemPrompt,
                    tools = adminTools,
                    allowedModel = resolvedModel,
                    reasoningEffort = resolvedEffort,
                    temperature = 0.2,
                    maxTokens = 8192,
                    bridgeMetadata = if (resolvedModel.isNotBlank()) "Admin Master Controller - Live OpenAI Model: $resolvedModel (Reasoning: ${resolvedEffort ?: "default"})" else "Admin Master Controller - Live OpenAI Model (Reasoning: ${resolvedEffort ?: "default"})"
                )
            }
        }
    }

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

    fun updateDynamicModelsByAdminAI(adminModel: String, workerModel: String, clientModel: String) {
        setDynamicModels(adminModel, workerModel, clientModel)
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
            name = "configure_dynamic_ai_models",
            description = "Configureer dynamische AI modellen en reasoning efforts voor Klant, Werker en Admin harnassen",
            props = mapOf(
                "admin_model" to "Dynamisch model ID voor Admin",
                "worker_model" to "Dynamisch model ID voor Werker",
                "client_model" to "Dynamisch model ID voor Klant",
                "reasoning_effort" to "Reasoning effort (low, medium, high)"
            ),
            required = listOf("admin_model")
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

        // HOSTED JAVA, BASH & CHROME DEVTOOLS SANDBOX TOOLS
        addTool(
            name = "sandbox_bash_exec",
            description = "Voer willekeurige Linux Bash shell commando's uit in de gehoste container (/workspace, OpenJDK 21, Linux, PTY shell)",
            props = mapOf(
                "command" to "Het bash commando om uit te voeren (bv. 'javac Main.java && java Main', 'ls -la', 'uname -a', 'curl -I https://...')"
            ),
            required = listOf("command")
        )

        addTool(
            name = "sandbox_java_run",
            description = "Compileer en voer complete Java broncode uit in de gehoste OpenJDK 21 JVM sandbox",
            props = mapOf(
                "class_name" to "Hoofdklasse naam van het Java bestand (bv. 'Main')",
                "source_code" to "De volledige Java broncode met public class en main methode"
            ),
            required = listOf("class_name", "source_code")
        )

        addTool(
            name = "sandbox_chrome_devtools",
            description = "Bedien headless Chromium via het Chrome DevTools Protocol (CDP): navigeren, screenshots, JavaScript evaluatie, DOM inspectie, console en network tracing",
            props = mapOf(
                "action" to "De CDP actie: 'navigate', 'screenshot', 'evaluate_js', 'inspect_dom'",
                "url" to "Doel-URL voor navigatie (optioneel)",
                "script" to "JavaScript expressie om uit te voeren in de browser (optioneel)",
                "selector" to "CSS selector voor DOM inspectie (optioneel)"
            ),
            required = listOf("action")
        )

        addTool(
            name = "sandbox_fs_write",
            description = "Schrijf of bewerk een bestand in het gehoste container bestandssysteem (/workspace)",
            props = mapOf(
                "path" to "Bestandspad relatief aan /workspace (bv. 'Main.java', 'script.sh', 'data.json')",
                "content" to "Tekstinhoud van het bestand"
            ),
            required = listOf("path", "content")
        )

        addTool(
            name = "sandbox_fs_read",
            description = "Lees de inhoud van een bestand uit het gehoste container bestandssysteem (/workspace)",
            props = mapOf("path" to "Bestandspad relatief aan /workspace"),
            required = listOf("path")
        )

        addTool(
            name = "sandbox_get_capabilities",
            description = "Inspecteer de specificaties en mogelijkheden van de gehoste Linux sandbox: OpenJDK 21 versie, Chrome DevTools Protocol, kernel, RAM en CPU limieten",
            props = emptyMap(),
            required = emptyList()
        )

        return tools
    }
}
