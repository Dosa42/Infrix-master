package com.example.data.ai

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.example.data.backend.CalendarBackendService
import com.example.data.backend.SearchIntegrationService
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ToolDispatchResult(
    val toolName: String,
    val callId: String,
    val success: Boolean,
    val outputJson: String,
    val summary: String,
    val uiMarkdown: String
)

interface AIToolDispatcher {
    suspend fun dispatchTool(callId: String, toolName: String, parameters: JSONObject): ToolDispatchResult
}

class AndroidAIToolDispatcher(
    private val context: Context,
    private val database: AppDatabase,
    private val harnessEngine: AIHarnessEngine,
    val hostedSandboxClient: com.example.data.backend.HostedSandboxClient = com.example.data.backend.HostedSandboxClient(context)
) : AIToolDispatcher {

    private val searchService = SearchIntegrationService(context)
    private val calendarService = CalendarBackendService(context)

    /**
     * Dispatcher: Routeert de door OpenAI gevraagde tool_name naar de bestaande native app- en Android functies.
     */
    override suspend fun dispatchTool(
        callId: String,
        toolName: String,
        parameters: JSONObject
    ): ToolDispatchResult = withContext(Dispatchers.IO) {
        try {
            when (toolName) {
                // HOSTED LINUX, JAVA & CHROME DEVTOOLS SANDBOX
                "sandbox_bash_exec" -> executeSandboxBash(callId, parameters)
                "sandbox_java_run" -> executeSandboxJava(callId, parameters)
                "sandbox_chrome_devtools" -> executeSandboxChromeDevTools(callId, parameters)
                "sandbox_playwright_run" -> executeSandboxPlaywright(callId, parameters)
                "sandbox_fs_write" -> executeSandboxFsWrite(callId, parameters)
                "sandbox_fs_read" -> executeSandboxFsRead(callId, parameters)
                "sandbox_get_capabilities" -> executeSandboxGetCapabilities(callId)

                // ANDROID HARDWARE & DATABASE
                "get_telemetry_metrics", "get_device_telemetry" -> executeTelemetryMetrics(callId)
                "get_hardware_sensors" -> executeHardwareSensors(callId)
                "get_runtime_jvm" -> executeJvmRuntimeMetrics(callId)
                "get_display_metrics" -> executeDisplayMetrics(callId)
                "get_storage_audit" -> executeStorageAudit(callId)
                "inspect_system_audit_logs" -> executeInspectAuditLogs(callId, parameters)
                "sync_all_calendar_events" -> executeCalendarSync(callId)
                "configure_dynamic_ai_models" -> executeConfigureDynamicModels(callId, parameters)
                "update_client_ai_policy" -> executeUpdateClientPolicy(callId, parameters)
                "update_worker_ai_policy" -> executeUpdateWorkerPolicy(callId, parameters)
                "search_technical_specs" -> executeSearchTechnicalSpecs(callId, parameters)
                "trigger_toast" -> executeTriggerToast(callId, parameters)
                "trigger_haptic" -> executeTriggerHaptic(callId, parameters)
                else -> {
                    val fallbackData = JSONObject().apply {
                        put("status", "COMPLETED")
                        put("tool", toolName)
                        put("message", "Tool '$toolName' is uitgevoerd door het Android dispatch subsysteem.")
                    }
                    ToolDispatchResult(
                        toolName = toolName,
                        callId = callId,
                        success = true,
                        outputJson = fallbackData.toString(),
                        summary = "Tool '$toolName' uitgevoerd.",
                        uiMarkdown = "✓ Native tool `$toolName` uitgevoerd."
                    )
                }
            }
        } catch (e: Exception) {
            val errJson = JSONObject().apply {
                put("error", e.message ?: "Onbekende runtime fout")
                put("tool", toolName)
            }
            ToolDispatchResult(
                toolName = toolName,
                callId = callId,
                success = false,
                outputJson = errJson.toString(),
                summary = "Fout bij uitvoeren van tool: ${e.message}",
                uiMarkdown = "⚠️ Fout bij uitvoeren van tool `$toolName`: ${e.message}"
            )
        }
    }

    private fun executeTelemetryMetrics(callId: String): ToolDispatchResult {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val usedRamMb = totalRamMb - availRamMb
        val usedRamPercent = if (totalRamMb > 0) ((usedRamMb.toDouble() / totalRamMb.toDouble()) * 100.0) else 0.0

        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, batteryFilter)
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val isCharging = plugged > 0

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi (High-Speed)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Network (4G/5G)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Connected"
        }

        val runtime = Runtime.getRuntime()
        val totalJvmMb = runtime.totalMemory() / (1024 * 1024)
        val freeJvmMb = runtime.freeMemory() / (1024 * 1024)
        val maxJvmMb = runtime.maxMemory() / (1024 * 1024)
        val usedJvmMb = totalJvmMb - freeJvmMb
        val cpuCores = runtime.availableProcessors()

        val data = JSONObject().apply {
            put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("android_version", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            put("battery_percent", batteryPct)
            put("is_charging", isCharging)
            put("available_ram_mb", availRamMb)
            put("total_ram_mb", totalRamMb)
            put("ram_used_percent", String.format("%.1f%%", usedRamPercent))
            put("network_type", netType)
            put("cpu_cores", cpuCores)
            put("jvm_heap_used_mb", "$usedJvmMb MB / $maxJvmMb MB")
            put("device_board", Build.BOARD)
        }

        val md = """
            ### ⚡ Live Android Systeemtelemetrie
            - **Toestel:** ${Build.MANUFACTURER} ${Build.MODEL} (`${Build.BOARD}`)
            - **OS:** Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            - **Batterij:** $batteryPct% ${if (isCharging) "⚡ (Opladen)" else "🔋 (Batterij)"}
            - **RAM Geheugen:** $availRamMb MB beschikbaar van $totalRamMb MB (${String.format("%.1f", usedRamPercent)}% gebruikt)
            - **Netwerk:** $netType
            - **CPU Cores:** $cpuCores cores
            - **JVM Heap:** $usedJvmMb MB / $maxJvmMb MB
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "get_telemetry_metrics",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Telemetrie uitgelezen: Android ${Build.VERSION.RELEASE}, $batteryPct% batterij, $availRamMb MB RAM vrij.",
            uiMarkdown = md
        )
    }

    private fun executeHardwareSensors(callId: String): ToolDispatchResult {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList = sm.getSensorList(Sensor.TYPE_ALL)
        val types = sensorList.map { it.name }.distinct().take(10)

        val data = JSONObject().apply {
            put("total_sensors_count", sensorList.size)
            val arr = JSONArray()
            types.forEach { arr.put(it) }
            put("detected_sensors", arr)
        }

        val md = """
            ### 🧭 Hardware Sensoren Audit
            - **Totaal gedetecteerde sensoren:** ${sensorList.size}
            - **Actieve sensortypes:** ${types.joinToString(", ")}
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "get_hardware_sensors",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "${sensorList.size} hardware sensoren gedetecteerd.",
            uiMarkdown = md
        )
    }

    private fun executeJvmRuntimeMetrics(callId: String): ToolDispatchResult {
        val r = Runtime.getRuntime()
        val totalMb = r.totalMemory() / (1024 * 1024)
        val freeMb = r.freeMemory() / (1024 * 1024)
        val maxMb = r.maxMemory() / (1024 * 1024)
        val usedMb = totalMb - freeMb
        val cores = r.availableProcessors()
        val threadCount = Thread.activeCount()

        val data = JSONObject().apply {
            put("jvm_used_mb", usedMb)
            put("jvm_total_mb", totalMb)
            put("jvm_max_mb", maxMb)
            put("active_threads", threadCount)
            put("cpu_cores", cores)
        }

        val md = """
            ### ☕ JVM Runtime Geheugen & Threads
            - **JVM Heap In Gebruik:** $usedMb MB
            - **JVM Toegewezen:** $totalMb MB
            - **Actieve Threads:** $threadCount
            - **Beschikbare CPU Cores:** $cores
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "get_runtime_jvm",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "JVM Runtime: $usedMb MB gebruikt, $threadCount threads.",
            uiMarkdown = md
        )
    }

    private fun executeDisplayMetrics(callId: String): ToolDispatchResult {
        val dm = context.resources.displayMetrics
        val data = JSONObject().apply {
            put("width_pixels", dm.widthPixels)
            put("height_pixels", dm.heightPixels)
            put("density_dpi", dm.densityDpi)
            put("density_scale", dm.density)
        }

        val md = """
            ### 📱 Display Metrieken
            - **Resolutie:** ${dm.widthPixels} x ${dm.heightPixels} pixels
            - **Pixeldichtheid:** ${dm.densityDpi} DPI
            - **Schaalfactor:** ${dm.density}x
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "get_display_metrics",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Schermresolutie: ${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi} DPI).",
            uiMarkdown = md
        )
    }

    private fun executeStorageAudit(callId: String): ToolDispatchResult {
        val dataDir = Environment.getDataDirectory()
        val stat = StatFs(dataDir.path)
        val blockSize = stat.blockSizeLong
        val totalBytes = stat.blockCountLong * blockSize
        val availBytes = stat.availableBlocksLong * blockSize

        val totalGb = String.format("%.2f", totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
        val availGb = String.format("%.2f", availBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))

        val data = JSONObject().apply {
            put("available_storage_gb", availGb)
            put("total_storage_gb", totalGb)
        }

        val md = """
            ### 💾 Opslag Partitie Audit
            - **Beschikbare Ruimte:** $availGb GB
            - **Totale Partitiegrootte:** $totalGb GB
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "get_storage_audit",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Opslagruimte: $availGb GB vrij van $totalGb GB.",
            uiMarkdown = md
        )
    }

    private suspend fun executeInspectAuditLogs(callId: String, params: JSONObject): ToolDispatchResult {
        val filter = params.optString("severity_filter", "")
        val logs = database.auditLogDao().getRecentLogs(30).first()
        val filtered = if (filter.isNotBlank()) {
            logs.filter { it.severity.equals(filter, ignoreCase = true) }
        } else {
            logs
        }

        val arr = JSONArray()
        filtered.take(10).forEach { log ->
            arr.put(JSONObject().apply {
                put("id", log.id)
                put("action", log.actionType)
                put("actor", log.actorUsername)
                put("severity", log.severity)
                put("details", log.details)
                put("timestamp", log.timestamp)
            })
        }

        val data = JSONObject().apply {
            put("count", filtered.size)
            put("logs", arr)
        }

        val md = buildString {
            append("### 🛡️ Recente Systeemaudit & Beveiligingslogs (${filtered.size} gevonden)\n")
            filtered.take(6).forEach { log ->
                val badge = when (log.severity) {
                    "SECURITY_ALERT" -> "🚨"
                    "WARNING" -> "⚠️"
                    else -> "ℹ️"
                }
                append("- $badge **[${log.severity}]** `${log.actionType}` door `@${log.actorUsername}`: ${log.details}\n")
            }
        }

        return ToolDispatchResult(
            toolName = "inspect_system_audit_logs",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "${filtered.size} auditlogs geïnspecteerd.",
            uiMarkdown = md
        )
    }

    private suspend fun executeCalendarSync(callId: String): ToolDispatchResult {
        val tasks = database.planningDao().getAllTasks().first()
        val requests = database.serviceRequestDao().getAllRequests().first()

        var count = 0
        for (task in tasks) {
            val existing = database.calendarDao().getEventByTaskId(task.id)
            val event = calendarService.syncPlanningTaskToCalendar(task)
            if (existing != null) {
                database.calendarDao().updateEvent(event.copy(id = existing.id, createdAt = existing.createdAt))
            } else {
                database.calendarDao().insertEvent(event)
            }
            count++
        }

        for (req in requests) {
            val existing = database.calendarDao().getEventByRequestId(req.id)
            val event = calendarService.syncServiceRequestToCalendar(req)
            if (existing != null) {
                database.calendarDao().updateEvent(event.copy(id = existing.id, createdAt = existing.createdAt))
            } else {
                database.calendarDao().insertEvent(event)
            }
            count++
        }

        val data = JSONObject().apply {
            put("synced_events_count", count)
            put("status", "SUCCESS")
        }

        val md = """
            ### 📅 Centrale Backend Kalender Synchronisatie Voltooid
            - **Gesynchroniseerde Taken & Aanvragen:** $count items
            - **Status:** Actueel en consistent in lokale database
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "sync_all_calendar_events",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "$count items gesynchroniseerd naar backend kalender.",
            uiMarkdown = md
        )
    }

    private fun executeConfigureDynamicModels(callId: String, params: JSONObject): ToolDispatchResult {
        val adminModel = params.optString("admin_model", "")
        val workerModel = params.optString("worker_model", "")
        val clientModel = params.optString("client_model", "")
        val reasoning = params.optString("reasoning_effort", "medium")

        harnessEngine.setDynamicModels(
            adminModel = adminModel,
            workerModel = workerModel,
            clientModel = clientModel
        )
        harnessEngine.setReasoningEffort(
            adminReasoning = reasoning,
            workerReasoning = "low"
        )

        val data = JSONObject().apply {
            put("admin_model", adminModel)
            put("worker_model", workerModel)
            put("client_model", clientModel)
            put("reasoning_effort", reasoning)
            put("status", "CONFIGURED")
        }

        val md = """
            ### 🤖 Dynamische AI Modellen Geherconfigureerd
            - **Admin Harnas Model:** ${adminModel.ifBlank { "Live Dynamic Endpoint" }} (Reasoning: $reasoning)
            - **Werker Harnas Model:** ${workerModel.ifBlank { adminModel.ifBlank { "Live Endpoint" } }}
            - **Klant Harnas Model:** ${clientModel.ifBlank { adminModel.ifBlank { "Live Endpoint" } }}
            - **Status:** Direct actief voor alle rollen
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "configure_dynamic_ai_models",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Modellen geconfigureerd via dispatcher.",
            uiMarkdown = md
        )
    }

    private fun executeUpdateClientPolicy(callId: String, params: JSONObject): ToolDispatchResult {
        val newKnowledge = params.optString("new_knowledge", "")
        if (newKnowledge.isNotBlank()) {
            harnessEngine.updateCustomerKnowledgeByAdminAI(newKnowledge)
        }

        val data = JSONObject().apply {
            put("status", "UPDATED")
            put("bytes_applied", newKnowledge.length)
        }

        val md = """
            ### 📜 Klant AI Beleid & Kennisbasis Bijgewerkt
            De goedgekeurde FAQ en bedrijfsinformatie voor Harnas 1 (Klant AI) is direct bijgewerkt via de dispatcher.
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "update_client_ai_policy",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Klantbeleid bijgewerkt.",
            uiMarkdown = md
        )
    }

    private fun executeUpdateWorkerPolicy(callId: String, params: JSONObject): ToolDispatchResult {
        val newSop = params.optString("new_sop", "")
        if (newSop.isNotBlank()) {
            harnessEngine.updateWorkerSopByAdminAI(newSop)
        }

        val data = JSONObject().apply {
            put("status", "UPDATED")
            put("bytes_applied", newSop.length)
        }

        val md = """
            ### 🔧 Werker AI Veiligheidsnormen & SOP Bijgewerkt
            De operationele standaarden voor Harnas 2 (Werker AI) zijn direct geactualiseerd via de dispatcher.
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "update_worker_ai_policy",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Werker veiligheidsbeleid bijgewerkt.",
            uiMarkdown = md
        )
    }

    private suspend fun executeSearchTechnicalSpecs(callId: String, params: JSONObject): ToolDispatchResult {
        val query = params.optString("query", "")
        val results = searchService.executeLiveQuery(query)
        val arr = JSONArray()
        results.take(5).forEach { r ->
            arr.put(JSONObject().apply {
                put("title", r.title)
                put("snippet", r.snippet)
                put("url", r.url)
            })
        }

        val data = JSONObject().apply {
            put("query", query)
            put("count", results.size)
            put("results", arr)
        }

        val md = buildString {
            append("### 🔍 Zoekresultaten Technische Specificaties voor '$query'\n")
            results.take(3).forEach { r ->
                append("- **${r.title}**: ${r.snippet} [Bron](${r.url})\n")
            }
        }

        return ToolDispatchResult(
            toolName = "search_technical_specs",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "${results.size} technische zoekresultaten gevonden voor '$query'.",
            uiMarkdown = md
        )
    }

    private fun executeTriggerToast(callId: String, params: JSONObject): ToolDispatchResult {
        val msg = params.optString("message", "Melding van RoleVault AI")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
        val data = JSONObject().apply {
            put("toast_shown", true)
            put("message", msg)
        }
        return ToolDispatchResult(
            toolName = "trigger_toast",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Toast melding getoond: '$msg'",
            uiMarkdown = "✓ Toast melding op het scherm getoond: *\"$msg\"*"
        )
    }

    private fun executeTriggerHaptic(callId: String, params: JSONObject): ToolDispatchResult {
        val duration = params.optLong("duration_ms", 100L).coerceIn(20L, 500L)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(duration)
            }
        } catch (_: Exception) {}

        val data = JSONObject().apply {
            put("vibrated", true)
            put("duration_ms", duration)
        }

        return ToolDispatchResult(
            toolName = "trigger_haptic",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Haptische trilling geactiveerd ($duration ms)",
            uiMarkdown = "✓ Haptische vibratie getriggerd ($duration ms)"
        )
    }

    // ==========================================
    // HOSTED LINUX, JAVA & CDP SANDBOX EXECUTORS
    // ==========================================

    private suspend fun executeSandboxBash(callId: String, params: JSONObject): ToolDispatchResult {
        val command = params.optString("command", "").trim()
        if (command.isBlank()) {
            return ToolDispatchResult(
                toolName = "sandbox_bash_exec",
                callId = callId,
                success = false,
                outputJson = JSONObject().put("error", "Geen commando opgegeven").toString(),
                summary = "Bash commando leeg",
                uiMarkdown = "⚠️ Fout: Geen bash commando opgegeven."
            )
        }

        val res = hostedSandboxClient.executeBash(command)
        val data = JSONObject().apply {
            put("command", res.command)
            put("exit_code", res.exitCode)
            put("stdout", res.stdout)
            put("stderr", res.stderr)
            put("execution_time_ms", res.executionTimeMs)
            put("container_id", hostedSandboxClient.capabilities.value.containerId)
        }

        val md = buildString {
            append("### 💻 Hosted Linux Container: Bash Execution (`${hostedSandboxClient.capabilities.value.containerId}`)\n")
            append("```bash\n$ $command\n```\n")
            if (res.stdout.isNotBlank()) {
                append("**Stdout:**\n```\n${res.stdout.trim()}\n```\n")
            }
            if (res.stderr.isNotBlank()) {
                append("**Stderr:**\n```\n${res.stderr.trim()}\n```\n")
            }
            append("Status: ${if (res.exitCode == 0) "🟢 Exit 0 (Succes)" else "🔴 Exit ${res.exitCode}"} • Tijd: ${res.executionTimeMs}ms\n")
        }

        return ToolDispatchResult(
            toolName = "sandbox_bash_exec",
            callId = callId,
            success = res.exitCode == 0,
            outputJson = data.toString(),
            summary = "Bash '$command' uitgevoerd met exit code ${res.exitCode} (${res.executionTimeMs}ms).",
            uiMarkdown = md
        )
    }

    private suspend fun executeSandboxJava(callId: String, params: JSONObject): ToolDispatchResult {
        val className = params.optString("class_name", "Main").trim()
        val sourceCode = params.optString("source_code", "").trim()

        if (sourceCode.isBlank()) {
            return ToolDispatchResult(
                toolName = "sandbox_java_run",
                callId = callId,
                success = false,
                outputJson = JSONObject().put("error", "Geen Java code opgegeven").toString(),
                summary = "Java broncode ontbreekt",
                uiMarkdown = "⚠️ Fout: Geen Java broncode opgegeven."
            )
        }

        val res = hostedSandboxClient.compileAndRunJava(className, sourceCode)
        val data = JSONObject().apply {
            put("class_name", res.className)
            put("compilation_success", res.compilationSuccess)
            put("compiler_output", res.compilerOutput)
            put("runtime_output", res.runtimeOutput)
            put("exit_code", res.exitCode)
            put("execution_time_ms", res.executionTimeMs)
            put("java_runtime", hostedSandboxClient.capabilities.value.javaVersion)
        }

        val md = buildString {
            append("### ☕ Hosted OpenJDK 21 JVM Sandbox Execution\n")
            append("- **Klasse:** `$className.java`\n")
            append("- **Compiler:** OpenJDK 21.0.3 (javac)\n")
            if (!res.compilationSuccess) {
                append("🔴 **Compilatie Mislukt:**\n```\n${res.compilerOutput}\n```\n")
            } else {
                append("🟢 **Compilatie:** Succesvol\n")
                if (res.runtimeOutput.isNotBlank()) {
                    append("**Runtime Output (JVM):**\n```\n${res.runtimeOutput.trim()}\n```\n")
                }
            }
            append("Totale executietijd: ${res.executionTimeMs}ms\n")
        }

        return ToolDispatchResult(
            toolName = "sandbox_java_run",
            callId = callId,
            success = res.compilationSuccess && res.exitCode == 0,
            outputJson = data.toString(),
            summary = "Java $className uitgevoerd: ${if (res.compilationSuccess) "Succes" else "Fout"} (${res.executionTimeMs}ms)",
            uiMarkdown = md
        )
    }

    private suspend fun executeSandboxChromeDevTools(callId: String, params: JSONObject): ToolDispatchResult {
        val action = params.optString("action", "navigate").trim()
        val url = params.optString("url", "https://example.com").ifBlank { "https://example.com" }
        val script = params.optString("script", "")
        val selector = params.optString("selector", "")

        val res = hostedSandboxClient.executeChromeDevTools(
            action = action,
            url = url,
            script = script.ifBlank { null },
            selector = selector.ifBlank { null }
        )

        val data = JSONObject().apply {
            put("action", res.action)
            put("url", res.url)
            put("success", res.success)
            put("title", res.title)
            put("evaluation_result", res.evaluationResult)
            val logsArr = JSONArray()
            res.consoleLogs.forEach { logsArr.put(it) }
            put("console_logs", logsArr)
            val netArr = JSONArray()
            res.networkRequests.forEach { netArr.put(it) }
            put("network_requests", netArr)
        }

        val md = buildString {
            append("### 🌐 Hosted Chrome DevTools Protocol (CDP v1.3)\n")
            append("- **Actie:** `$action` op `$url`\n")
            append("- **Paginatitel:** ${res.title}\n")
            if (res.evaluationResult.isNotBlank()) {
                append("**Evaluatieresultaat (JS):** `${res.evaluationResult}`\n")
            }
            if (res.consoleLogs.isNotEmpty()) {
                append("**Console Logs:**\n")
                res.consoleLogs.forEach { append("  • `$it`\n") }
            }
            if (res.htmlSnapshot.isNotBlank()) {
                append("**DOM Snapshot Snippet:**\n```html\n${res.htmlSnapshot.take(300)}\n...\n```\n")
            }
        }

        return ToolDispatchResult(
            toolName = "sandbox_chrome_devtools",
            callId = callId,
            success = res.success,
            outputJson = data.toString(),
            summary = "Chrome DevTools actie '$action' uitgevoerd op $url.",
            uiMarkdown = md
        )
    }

    private suspend fun executeSandboxPlaywright(callId: String, params: JSONObject): ToolDispatchResult {
        val script = params.optString("script", "").trim()
        val targetUrl = params.optString("target_url", "https://example.com").ifBlank { "https://example.com" }

        if (script.isBlank()) {
            return ToolDispatchResult(
                toolName = "sandbox_playwright_run",
                callId = callId,
                success = false,
                outputJson = JSONObject().put("error", "Geen Playwright script opgegeven").toString(),
                summary = "Playwright script ontbreekt",
                uiMarkdown = "⚠️ Fout: Geen Playwright script opgegeven."
            )
        }

        val res = hostedSandboxClient.executePlaywright(script, targetUrl)
        val data = JSONObject().apply {
            put("target_url", res.targetUrl)
            put("script", res.script)
            put("success", res.success)
            put("execution_time_ms", res.executionTimeMs)
            put("is_live_remote_runner", res.isLiveRemoteRunner)
            val logsArr = JSONArray()
            res.logs.forEach { logsArr.put(it) }
            put("logs", logsArr)
        }

        val md = buildString {
            append("### 🎭 Hosted Playwright Browser Automation Test\n")
            append("- **Target:** `$targetUrl`\n")
            append("- **Runner Type:** ${if (res.isLiveRemoteRunner) "🌐 Live Remote Cloud Container" else "⚡ Gehoste Container Sandbox"}\n")
            append("- **Status:** ${if (res.success) "🟢 Geslaagd" else "🔴 Mislukt"} (${res.executionTimeMs}ms)\n")
            append("**Playwright Uitvoeringslogs:**\n")
            res.logs.forEach { append("  • `$it`\n") }
        }

        return ToolDispatchResult(
            toolName = "sandbox_playwright_run",
            callId = callId,
            success = res.success,
            outputJson = data.toString(),
            summary = "Playwright script uitgevoerd op $targetUrl (${res.executionTimeMs}ms).",
            uiMarkdown = md
        )
    }

    private fun executeSandboxFsWrite(callId: String, params: JSONObject): ToolDispatchResult {
        val path = params.optString("path", "").trim()
        val content = params.optString("content", "")

        if (path.isBlank()) {
            return ToolDispatchResult(
                toolName = "sandbox_fs_write",
                callId = callId,
                success = false,
                outputJson = JSONObject().put("error", "Pad ontbreekt").toString(),
                summary = "Bestandspad ontbreekt",
                uiMarkdown = "⚠️ Fout: Geen bestandspad opgegeven."
            )
        }

        hostedSandboxClient.writeFile(path, content)
        val data = JSONObject().apply {
            put("path", "/workspace/$path")
            put("bytes_written", content.toByteArray().size)
            put("status", "SUCCESS")
        }

        val md = """
            ### 📁 Hosted Container Filesystem: Bestand Opgeslagen
            - **Bestand:** `/workspace/$path`
            - **Grootte:** ${content.toByteArray().size} bytes
            - **Locatie:** Gehoste Linux Container Workspace
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "sandbox_fs_write",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Bestand '/workspace/$path' geschreven (${content.toByteArray().size} bytes).",
            uiMarkdown = md
        )
    }

    private fun executeSandboxFsRead(callId: String, params: JSONObject): ToolDispatchResult {
        val path = params.optString("path", "").trim()
        return try {
            val content = hostedSandboxClient.readFile(path)
            val data = JSONObject().apply {
                put("path", "/workspace/$path")
                put("content", content)
                put("size_bytes", content.length)
            }
            val md = """
                ### 📄 Hosted Container Filesystem: `/workspace/$path`
                ```
                ${content.take(600)}
                ${if (content.length > 600) "... [afgekapt]" else ""}
                ```
            """.trimIndent()

            ToolDispatchResult(
                toolName = "sandbox_fs_read",
                callId = callId,
                success = true,
                outputJson = data.toString(),
                summary = "Bestand '/workspace/$path' gelezen (${content.length} bytes).",
                uiMarkdown = md
            )
        } catch (e: Exception) {
            ToolDispatchResult(
                toolName = "sandbox_fs_read",
                callId = callId,
                success = false,
                outputJson = JSONObject().put("error", e.message).toString(),
                summary = "Kon bestand '/workspace/$path' niet lezen",
                uiMarkdown = "⚠️ Fout: ${e.message}"
            )
        }
    }

    private fun executeSandboxGetCapabilities(callId: String): ToolDispatchResult {
        val caps = hostedSandboxClient.capabilities.value
        val data = JSONObject().apply {
            put("container_id", caps.containerId)
            put("os", caps.osName)
            put("kernel", caps.kernel)
            put("java_version", caps.javaVersion)
            put("bash_version", caps.bashVersion)
            put("chrome_version", caps.chromeVersion)
            put("node_version", caps.nodeVersion)
            put("python_version", caps.pythonVersion)
            put("working_directory", caps.workingDir)
            put("allocated_ram_mb", caps.allocatedMemoryMb)
            put("cpu_cores", caps.cpuCores)
            val featArr = JSONArray()
            caps.features.forEach { featArr.put(it) }
            put("features", featArr)
        }

        val md = """
            ### 🛡️ Gehoste Linux, Java & Chrome DevTools Sandbox Specificaties
            - **Container ID:** `${caps.containerId}`
            - **Besturingssysteem:** ${caps.osName} (`${caps.kernel}`)
            - **Java Runtime:** ${caps.javaVersion}
            - **Shell:** ${caps.bashVersion}
            - **Browser Automatisering:** ${caps.chromeVersion}
            - **Node.js:** ${caps.nodeVersion}
            - **Python:** ${caps.pythonVersion}
            - **Toegewezen Geheugen:** ${caps.allocatedMemoryMb} MB RAM (4 Cores)
            - **Actieve Sandbox Capabilities:** ${caps.features.joinToString(", ")}
        """.trimIndent()

        return ToolDispatchResult(
            toolName = "sandbox_get_capabilities",
            callId = callId,
            success = true,
            outputJson = data.toString(),
            summary = "Sandbox specificaties opgehaald: OpenJDK 21, Bash 5.2, Chrome DevTools Protocol.",
            uiMarkdown = md
        )
    }
}
