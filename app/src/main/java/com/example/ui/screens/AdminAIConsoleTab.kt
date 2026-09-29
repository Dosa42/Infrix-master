package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AIHarnessCategory
import com.example.data.auth.ChatMessage
import com.example.ui.theme.AdminPrimary
import com.example.ui.theme.DarkAppBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkCardSurfaceVariant
import com.example.ui.theme.DarkTextField
import com.example.ui.theme.DarkTextFieldBorder
import com.example.ui.theme.KlantPrimary
import com.example.ui.theme.PrimaryBlueGlow
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WerkerPrimary
import com.example.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminAIConsoleTab(
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val session by viewModel.chatGPTSession.collectAsState()
    val models by viewModel.chatGPTModels.collectAsState()

    var tokenInput by remember { mutableStateOf("") }
    var isSavingToken by remember { mutableStateOf(false) }
    var tokenFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Chat State for Admin AI Master Orchestrator
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var promptInput by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamStatusText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(DarkAppBackground),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // -------------------------------------------------------------
        // 1. CHATGPT AUTH TOKEN & INJECTIE CONSOLE (GEEN OAUTH PROMPT)
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, PrimaryBlueGlow.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ ChatGPT Auth Token & Sessie Injector",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Handmatige invoer van Bearer Token of Sessie JSON. Geen aanvraag in app; strikt geactiveerd op geldige token.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    if (session?.isValid == true) StatusSuccess.copy(alpha = 0.2f) else StatusDanger.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (session?.isValid == true) StatusSuccess else StatusDanger,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (session?.isValid == true) "● AI Backend Gewapend" else "● Geen Geldige Token",
                                color = if (session?.isValid == true) StatusSuccess else StatusDanger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    session?.let { activeSession ->
                        if (activeSession.isValid) {
                            val expDate = if (activeSession.expiresAt > 0) {
                                SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date(activeSession.expiresAt))
                            } else {
                                "Onbeperkt / Geen exp"
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkCardSurfaceVariant),
                                border = BorderStroke(1.dp, DarkCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "Geauthenticeerd Account: ${activeSession.email}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "Account ID: ${activeSession.accountId.ifBlank { "N/A" }}", color = TextSecondary, fontSize = 11.sp)
                                    Text(text = "Token Vervaltijd: $expDate", color = StatusWarning, fontSize = 11.sp)
                                    Text(text = "Beschikbare Modellen: ${models.size} live geladen", color = PrimaryBlueGlow, fontSize = 11.sp)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.clearChatGPTSession {
                                                Toast.makeText(context, "Sessie gewist", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger),
                                        border = BorderStroke(1.dp, StatusDanger)
                                    ) {
                                        Text("Sessie & Token Wissen", fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Plak Bearer Token (Bearer eyJ...) of volledige JSON Sessie", color = TextMuted, fontSize = 12.sp) },
                        placeholder = { Text("eyJhbGciOiJSUzI1NiIs...", color = TextMuted.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlueGlow,
                            unfocusedBorderColor = DarkTextFieldBorder,
                            focusedContainerColor = DarkTextField,
                            unfocusedContainerColor = DarkTextField
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_chatgpt_token_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (tokenInput.isNotBlank()) {
                                    isSavingToken = true
                                    viewModel.injectChatGPTToken(tokenInput) { success, msg ->
                                        isSavingToken = false
                                        tokenFeedback = Pair(success, msg)
                                        if (success) {
                                            tokenInput = ""
                                            Toast.makeText(context, "Token opgeslagen!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueGlow),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_save_token_button")
                        ) {
                            if (isSavingToken) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Token Injecteren & Valideren", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    tokenInput = clip
                                    Toast.makeText(context, "Gekopieerd uit klembord", Toast.LENGTH_SHORT).show()
                                }
                            },
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier.testTag("admin_paste_token_button")
                        ) {
                            Text("📋 Plak Klembord", color = TextPrimary, fontSize = 12.sp)
                        }
                    }

                    tokenFeedback?.let { (success, msg) ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (success) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF7F1D1D).copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (success) StatusSuccess else StatusDanger, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = msg, color = if (success) StatusSuccess else StatusDanger, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. DRIE-HARNASSEN ARCHITECTUUR & BRUG SCHEMA
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏛️ 3-Tier AI Harnassen & Exclusieve Admin Brug",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "De menselijke beheerder communiceert uitsluitend met de Admin AI. De Admin AI regeert en configureert automatisch de Werker en Klant AI.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Harnas 1: Klant AI
                        HarnessOverviewCard(
                            tier = "Harnas 1",
                            name = "Klant AI",
                            role = "Klantportaal Sandbox",
                            isolation = "Strikt Geïsoleerd: Alleen goedgekeurde FAQ & eigen aanvragen. Geen toegang tot werker/admin.",
                            accentColor = KlantPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Harnas 2: Werker AI
                        HarnessOverviewCard(
                            tier = "Harnas 2",
                            name = "Werker AI",
                            role = "Monteur Co-Pilot",
                            isolation = "Deterministische Policy: Werkorders, NEN-veiligheidsnormen, urenhulp. Geen admintoegang.",
                            accentColor = WerkerPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        // Harnas 3: Admin AI
                        HarnessOverviewCard(
                            tier = "Harnas 3",
                            name = "Admin AI",
                            role = "Master Orchestrator",
                            isolation = "Volledige Autoriteit: Dev tools, telemetrie, audits & exclusieve configuratiebrug voor 1 & 2.",
                            accentColor = AdminPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. INTERACTIEVE ADMIN AI MASTER CONSOLE (CHAT & TOOL EXECUTION)
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, AdminPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🧠 Admin Master AI Console",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Centrale besturing voor het volledige systeem en automatische sturing van Klant- en Werker AI",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        if (isStreaming) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = AdminPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = streamStatusText.ifBlank { "AI Actief..." }, color = AdminPrimary, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (chatMessages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkAppBackground, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Stel een vraag of geef een instructie aan de Admin Master AI (bijv. 'Controleer systeemgezondheid', 'Werk klant AI richtlijnen bij' of 'Inspecteer werkorders').",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Chat message bubbles
        items(chatMessages, key = { it.id }) { msg ->
            ChatMessageBubble(message = msg)
        }

        // Chat Input Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Instructie voor Admin Master AI...", color = TextMuted, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AdminPrimary,
                            unfocusedBorderColor = DarkTextFieldBorder,
                            focusedContainerColor = DarkTextField,
                            unfocusedContainerColor = DarkTextField
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_ai_chat_input")
                    )

                    Button(
                        onClick = {
                            if (promptInput.isNotBlank() && !isStreaming) {
                                val userText = promptInput
                                promptInput = ""
                                val userMsg = ChatMessage(role = "user", text = userText)
                                chatMessages.add(userMsg)

                                val assistantMsgId = java.util.UUID.randomUUID().toString()
                                val assistantMsg = ChatMessage(id = assistantMsgId, role = "assistant", text = "")
                                chatMessages.add(assistantMsg)

                                isStreaming = true
                                streamStatusText = "Verbinden met AI..."

                                viewModel.sendHarnessPrompt(
                                    category = AIHarnessCategory.ADMIN,
                                    messages = chatMessages.dropLast(1),
                                    userPrompt = userText,
                                    onChunk = { chunk ->
                                        val idx = chatMessages.indexOfFirst { it.id == assistantMsgId }
                                        if (idx != -1) {
                                            val existing = chatMessages[idx]
                                            chatMessages[idx] = existing.copy(text = existing.text + chunk)
                                        }
                                    },
                                    onStatus = { status ->
                                        streamStatusText = status
                                    },
                                    onComplete = { success, finalOutput ->
                                        isStreaming = false
                                        streamStatusText = ""
                                        if (!success) {
                                            val idx = chatMessages.indexOfFirst { it.id == assistantMsgId }
                                            if (idx != -1) {
                                                chatMessages[idx] = ChatMessage(
                                                    id = assistantMsgId,
                                                    role = "assistant",
                                                    text = "⚠️ $finalOutput"
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                        modifier = Modifier.testTag("admin_ai_send_button")
                    ) {
                        Text("Verzend", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun HarnessOverviewCard(
    tier: String,
    name: String,
    role: String,
    isolation: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardSurfaceVariant),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = tier, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(text = name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = role, color = TextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = isolation, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) Color(0xFF1E3A8A) else DarkCardSurfaceVariant
    val borderColor = if (isUser) PrimaryBlueGlow else DarkCardBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = align
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(1.dp, borderColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "U (Admin)" else "Admin Master AI",
                    color = if (isUser) PrimaryBlueGlow else AdminPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text.ifBlank { "..." },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
