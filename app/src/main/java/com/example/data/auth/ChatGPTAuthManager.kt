package com.example.data.auth

import android.content.Context
import android.os.Environment
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class ChatGPTSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val idToken: String = "",
    val accountId: String = "",
    val email: String = "",
    val clientId: String = "app_EMoamEEZ73f0CkXaXp7hrann",
    val expiresAt: Long = 0L,
    val refreshedAt: Long = 0L
) {
    val isValid: Boolean get() = accessToken.isNotBlank() && (expiresAt == 0L || System.currentTimeMillis() < expiresAt)
}

data class ChatGPTModelInfo(
    val id: String,
    val name: String,
    val description: String = "",
    val reasoningLevels: List<String> = emptyList()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "assistant", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatGPTAuthManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "ChatGPTAuthManager"

    companion object {
        const val API_BASE = "https://chatgpt.com/backend-api/codex"
        const val PUBLIC_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        const val CODEX_VERSION = "0.155.1"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val authMutex = Mutex()

    private val authDir: File get() {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val vaultDir = File(downloadDir, "ObsidianVault")
        val dir = File(vaultDir, ".auth")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    val sessionFile: File get() {
        val primary = File(authDir, "chatgpt_session.json")
        if (primary.exists()) return primary
        val sharedVaultFile = File(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ObsidianVault/.auth"),
            "chatgpt_session.json"
        )
        return if (sharedVaultFile.exists()) sharedVaultFile else primary
    }

    private val modelsCacheFile: File get() = File(authDir, "chatgpt_models.json")

    private val _sessionState = MutableStateFlow<ChatGPTSession?>(null)
    val sessionState: StateFlow<ChatGPTSession?> = _sessionState.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _models = MutableStateFlow<List<ChatGPTModelInfo>>(loadModelsFromDisk())
    val models: StateFlow<List<ChatGPTModelInfo>> = _models.asStateFlow()

    private val _activeModel = MutableStateFlow<String>("")
    val activeModel: StateFlow<String> = _activeModel.asStateFlow()

    private val _selectedReasoningEffort = MutableStateFlow<String?>("medium")
    val selectedReasoningEffort: StateFlow<String?> = _selectedReasoningEffort.asStateFlow()

    private val activeStreams = ConcurrentHashMap<String, Call>()

    fun setActiveModel(modelId: String) {
        if (modelId.isNotBlank()) {
            _activeModel.value = modelId
        }
    }

    fun setSelectedReasoningEffort(effort: String?) {
        _selectedReasoningEffort.value = effort
    }

    init {
        val s = loadSessionFromDisk()
        if (s != null && s.isValid) {
            scope.launch(Dispatchers.IO) {
                try {
                    fetchModels()
                } catch (e: Exception) {
                    Log.w(TAG, "Initial dynamic models fetch deferred: ${e.message}")
                }
            }
        }
    }

    /**
     * Handmatig injecteren van een Bearer Token of JSON payload.
     * Geen OAuth handshake in de app: puur directe token/sessie parsing en validatie.
     * Indien ongeldig -> Result.failure (GEEN fallback).
     */
    fun injectManualToken(input: String): Result<ChatGPTSession> {
        val cleanInput = input.trim()
        if (cleanInput.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingevoerde token mag niet leeg zijn. (Geen fallback)"))
        }

        return try {
            val session = if (cleanInput.startsWith("{") && cleanInput.endsWith("}")) {
                // JSON payload input
                val json = JSONObject(cleanInput)
                val token = json.optString("accessToken", json.optString("access_token", ""))
                if (token.isBlank()) {
                    throw IllegalArgumentException("Geen 'accessToken' gevonden in de JSON payload.")
                }

                val accessClaims = parseJwt(token)
                val idToken = json.optString("idToken", json.optString("id_token", ""))
                val idClaims = parseJwt(idToken)
                val authClaim = idClaims.optJSONObject("https://api.openai.com/auth") ?: accessClaims.optJSONObject("https://api.openai.com/auth")
                val profileClaim = accessClaims.optJSONObject("https://api.openai.com/profile")

                val accountId = json.optString("accountId", authClaim?.optString("chatgpt_account_id", ""))
                val email = json.optString("email", idClaims.optString("email", profileClaim?.optString("email", accessClaims.optString("email", "ChatGPT User"))))
                val expFromJwt = accessClaims.optLong("exp", 0L) * 1000L
                val expiresAt = if (expFromJwt > 0) expFromJwt else json.optLong("expiresAt", json.optLong("expires_at", 0L))

                ChatGPTSession(
                    accessToken = token,
                    refreshToken = json.optString("refreshToken", json.optString("refresh_token", "")),
                    idToken = idToken,
                    accountId = accountId,
                    email = email,
                    clientId = json.optString("clientId", PUBLIC_CLIENT_ID),
                    expiresAt = expiresAt,
                    refreshedAt = System.currentTimeMillis()
                )
            } else {
                // Raw Bearer Token String (bijv. "Bearer eyJ..." of "eyJ...")
                val rawToken = if (cleanInput.startsWith("Bearer ", ignoreCase = true)) {
                    cleanInput.substring(7).trim()
                } else {
                    cleanInput
                }

                val accessClaims = parseJwt(rawToken)
                val authClaim = accessClaims.optJSONObject("https://api.openai.com/auth")
                val profileClaim = accessClaims.optJSONObject("https://api.openai.com/profile")

                val accountId = authClaim?.optString("chatgpt_account_id", "") ?: ""
                val email = profileClaim?.optString("email", accessClaims.optString("email", "ChatGPT Injected Token")) ?: "ChatGPT Injected Token"
                val expFromJwt = accessClaims.optLong("exp", 0L) * 1000L

                ChatGPTSession(
                    accessToken = rawToken,
                    refreshToken = "",
                    idToken = "",
                    accountId = accountId,
                    email = email,
                    clientId = PUBLIC_CLIENT_ID,
                    expiresAt = expFromJwt,
                    refreshedAt = System.currentTimeMillis()
                )
            }

            if (!session.isValid) {
                throw IllegalStateException("De ingevoerde ChatGPT token is verlopen of ongeldig. (Geen fallback)")
            }

            saveSessionToDisk(session)
            _sessionState.value = session

            // Dynamisch modellen ophalen met de nieuwe geldige token
            scope.launch(Dispatchers.IO) {
                try {
                    fetchModels()
                } catch (e: Exception) {
                    Log.w(TAG, "Fetch models after token injection failed: ${e.message}")
                }
            }

            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Token injection failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun loadSessionFromDisk(): ChatGPTSession? {
        return try {
            if (sessionFile.exists()) {
                val json = JSONObject(sessionFile.readText())
                val session = ChatGPTSession(
                    accessToken = json.optString("accessToken", ""),
                    refreshToken = json.optString("refreshToken", ""),
                    idToken = json.optString("idToken", ""),
                    accountId = json.optString("accountId", ""),
                    email = json.optString("email", ""),
                    clientId = json.optString("clientId", PUBLIC_CLIENT_ID),
                    expiresAt = json.optLong("expiresAt", 0L),
                    refreshedAt = json.optLong("refreshedAt", 0L)
                )
                if (session.isValid) {
                    _sessionState.value = session
                    session
                } else {
                    _sessionState.value = null
                    null
                }
            } else {
                _sessionState.value = null
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading chatgpt_session.json", e)
            _sessionState.value = null
            null
        }
    }

    fun saveSessionToDisk(session: ChatGPTSession) {
        try {
            val json = JSONObject().apply {
                put("accessToken", session.accessToken)
                put("refreshToken", session.refreshToken)
                put("idToken", session.idToken)
                put("accountId", session.accountId)
                put("email", session.email)
                put("clientId", session.clientId)
                put("expiresAt", session.expiresAt)
                put("refreshedAt", session.refreshedAt)
            }
            val targetFile = sessionFile
            val parentDir = targetFile.parentFile ?: authDir
            if (!parentDir.exists()) parentDir.mkdirs()
            val tempFile = File(parentDir, "${targetFile.name}.tmp")
            tempFile.writeText(json.toString(2))
            if (tempFile.renameTo(targetFile) || (targetFile.delete() && tempFile.renameTo(targetFile))) {
                Log.d(TAG, "Atomically saved chatgpt_session.json")
            } else {
                targetFile.writeText(json.toString(2))
                tempFile.delete()
            }
            _sessionState.value = session
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving chatgpt_session.json", e)
        }
    }

    fun clearSession() {
        try {
            if (sessionFile.exists()) {
                sessionFile.delete()
            }
            _sessionState.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing chatgpt session", e)
        }
    }

    fun loadModelsFromDisk(): List<ChatGPTModelInfo> {
        return try {
            if (modelsCacheFile.exists()) {
                val arr = JSONArray(modelsCacheFile.readText())
                val list = mutableListOf<ChatGPTModelInfo>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id")
                    val name = obj.optString("name", id)
                    val desc = obj.optString("description", "")
                    val rArr = obj.optJSONArray("reasoningLevels")
                    val reasonLevels = mutableListOf<String>()
                    if (rArr != null) {
                        for (j in 0 until rArr.length()) {
                            reasonLevels.add(rArr.getString(j))
                        }
                    }
                    if (id.isNotBlank()) {
                        list.add(ChatGPTModelInfo(id, name, desc, reasonLevels))
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading cached models", e)
            emptyList()
        }
    }

    fun saveModelsToDisk(models: List<ChatGPTModelInfo>) {
        try {
            val arr = JSONArray()
            for (m in models) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("description", m.description)
                    val rArr = JSONArray()
                    m.reasoningLevels.forEach { rArr.put(it) }
                    put("reasoningLevels", rArr)
                }
                arr.put(obj)
            }
            modelsCacheFile.writeText(arr.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed caching dynamic models to disk", e)
        }
    }

    fun getApiHeaders(): Map<String, String> {
        val session = _sessionState.value ?: loadSessionFromDisk()
            ?: throw IllegalStateException("Geen geldige ChatGPT Auth Token aanwezig. Plak eerst een geldige token of upload de sessie. (Geen fallback)")

        if (!session.isValid) {
            throw IllegalStateException("De actieve ChatGPT Token is verlopen. Vernieuw de token handmatig. (Geen fallback)")
        }

        val map = mutableMapOf<String, String>()
        map["Authorization"] = "Bearer ${session.accessToken}"
        map["originator"] = "codex_cli_rs"
        if (session.accountId.isNotBlank()) {
            map["ChatGPT-Account-ID"] = session.accountId
        }
        return map
    }

    suspend fun fetchModels(): List<ChatGPTModelInfo> = withContext(Dispatchers.IO) {
        _isLoadingModels.value = true
        val list = mutableListOf<ChatGPTModelInfo>()
        try {
            val endpoints = listOf(
                "$API_BASE/models?client_version=$CODEX_VERSION",
                "https://chatgpt.com/backend-api/models",
                "https://api.openai.com/v1/models"
            )

            for (url in endpoints) {
                try {
                    val reqBuilder = Request.Builder().url(url)
                    val headers = getApiHeaders()
                    headers.forEach { (k, v) -> reqBuilder.header(k, v) }

                    val resp = okHttpClient.newCall(reqBuilder.build()).execute()
                    val body = resp.body?.string() ?: ""

                    if (resp.isSuccessful && body.isNotBlank()) {
                        val json = JSONObject(body)
                        val arr = json.optJSONArray("models")
                            ?: json.optJSONArray("data")
                            ?: json.optJSONArray("categories")
                        if (arr != null && arr.length() > 0) {
                            for (i in 0 until arr.length()) {
                                val item = arr.get(i)
                                if (item is JSONObject) {
                                    val id = item.optString("slug", item.optString("id", ""))
                                    if (id.isNotBlank()) {
                                        val name = item.optString("display_name", item.optString("title", item.optString("name", id)))
                                        val desc = item.optString("description", item.optString("snippet", ""))
                                        val reasonArr = item.optJSONArray("supported_reasoning_levels")
                                        val reasonLevels = mutableListOf<String>()
                                        if (reasonArr != null) {
                                            for (j in 0 until reasonArr.length()) {
                                                reasonLevels.add(reasonArr.getString(j))
                                            }
                                        }
                                        if (list.none { it.id.equals(id, ignoreCase = true) }) {
                                            list.add(ChatGPTModelInfo(id, name, desc, reasonLevels))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Dynamic fetch from $url failed: ${e.message}")
                }

                if (list.isNotEmpty()) break
            }

            if (list.isNotEmpty()) {
                _models.value = list
                if (_activeModel.value.isBlank()) {
                    _activeModel.value = list.first().id
                }
                saveModelsToDisk(list)
            } else {
                val disk = loadModelsFromDisk()
                if (disk.isNotEmpty()) {
                    _models.value = disk
                    if (_activeModel.value.isBlank()) {
                        _activeModel.value = disk.first().id
                    }
                    list.addAll(disk)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching dynamic OpenAI models", e)
        } finally {
            _isLoadingModels.value = false
        }
        list
    }

    /**
     * Uitvoeren van real-time AI Streaming Responses via ChatGPT Codex Responses API.
     * Geen fallback: als de token ontbreekt of API 401 geeft, faalt de call direct met duidelijke foutmelding.
     */
    suspend fun streamResponses(
        model: String,
        messages: List<ChatMessage>,
        userPrompt: String,
        systemInstructions: String,
        toolsArray: JSONArray = JSONArray(),
        reasoningEffort: String? = null,
        onChunk: (String) -> Unit,
        onStatus: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val url = "$API_BASE/responses"
        val headers = getApiHeaders()

        val inputList = JSONArray()
        for (m in messages) {
            val msgObj = JSONObject().apply {
                put("type", "message")
                put("role", if (m.role == "user") "user" else "assistant")
                val contentArr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", if (m.role == "user") "input_text" else "output_text")
                        put("text", m.text)
                    })
                }
                put("content", contentArr)
            }
            inputList.put(msgObj)
        }

        val lastMsg = messages.lastOrNull()
        if (lastMsg == null || lastMsg.role != "user" || lastMsg.text != userPrompt) {
            inputList.put(JSONObject().apply {
                put("type", "message")
                put("role", "user")
                val contentArr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "input_text")
                        put("text", userPrompt)
                    })
                }
                put("content", contentArr)
            })
        }

        var effectiveModel = model.ifBlank { _activeModel.value }.ifBlank { _models.value.firstOrNull()?.id ?: "" }
        if (effectiveModel.isBlank()) {
            val liveModels = fetchModels()
            effectiveModel = liveModels.firstOrNull()?.id ?: _activeModel.value
        }

        if (effectiveModel.isBlank()) {
            throw IllegalStateException("Geen live OpenAI model beschikbaar. Enkel de nieuwste dynamische modellen van OpenAI zijn toegestaan (geen fallback).")
        }

        val payload = JSONObject().apply {
            put("model", effectiveModel)
            put("instructions", systemInstructions)
            put("input", inputList)
            put("stream", true)
            put("store", false)
            if (toolsArray.length() > 0) {
                put("tools", toolsArray)
                put("parallel_tool_calls", false)
            }
            if (!reasoningEffort.isNullOrBlank()) {
                put("reasoning", JSONObject().put("effort", reasoningEffort))
            }
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .apply {
                headers.forEach { (k, v) -> header(k, v) }
                header("Content-Type", "application/json")
                header("Accept", "text/event-stream")
            }
            .build()

        val streamId = "stream_${System.currentTimeMillis()}"
        val call = okHttpClient.newCall(request)
        activeStreams[streamId] = call

        val accumulatedText = StringBuilder()
        val inFlightFunctionCalls = mutableMapOf<String, Pair<String, StringBuilder>>()
        val completedToolBlocks = mutableSetOf<String>()

        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw IOException("ChatGPT API Fout (${response.code}): $errBody")
            }

            val source = response.body?.source() ?: throw IOException("Lege respons stream van ChatGPT API")
            var line: String? = null

            while (source.readUtf8Line().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.startsWith("data: ")) {
                    val dataStr = currentLine.substring(6).trim()
                    if (dataStr == "[DONE]") break

                    try {
                        val event = JSONObject(dataStr)
                        val type = event.optString("type")

                        if (type == "response.output_text.delta" || type == "response.refusal.delta") {
                            val delta = event.optString("delta", "")
                            if (delta.isNotEmpty()) {
                                accumulatedText.append(delta)
                                onChunk(delta)
                                onStatus("Antwoord genereren...")
                            }
                        } else if (type == "response.output_item.added") {
                            val item = event.optJSONObject("item")
                            if (item != null && item.optString("type") == "function_call") {
                                val callId = item.optString("call_id", item.optString("id", "call_${System.currentTimeMillis()}"))
                                val name = item.optString("name")
                                inFlightFunctionCalls[callId] = Pair(name, StringBuilder())
                                onStatus("Tool uitvoeren: $name...")
                            }
                        } else if (type == "response.function_call_arguments.delta") {
                            val callId = event.optString("call_id", inFlightFunctionCalls.keys.lastOrNull() ?: "")
                            val delta = event.optString("delta", "")
                            inFlightFunctionCalls[callId]?.second?.append(delta)
                        } else if (type == "response.function_call_arguments.done" || type == "response.output_item.done") {
                            val item = event.optJSONObject("item")
                            val callId = event.optString("call_id", item?.optString("call_id", item?.optString("id", "")) ?: "")
                            val name = item?.optString("name") ?: inFlightFunctionCalls[callId]?.first ?: ""
                            val args = item?.optString("arguments") ?: inFlightFunctionCalls[callId]?.second?.toString() ?: "{}"
                            if (name.isNotBlank()) {
                                val block = formatFunctionCallToToolBlock(name, args)
                                if (!completedToolBlocks.contains(block)) {
                                    completedToolBlocks.add(block)
                                    accumulatedText.append("\n\n").append(block).append("\n")
                                    onChunk("\n\n$block\n")
                                }
                            }
                        } else if (type == "response.created" || type == "response.in_progress" || type.startsWith("response.reasoning")) {
                            onStatus("Nadenken...")
                        } else if (type == "response.completed") {
                            val respObj = event.optJSONObject("response")
                            val outputArr = respObj?.optJSONArray("output")
                            if (outputArr != null) {
                                for (i in 0 until outputArr.length()) {
                                    val item = outputArr.getJSONObject(i)
                                    val itemType = item.optString("type")
                                    if (itemType == "function_call") {
                                        val name = item.optString("name")
                                        val args = item.optString("arguments")
                                        if (name.isNotBlank()) {
                                            val block = formatFunctionCallToToolBlock(name, args)
                                            if (!completedToolBlocks.contains(block)) {
                                                completedToolBlocks.add(block)
                                                accumulatedText.append("\n\n").append(block).append("\n")
                                                onChunk("\n\n$block\n")
                                            }
                                        }
                                    } else {
                                        val content = item.optJSONArray("content")
                                        if (content != null && accumulatedText.isEmpty()) {
                                            for (j in 0 until content.length()) {
                                                val c = content.getJSONObject(j)
                                                if (c.optString("type") == "output_text") {
                                                    val text = c.optString("text")
                                                    accumulatedText.append(text)
                                                    onChunk(text)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (type == "error" || type == "response.failed") {
                            val err = event.optJSONObject("error")?.optString("message") ?: "Stream fout"
                            throw IOException(err)
                        }
                    } catch (_: Exception) {}
                }
            }
        } finally {
            activeStreams.remove(streamId)
        }

        return@withContext accumulatedText.toString()
    }

    private fun formatFunctionCallToToolBlock(name: String, argsJsonStr: String): String {
        val argsObj = try {
            if (argsJsonStr.isNotBlank()) JSONObject(argsJsonStr) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }
        val toolCallObj = JSONObject().apply {
            put("action", "execute_tool")
            put("tool_name", name)
            put("parameters", argsObj)
        }
        return "```tool_call\n${toolCallObj.toString(2)}\n```"
    }

    fun cancelStream(streamId: String) {
        activeStreams[streamId]?.cancel()
        activeStreams.remove(streamId)
    }

    private fun parseJwt(token: String): JSONObject {
        if (token.isBlank()) return JSONObject()
        return try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                var base64 = parts[1].replace("-", "+").replace("_", "/")
                while (base64.length % 4 != 0) base64 += "="
                val decoded = Base64.decode(base64, Base64.DEFAULT)
                JSONObject(String(decoded, StandardCharsets.UTF_8))
            } else {
                JSONObject()
            }
        } catch (_: Exception) {
            JSONObject()
        }
    }
}
