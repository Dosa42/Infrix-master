package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AIHarnessCategory
import com.example.data.ai.AIHarnessEngine
import com.example.data.auth.ChatGPTAuthManager
import com.example.data.model.PlanningTaskEntity
import com.example.data.model.ServiceRequestEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var authManager: ChatGPTAuthManager
    private lateinit var harnessEngine: AIHarnessEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authManager = ChatGPTAuthManager(context, CoroutineScope(Dispatchers.Unconfined))
        harnessEngine = AIHarnessEngine()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("RoleVault", appName)
    }

    @Test
    fun `manual token injection with valid JSON session succeeds`() {
        val sampleJson = """
            {
                "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTYiLCJlbWFpbCI6ImFkbWluQGZpcm1hLm5sIiwiZXhwIjoyMDgwMDAwMDAwfQ.signature",
                "refreshToken": "long_lived_refresh_secret",
                "accountId": "acc_role_vault_admin",
                "email": "admin@firma.nl",
                "expiresAt": 2080000000000
            }
        """.trimIndent()

        val result = authManager.injectManualToken(sampleJson)
        assertTrue("Injectie moet slagen voor geldige JSON", result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("admin@firma.nl", session?.email)
        assertEquals("acc_role_vault_admin", session?.accountId)
        assertTrue("Sessie moet geldig zijn", session?.isValid == true)
    }

    @Test
    fun `manual token injection with raw Bearer token string succeeds`() {
        val rawToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiZW1haWwiOiJ1c2VyQGNoYXRncHQubG9jYWwiLCJleHAiOjIwODAwMDAwMDB9.signature"
        val result = authManager.injectManualToken(rawToken)
        assertTrue("Injectie moet slagen voor Bearer string", result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertTrue("Sessie moet geldig zijn", session?.isValid == true)
        assertTrue("Bearer prefix moet gestript zijn van accessToken", session?.accessToken?.startsWith("Bearer ") == false)
    }

    @Test
    fun `manual token injection with empty string fails with no fallback`() {
        val result = authManager.injectManualToken("   ")
        assertTrue("Lege invoer moet falen", result.isFailure)
    }

    @Test
    fun `manual token injection with expired token fails with no fallback`() {
        val expiredJson = """
            {
                "accessToken": "sample_expired_token",
                "expiresAt": 1000000
            }
        """.trimIndent()

        val result = authManager.injectManualToken(expiredJson)
        assertTrue("Verlopen token moet falen", result.isFailure)
    }

    @Test
    fun `dynamic models and reasoning effort configured in harness engine`() {
        val dynamicAdmin = "o3-mini"
        val dynamicWorker = "gpt-4.5"
        val dynamicClient = "o4-mini"

        harnessEngine.setDynamicModels(
            adminModel = dynamicAdmin,
            workerModel = dynamicWorker,
            clientModel = dynamicClient
        )
        harnessEngine.setReasoningEffort(adminReasoning = "high", workerReasoning = "medium")

        val adminUser = UserEntity(username = "admin1", fullName = "Admin", email = "admin@firma.nl", role = UserRole.ADMIN, passwordHash = "", salt = "")
        val workerUser = UserEntity(username = "worker1", fullName = "Worker", email = "worker@firma.nl", role = UserRole.WERKER, passwordHash = "", salt = "")
        val clientUser = UserEntity(username = "client1", fullName = "Client", email = "client@firma.nl", role = UserRole.KLANT, passwordHash = "", salt = "")

        val adminConfig = harnessEngine.resolveHarnessConfig(AIHarnessCategory.ADMIN, adminUser)
        assertEquals(dynamicAdmin, adminConfig.allowedModel)
        assertEquals("high", adminConfig.reasoningEffort)

        val workerConfig = harnessEngine.resolveHarnessConfig(AIHarnessCategory.WERKER, workerUser)
        assertEquals(dynamicWorker, workerConfig.allowedModel)
        assertEquals("medium", workerConfig.reasoningEffort)

        val clientConfig = harnessEngine.resolveHarnessConfig(AIHarnessCategory.KLANT, clientUser)
        assertEquals(dynamicClient, clientConfig.allowedModel)
    }

    @Test
    fun `klant harness enforces strict customer FAQ and isolation with dynamic model`() {
        harnessEngine.setDynamicModels(adminModel = "dynamic-admin", workerModel = "dynamic-worker", clientModel = "dynamic-client")

        val clientUser = UserEntity(
            username = "klant1",
            fullName = "Karel Klant",
            email = "karel@klant.nl",
            role = UserRole.KLANT,
            passwordHash = "",
            salt = ""
        )

        val requests = listOf(
            ServiceRequestEntity(
                id = 101,
                clientUsername = "klant1",
                clientName = "Karel Klant",
                title = "Onderhoud CV-ketel",
                description = "Jaarlijkse inspectie",
                preferredDate = "2026-10-05"
            ),
            ServiceRequestEntity(
                id = 102,
                clientUsername = "andere_klant",
                clientName = "Andere Klant",
                title = "Privé aanvraag",
                description = "Geheime info",
                preferredDate = "2026-10-06"
            )
        )

        val config = harnessEngine.resolveHarnessConfig(
            category = AIHarnessCategory.KLANT,
            currentUser = clientUser,
            activeTasks = emptyList(),
            activeRequests = requests,
            allUsers = emptyList()
        )

        assertEquals("dynamic-client", config.allowedModel)
        assertEquals(0, config.tools.length()) // Klanten hebben geen tools
        assertTrue("Prompt moet eigen aanvraag bevatten", config.systemPrompt.contains("Aanvraag #101"))
        assertFalse("Prompt mag GEEN aanvraag van andere klanten bevatten", config.systemPrompt.contains("Privé aanvraag"))
        assertTrue("Prompt moet goedgekeurde FAQ bevatten", config.systemPrompt.contains("RoleVault Diensten & Techniek"))
    }

    @Test
    fun `werker harness enforces deterministic policy and assigned tasks with dynamic tools`() {
        harnessEngine.setDynamicModels(adminModel = "dynamic-admin", workerModel = "dynamic-worker-o3", clientModel = "dynamic-client")
        harnessEngine.setReasoningEffort(adminReasoning = "medium", workerReasoning = "low")

        val workerUser = UserEntity(
            username = "monteur_jan",
            fullName = "Jan Monteur",
            email = "jan@monteur.nl",
            role = UserRole.WERKER,
            passwordHash = "",
            salt = ""
        )

        val tasks = listOf(
            PlanningTaskEntity(
                id = 1,
                title = "Vervangen Hoofdschakelaar",
                description = "Meterkast revisie",
                assignedWorkerUsername = "monteur_jan",
                assignedWorkerName = "Jan Monteur",
                clientUsername = "klant_abc",
                clientName = "Bedrijf ABC",
                status = "Gepland",
                priority = "Hoog",
                scheduledDate = "2026-10-01",
                estimatedHours = 3.5,
                location = "Amsterdam"
            ),
            PlanningTaskEntity(
                id = 2,
                title = "Geheime Taak Andere Monteur",
                description = "Andere opdracht",
                assignedWorkerUsername = "monteur_piet",
                assignedWorkerName = "Piet Monteur",
                clientUsername = "klant_xyz",
                clientName = "Bedrijf XYZ",
                status = "Gepland",
                priority = "Normaal",
                scheduledDate = "2026-10-02",
                estimatedHours = 2.0,
                location = "Rotterdam"
            )
        )

        val config = harnessEngine.resolveHarnessConfig(
            category = AIHarnessCategory.WERKER,
            currentUser = workerUser,
            activeTasks = tasks,
            activeRequests = emptyList(),
            allUsers = emptyList()
        )

        assertEquals("dynamic-worker-o3", config.allowedModel)
        assertEquals("low", config.reasoningEffort)
        assertTrue("Werker moet technische tools hebben", config.tools.length() > 0)
        assertTrue("Prompt moet toegewezen taak bevatten", config.systemPrompt.contains("Vervangen Hoofdschakelaar"))
        assertFalse("Prompt mag GEEN taken van andere monteurs bevatten", config.systemPrompt.contains("Geheime Taak Andere Monteur"))
        assertTrue("Prompt moet NEN-veiligheidsnormen bevatten", config.systemPrompt.contains("NEN 3140"))
    }

    @Test
    fun `admin master harness has full authority, dynamic models and dev tools`() {
        harnessEngine.setDynamicModels(adminModel = "dynamic-flagship-o3", workerModel = "dynamic-worker", clientModel = "dynamic-client")
        harnessEngine.setReasoningEffort(adminReasoning = "high", workerReasoning = "low")

        val adminUser = UserEntity(
            username = "Infrix-dev",
            fullName = "Infrix Hoofdbeheerder",
            email = "admin@rolevault.local",
            role = UserRole.ADMIN,
            passwordHash = "",
            salt = ""
        )

        val config = harnessEngine.resolveHarnessConfig(
            category = AIHarnessCategory.ADMIN,
            currentUser = adminUser,
            activeTasks = emptyList(),
            activeRequests = emptyList(),
            allUsers = emptyList()
        )

        assertEquals("dynamic-flagship-o3", config.allowedModel)
        assertEquals("high", config.reasoningEffort)
        assertTrue("Admin moet alle tools hebben (audit logs, calendar sync, dynamic model config, policy bridge, telemetrie)", config.tools.length() >= 6)
        assertTrue("System prompt moet master orchestrator rol benoemen", config.systemPrompt.contains("Master AI"))
    }

    @Test
    fun `admin ai dynamic policy updates propagate to client and worker harnesses`() {
        // Admin updates customer FAQ
        val updatedFaq = "Nieuwe bedrijfsregel 2026: Alle spoedaanvragen binnen 30 minuten ter plaatse."
        harnessEngine.updateCustomerKnowledgeByAdminAI(updatedFaq)

        val clientUser = UserEntity(username = "k1", fullName = "K1", email = "k1@klant.nl", role = UserRole.KLANT, passwordHash = "", salt = "")
        val clientConfig = harnessEngine.resolveHarnessConfig(AIHarnessCategory.KLANT, clientUser)
        assertTrue("Klant prompt moet de nieuw geüpdatete bedrijfsregel bevatten", clientConfig.systemPrompt.contains(updatedFaq))

        // Admin updates worker SOP
        val updatedSop = "Nieuwe veiligheidsinstructie: VDE-geïsoleerd gereedschap 1000V verplicht keuren voor elke klus."
        harnessEngine.updateWorkerSopByAdminAI(updatedSop)

        val workerUser = UserEntity(username = "w1", fullName = "W1", email = "w1@monteur.nl", role = UserRole.WERKER, passwordHash = "", salt = "")
        val workerConfig = harnessEngine.resolveHarnessConfig(AIHarnessCategory.WERKER, workerUser)
        assertTrue("Werker prompt moet de nieuwe veiligheidsinstructie bevatten", workerConfig.systemPrompt.contains(updatedSop))
    }

    @Test
    fun `harness returns empty string without dummy fallback model`() {
        val cleanHarness = AIHarnessEngine()
        val adminUser = UserEntity(username = "admin", fullName = "Admin", email = "a@a.nl", role = UserRole.ADMIN, passwordHash = "", salt = "")
        val config = cleanHarness.resolveHarnessConfig(AIHarnessCategory.ADMIN, adminUser)
        assertEquals("", config.allowedModel)
        assertFalse("Mag geen dummy fallback bevatten", config.allowedModel.contains("dynamic-live-model"))
    }
}
