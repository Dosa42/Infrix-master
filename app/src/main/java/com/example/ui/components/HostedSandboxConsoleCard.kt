package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backend.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HostedSandboxConsoleCard(
    sandboxClient: HostedSandboxClient,
    onQuickPromptSelected: ((String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val capabilities by sandboxClient.capabilities.collectAsState()
    val status by sandboxClient.status.collectAsState()

    var selectedTab by remember { mutableStateOf("terminal") } // "terminal", "java", "cdp", "settings"

    // Terminal State
    var terminalInput by remember { mutableStateOf("java -version") }
    var isRunningBash by remember { mutableStateOf(false) }
    val commandLog = remember {
        mutableStateListOf(
            "✦ [SANDBOX PTY READY] Hosted Linux container allocated: ${capabilities.containerId}",
            "✦ OpenJDK 21.0.3, Bash 5.2, Chrome DevTools Protocol bound to ws://127.0.0.1:9222",
            "✦ Workspace directory: /workspace (UID: 1000 sandbox)\n"
        )
    }

    // Java Runner State
    var javaClassName by remember { mutableStateOf("Main") }
    var javaSourceCode by remember {
        mutableStateOf(
            """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("✦ Hallo vanuit de Gehoste OpenJDK 21 Sandbox!");
                    System.out.println("Java Versie: " + System.getProperty("java.version"));
                    System.out.println("Geheugen Max: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
                    System.out.println("Beschikbare CPU Cores: " + Runtime.getRuntime().availableProcessors());
                }
            }
            """.trimIndent()
        )
    }
    var isRunningJava by remember { mutableStateOf(false) }
    var javaResult by remember { mutableStateOf<SandboxJavaResult?>(null) }

    // Chrome DevTools (CDP) State
    var cdpUrl by remember { mutableStateOf("https://example.com") }
    var cdpAction by remember { mutableStateOf("navigate") }
    var isRunningCdp by remember { mutableStateOf(false) }
    var cdpResult by remember { mutableStateOf<ChromeDevToolsResult?>(null) }

    // Settings
    var endpointInput by remember { mutableStateOf(sandboxClient.endpointUrl) }
    var apiKeyInput by remember { mutableStateOf(sandboxClient.apiKey) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hosted_sandbox_console_card"),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        border = BorderStroke(1.dp, PrimaryBlueGlow.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gehoste Linux, Java & Chrome Sandbox",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Container: ${capabilities.containerId} • OpenJDK 21 • Bash 5.2 • CDP v1.3",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(StatusSuccess.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(1.dp, StatusSuccess, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "● HOSTED READY",
                        color = StatusSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkAppBackground, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("terminal", "💻 Terminal", PrimaryBlueGlow),
                    Triple("java", "☕ Java 21", StatusWarning),
                    Triple("cdp", "🌐 DevTools", StatusInfo),
                    Triple("settings", "⚙️ Endpoint", TextSecondary)
                ).forEach { (id, label, color) ->
                    val isSelected = selectedTab == id
                    Button(
                        onClick = { selectedTab = id },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) color else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab 1: Terminal / Bash
            if (selectedTab == "terminal") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "java -version",
                            "ls -la /workspace",
                            "cat Main.java",
                            "uname -a",
                            "whoami",
                            "curl -I https://google.com"
                        ).forEach { presetCmd ->
                            OutlinedButton(
                                onClick = {
                                    terminalInput = presetCmd
                                    coroutineScope.launch {
                                        isRunningBash = true
                                        val res = sandboxClient.executeBash(presetCmd)
                                        isRunningBash = false
                                        commandLog.add("$ $presetCmd")
                                        if (res.stdout.isNotBlank()) commandLog.add(res.stdout.trim())
                                        if (res.stderr.isNotBlank()) commandLog.add("[stderr] ${res.stderr.trim()}")
                                        commandLog.add("[Exit Code: ${res.exitCode}] (${res.executionTimeMs}ms)\n")
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Text(presetCmd, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            }
                        }
                    }

                    // Terminal Screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color(0xFF090D16), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        val scroll = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(scroll)) {
                            commandLog.forEach { line ->
                                Text(
                                    text = line,
                                    color = if (line.startsWith("$")) PrimaryBlueGlow else if (line.contains("error") || line.contains("stderr")) StatusDanger else Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = terminalInput,
                            onValueChange = { terminalInput = it },
                            placeholder = { Text("Voer bash commando in...", fontSize = 12.sp, color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryBlueGlow,
                                unfocusedBorderColor = DarkTextFieldBorder,
                                focusedContainerColor = DarkTextField,
                                unfocusedContainerColor = DarkTextField
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (terminalInput.isNotBlank() && !isRunningBash) {
                                    val cmd = terminalInput
                                    coroutineScope.launch {
                                        isRunningBash = true
                                        val res = sandboxClient.executeBash(cmd)
                                        isRunningBash = false
                                        commandLog.add("$ $cmd")
                                        if (res.stdout.isNotBlank()) commandLog.add(res.stdout.trim())
                                        if (res.stderr.isNotBlank()) commandLog.add("[stderr] ${res.stderr.trim()}")
                                        commandLog.add("[Exit Code: ${res.exitCode}] (${res.executionTimeMs}ms)\n")
                                    }
                                }
                            })
                        )

                        Button(
                            onClick = {
                                if (terminalInput.isNotBlank() && !isRunningBash) {
                                    val cmd = terminalInput
                                    coroutineScope.launch {
                                        isRunningBash = true
                                        val res = sandboxClient.executeBash(cmd)
                                        isRunningBash = false
                                        commandLog.add("$ $cmd")
                                        if (res.stdout.isNotBlank()) commandLog.add(res.stdout.trim())
                                        if (res.stderr.isNotBlank()) commandLog.add("[stderr] ${res.stderr.trim()}")
                                        commandLog.add("[Exit Code: ${res.exitCode}] (${res.executionTimeMs}ms)\n")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueGlow),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isRunningBash
                        ) {
                            if (isRunningBash) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Uitvoeren", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Tab 2: Java 21 Sandbox Runner
            if (selectedTab == "java") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Java Bestand: $javaClassName.java (OpenJDK 21)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )

                        Button(
                            onClick = {
                                if (javaSourceCode.isNotBlank() && !isRunningJava) {
                                    coroutineScope.launch {
                                        isRunningJava = true
                                        javaResult = sandboxClient.compileAndRunJava(javaClassName, javaSourceCode)
                                        isRunningJava = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            enabled = !isRunningJava
                        ) {
                            if (isRunningJava) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black)
                            } else {
                                Text("▶ Compileer & Start JVM", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = javaSourceCode,
                        onValueChange = { javaSourceCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC),
                            focusedBorderColor = StatusWarning,
                            unfocusedBorderColor = DarkTextFieldBorder,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )

                    // Output Card
                    javaResult?.let { res ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (res.compilationSuccess) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFF7F1D1D).copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, if (res.compilationSuccess) StatusSuccess else StatusDanger),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (res.compilationSuccess) "🟢 JVM Executie Succesvol (${res.executionTimeMs}ms)" else "🔴 Compilatiefout",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (res.compilationSuccess) StatusSuccess else StatusDanger
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (res.compilationSuccess) res.runtimeOutput else res.compilerOutput,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Tab 3: Chrome DevTools Protocol (CDP)
            if (selectedTab == "cdp") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cdpUrl,
                            onValueChange = { cdpUrl = it },
                            placeholder = { Text("https://example.com", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = StatusInfo,
                                unfocusedBorderColor = DarkTextFieldBorder,
                                focusedContainerColor = DarkTextField,
                                unfocusedContainerColor = DarkTextField
                            )
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isRunningCdp = true
                                    cdpResult = sandboxClient.executeChromeDevTools("navigate", cdpUrl)
                                    isRunningCdp = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusInfo),
                            shape = RoundedCornerShape(6.dp),
                            enabled = !isRunningCdp
                        ) {
                            if (isRunningCdp) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White)
                            } else {
                                Text("Inspecteer", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // CDP & Playwright Action Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("screenshot", "📸 Screenshot"),
                            Pair("inspect_dom", "🔍 Inspect DOM"),
                            Pair("evaluate_js", "⚡ Eval JS"),
                            Pair("playwright", "🎭 Playwright Test")
                        ).forEach { (act, label) ->
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isRunningCdp = true
                                        if (act == "playwright") {
                                            val pwScript = "const page = await browser.newPage(); await page.goto('$cdpUrl');"
                                            val pwRes = sandboxClient.executePlaywright(pwScript, cdpUrl)
                                            cdpResult = ChromeDevToolsResult(
                                                action = "playwright",
                                                url = cdpUrl,
                                                success = pwRes.success,
                                                title = "Playwright Test op $cdpUrl (${if (pwRes.isLiveRemoteRunner) "Remote Container" else "Sandbox Container"})",
                                                consoleLogs = pwRes.logs
                                            )
                                        } else {
                                            cdpResult = sandboxClient.executeChromeDevTools(act, cdpUrl, script = "document.title")
                                        }
                                        isRunningCdp = false
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.5f))
                            ) {
                                Text(label, fontSize = 10.sp, color = StatusInfo)
                            }
                        }
                    }

                    // CDP Results Card
                    cdpResult?.let { res ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, StatusInfo),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🌐 Pagina: ${res.title}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusInfo)
                                if (res.consoleLogs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Console Logs:", fontSize = 10.sp, color = TextMuted)
                                    res.consoleLogs.forEach { log ->
                                        Text("• $log", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                                    }
                                }
                                if (res.htmlSnapshot.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("DOM Tree:", fontSize = 10.sp, color = TextMuted)
                                    Text(res.htmlSnapshot.take(200), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }
                }
            }

            // Tab 4: Settings & Custom Hosted Endpoint
            if (selectedTab == "settings") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Gehoste Sandbox Endpoint Configuratie",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = {
                            endpointInput = it
                            sandboxClient.endpointUrl = it
                        },
                        label = { Text("Sandbox Server URL (Docker / E2B / Cloud REST)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlueGlow,
                            unfocusedBorderColor = DarkTextFieldBorder
                        )
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            sandboxClient.apiKey = it
                        },
                        label = { Text("Bearer Token / API Key (optioneel)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlueGlow,
                            unfocusedBorderColor = DarkTextFieldBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Quick Prompt Buttons that invoke the sandbox tools
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Pair("☕ Test Java in Sandbox", "Schrijf en voer via sandbox_java_run een Java programma uit in de gehoste OpenJDK 21 sandbox dat priemgetallen berekent."),
                    Pair("💻 Run Bash in Container", "Voer via sandbox_bash_exec het commando 'java -version && ls -la /workspace' uit in de gehoste container."),
                    Pair("🌐 Chrome DevTools CDP", "Gebruik sandbox_chrome_devtools om naar https://example.com te navigeren en de DOM structuur en paginatitel te inspecteren.")
                ).forEach { (label, prompt) ->
                    AssistChip(
                        onClick = { onQuickPromptSelected?.invoke(prompt) },
                        label = { Text(label, fontSize = 10.sp, color = TextPrimary) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = DarkCardSurfaceVariant)
                    )
                }
            }
        }
    }
}
