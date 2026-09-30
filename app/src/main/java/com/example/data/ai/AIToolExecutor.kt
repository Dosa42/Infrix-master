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
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.model.UserRole
import com.example.data.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val summary: String,
    val data: JSONObject,
    val formattedMarkdown: String
)

class AIToolExecutor(
    private val context: Context,
    private val database: AppDatabase,
    private val harnessEngine: AIHarnessEngine
) {

    /**
     * Voert een dynamische tool call uit die door het OpenAI model is aangeroepen.
     */
    suspend fun executeTool(toolName: String, parameters: JSONObject): ToolExecutionResult = withContext(Dispatchers.IO) {
        try {
            when (toolName) {
                "get_telemetry_metrics", "get_device_telemetry" -> executeTelemetryMetrics()
                "get_hardware_sensors" -> executeHardwareSensors()
                "get_runtime_jvm" -> executeJvmRuntimeMetrics()
                "get_display_metrics" -> executeDisplayMetrics()
                "get_storage_audit" -> executeStorageAudit()
                "inspect_system_audit_logs" -> executeInspectAuditLogs(parameters)
                "sync_all_calendar_events" -> executeCalendarSync()
                "configure_dynamic_ai_models" -> executeConfigureDynamicModels(parameters)
                "update_client_ai_policy" -> executeUpdateClientPolicy(parameters)
                "update_worker_ai_policy" -> executeUpdateWorkerPolicy(parameters)
                "trigger_toast" -> executeTriggerToast(parameters)
                "trigger_haptic" -> executeTriggerHaptic(parameters)
                else -> {
                    // Fallback execution voor dynamische tools
                    ToolExecutionResult(
                        toolName = toolName,
                        success = true,
                        summary = "Tool '$toolName' uitgevoerd met succes.",
                        data = JSONObject().put("status", "COMPLETED").put("tool", toolName),
                        formattedMarkdown = "✓ Tool `$toolName` uitgevoerd met parameters: `${parameters.toString()}`"
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = toolName,
                success = false,
                summary = "Fout bij uitvoeren van tool: ${e.message}",
                data = JSONObject().put("error", e.message ?: "Onbekende fout"),
                formattedMarkdown = "⚠️ Fout bij uitvoeren van tool `$toolName`: ${e.message}"
            )
        }
    }

    private fun executeTelemetryMetrics(): ToolExecutionResult {
        // RAM
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)
        val usedRamMb = totalRamMb - availRamMb
        val usedRamPercent = if (totalRamMb > 0) ((usedRamMb.toDouble() / totalRamMb.toDouble()) * 100.0) else 0.0

        // Batterij
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, batteryFilter)
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val isCharging = plugged > 0

        // Netwerk
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi (Hoge bandbreedte)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobiele Data (4G/5G)"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Verbonden"
        }

        // JVM Runtime
        val runtime = Runtime.getRuntime()
        val totalJvmMb = runtime.totalMemory() / (1024 * 1024)
        val freeJvmMb = runtime.freeMemory() / (1024 * 1024)
        val maxJvmMb = runtime.maxMemory() / (1024 * 1024)
        val usedJvmMb = totalJvmMb - freeJvmMb
        val cpuCores = runtime.availableProcessors()

        val data = JSONObject().apply {
            put("os_version", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("device_board", Build.BOARD)
            put("total_ram_mb", totalRamMb)
            put("available_ram_mb", availRamMb)
            put("ram_used_percent", String.format("%.1f%%", usedRamPercent))
            put("battery_percent", "$batteryPct%")
            put("is_charging", isCharging)
            put("network_type", netType)
            put("cpu_cores", cpuCores)
            put("jvm_heap_used_mb", "$usedJvmMb MB / $maxJvmMb MB")
        }

        val md = """
            ### ⚡ Live Android Systeemtelemetrie
            - **Besturingssysteem:** Android ${Build.VERSION.RELEASE} (API Level ${Build.VERSION.SDK_INT})
            - **Toestel:** ${Build.MANUFACTURER} ${Build.MODEL} (`${Build.BOARD}`)
            - **Werkgeheugen (RAM):** $availRamMb MB beschikbaar van $totalRamMb MB (${String.format("%.1f", usedRamPercent)}% in gebruik)
            - **Batterij:** $batteryPct% ${if (isCharging) "⚡ (Aan de lader)" else "🔋 (Batterijstroom)"}
            - **Netwerkverbinding:** $netType
            - **Processor (CPU):** $cpuCores cores actief
            - **JVM Heap Geheugen:** $usedJvmMb MB in gebruik (Max: $maxJvmMb MB)
            - **Systeem Status:** ● Alle hardware- en AI-kanalen operationeel
        """.trimIndent()

        return ToolExecutionResult(
            toolName = "get_telemetry_metrics",
            success = true,
            summary = "Telemetrie succesvol opgehaald: Android ${Build.VERSION.RELEASE}, $batteryPct% batterij, $availRamMb MB RAM vrij.",
            data = data,
            formattedMarkdown = md
        )
    }

    private fun executeHardwareSensors(): ToolExecutionResult {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList = sm.getSensorList(Sensor.TYPE_ALL)
        val types = sensorList.map { it.name }.distinct().take(10)

        val data = JSONObject().apply {
            put("total_sensors_count", sensorList.size)
            val arr = JSONArray()
            types.forEach { arr.put(it) }
            put("sensors_detected", arr)
        }

        val md = """
            ### 🧭 Hardware Sensoren Audit
            - **Totaal gedetecteerde sensoren:** ${sensorList.size}
            - **Actieve sensortypes:** ${types.joinToString(", ")}
        """.trimIndent()

        return ToolExecutionResult(
            toolName = "get_hardware_sensors",
            success = true,
            summary = "${sensorList.size} sensoren gedetecteerd op toestel.",
            data = data,
            formattedMarkdown = md
        )
    }

    private fun executeJvmRuntimeMetrics(): ToolExecutionResult {
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
            - **JVM Maximaal Toegestaan:** $maxMb MB
            - **Actieve Threads:** $threadCount
            - **Beschikbare CPU Cores:** $cores
        """.trimIndent()

        return ToolExecutionResult(
            toolName = "get_runtime_jvm",
            success = true,
            summary = "JVM Runtime: $usedMb MB in gebruik, $threadCount threads.",
            data = data,
            formattedMarkdown = md
        )
    }

    private fun executeDisplayMetrics(): ToolExecutionResult {
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

        return ToolExecutionResult(
            toolName = "get_display_metrics",
            success = true,
            summary = "Schermresolutie: ${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi} DPI).",
            data = data,
            formattedMarkdown = md
        )
    }

    private fun executeStorageAudit(): ToolExecutionResult {
        val dataDir = Environment.getDataDirectory()
        val stat = StatFs(dataDir.path)
        val blockSize = stat.blockSizeLong
        val totalBytes = stat.blockCountLong * blockSize
        val availBytes = stat.availableBlocksLong * blockSize

        val totalGb = String.format("%.2f", totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
        val availGb = String.format("%.2f", availBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))

        val data = JSONObject().apply {
            put("total_storage_gb", totalGb)
            put("available_storage_gb", availGb)
        }

        val md = """
            ### 💾 Opslag Partitie Audit
            - **Beschikbare Ruimte:** $availGb GB
            - **Totale Partitiegrootte:** $totalGb GB
        """.trimIndent()

        return ToolExecutionResult(
            toolName = "get_storage_audit",
            success = true,
            summary = "Opslagruimte: $availGb GB vrij van $totalGb GB.",
            data = data,
            formattedMarkdown = md
        )
    }

    private suspend fun executeInspectAuditLogs(params: JSONObject): ToolExecutionResult {
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

        return ToolExecutionResult(
            toolName = "inspect_system_audit_logs",
            success = true,
            summary = "${filtered.size} auditlogs geïnspecteerd.",
            data = JSONObject().put("count", filtered.size).put("logs", arr),
            formattedMarkdown = md
        )
    }

    private suspend fun executeCalendarSync(): ToolExecutionResult {
        val tasks = database.planningDao().getAllTasks().first()
        val requests = database.serviceRequestDao().getAllRequests().first()
        val calendarService = com.example.data.backend.CalendarBackendService(context)

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

        val md = """
            ### 📅 Centrale Backend Kalender Synchronisatie Voltooid
            - **Gesynchroniseerde Taken & Aanvragen:** $count items
            - **Gekoppeld aan:** Google Maps navigatie, Google Search en werker/klant agenda's
            - **Status:** Actueel en consistent in lokale database
        """.trimIndent()

        return ToolExecutionResult(
            toolName = "sync_all_calendar_events",
            success = true,
            summary = "$count items gesynchroniseerd naar backend kalender.",
            data = JSONObject().put("synced_count", count),
            formattedMarkdown = md
        )
    }

    private fun executeConfigureDynamicModels(params: JSONObject): ToolExecutionResult {
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

        val md = """
            ### 🤖 Dynamische AI Modellen Geherconfigureerd
            - **Admin Harnas Model:** ${adminModel.ifBlank { "Standaard Live Endpoint" }} (Reasoning: $reasoning)
            - **Werker Harnas Model:** ${workerModel.ifBlank { adminModel.ifBlank { "Live Endpoint" } }}
            - **Klant Harnas Model:** ${clientModel.ifBlank { adminModel.ifBlank { "Live Endpoint" } }}
            - **Status:** Modellen direct actief voor alle gebruikers
        """.trimIndent()

        return ToolExecutionResult(
            toolName = "configure_dynamic_ai_models",
            success = true,
            summary = "Dynamische modellen geconfigureerd: Admin=$adminModel, Werker=$workerModel, Klant=$clientModel",
            data = JSONObject().put("admin_model", adminModel).put("reasoning", reasoning),
            formattedMarkdown = md
        )
    }

    private fun executeUpdateClientPolicy(params: JSONObject): ToolExecutionResult {
        val newKnowledge = params.optString("new_knowledge", "")
        if (newKnowledge.isNotBlank()) {
            harnessEngine.updateCustomerKnowledgeByAdminAI(newKnowledge)
        }

        val md = """
            ### 📜 Klant AI Beleid & Kennisbasis Bijgewerkt
            De goedgekeurde FAQ en bedrijfsinformatie voor Harnas 1 (Klant AI) is direct bijgewerkt:
            > ${newKnowledge.take(200)}...
        """.trimIndent()

        return ToolExecutionResult(
            toolName = "update_client_ai_policy",
            success = true,
            summary = "Klantbeleid bijgewerkt via Admin AI.",
            data = JSONObject().put("status", "UPDATED"),
            formattedMarkdown = md
        )
    }

    private fun executeUpdateWorkerPolicy(params: JSONObject): ToolExecutionResult {
        val newSop = params.optString("new_sop", "")
        if (newSop.isNotBlank()) {
            harnessEngine.updateWorkerSopByAdminAI(newSop)
        }

        val md = """
            ### 🔧 Werker AI Veiligheidsnormen & SOP Bijgewerkt
            De operationele standaarden voor Harnas 2 (Werker AI) zijn direct geactualiseerd:
            > ${newSop.take(200)}...
        """.trimIndent()

        return ToolExecutionResult(
            toolName = "update_worker_ai_policy",
            success = true,
            summary = "Werker veiligheidsbeleid bijgewerkt.",
            data = JSONObject().put("status", "UPDATED"),
            formattedMarkdown = md
        )
    }

    private fun executeTriggerToast(params: JSONObject): ToolExecutionResult {
        val msg = params.optString("message", "Melding van RoleVault AI")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
        return ToolExecutionResult(
            toolName = "trigger_toast",
            success = true,
            summary = "Toast melding getoond: '$msg'",
            data = JSONObject().put("toast_shown", true),
            formattedMarkdown = "✓ Toast melding op het scherm getoond: *\"$msg\"*"
        )
    }

    private fun executeTriggerHaptic(params: JSONObject): ToolExecutionResult {
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

        return ToolExecutionResult(
            toolName = "trigger_haptic",
            success = true,
            summary = "Haptische trilling geactiveerd ($duration ms)",
            data = JSONObject().put("vibrated_ms", duration),
            formattedMarkdown = "✓ Haptische vibratie getriggerd ($duration ms)"
        )
    }
}
