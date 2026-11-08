package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Http
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerConfig
import com.example.data.model.ServerLog
import com.example.ui.theme.EdgeloomBackground
import com.example.ui.theme.EdgeloomBorder
import com.example.ui.theme.EdgeloomCard
import com.example.ui.theme.EdgeloomCardElevated
import com.example.ui.theme.EdgeloomCodeBackground
import com.example.ui.theme.EdgeloomOnlineGreen
import com.example.ui.theme.EdgeloomPrimaryLime
import com.example.ui.theme.EdgeloomSignalEmber
import com.example.ui.theme.EdgeloomSurface
import com.example.ui.theme.EdgeloomTextDim
import com.example.ui.theme.EdgeloomTextMuted
import com.example.ui.theme.EdgeloomTextPrimary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ServerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val serverConfig by viewModel.serverConfig.collectAsState()
    val serverLogs by viewModel.serverLogs.collectAsState()
    val lanIp by viewModel.lanIp.collectAsState()
    val activeModel by viewModel.activeLoadedModel.collectAsState()
    val testResult by viewModel.serverTestResult.collectAsState()
    val isTesting by viewModel.isTestingServer.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedSnippetTab by remember { mutableIntStateOf(0) }
    var testPrompt by remember { mutableStateOf("Explain how local AI models execute on Android") }

    val fullBaseUrl = "http://$lanIp:${serverConfig.port}/v1"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EdgeloomBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Title
                Column {
                    Text(
                        text = "LOCAL ENDPOINT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomPrimaryLime,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Local API server",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Point any OpenAI SDK, IDE plugin or workflow tool at your phone.",
                        fontSize = 12.sp,
                        color = EdgeloomTextMuted
                    )
                }
            }

            item {
                // Engine Address Card
                EdgeloomServerControlCard(
                    config = serverConfig,
                    lanUrl = fullBaseUrl,
                    activeModelName = activeModel?.name ?: "llama-3.2-1b-instruct",
                    onToggleServer = { viewModel.toggleServer(it) },
                    onCopyUrl = {
                        clipboardManager.setText(AnnotatedString(fullBaseUrl))
                        Toast.makeText(context, "Copied endpoint URL", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                // Supported Endpoints
                EdgeloomEndpointsCard()
            }

            item {
                // In-App Endpoint Tester
                EdgeloomTesterCard(
                    testPrompt = testPrompt,
                    onPromptChange = { testPrompt = it },
                    isTesting = isTesting,
                    testResult = testResult,
                    onRunTest = { viewModel.testServerEndpoint(testPrompt) }
                )
            }

            item {
                // Use it from your code snippets (Python, JavaScript, cURL)
                EdgeloomCodeSnippetsCard(
                    selectedTab = selectedSnippetTab,
                    onTabSelected = { selectedSnippetTab = it },
                    baseUrl = fullBaseUrl,
                    apiKey = serverConfig.apiKey,
                    modelId = activeModel?.id ?: "llama-3.2-1b-instruct",
                    onCopyCode = { code ->
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Copied snippet", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REQUEST LOG (${serverLogs.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomPrimaryLime,
                        letterSpacing = 1.sp
                    )

                    if (serverLogs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearServerLogs() }) {
                            Icon(Icons.Default.ClearAll, contentDescription = "Clear", tint = EdgeloomTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (serverLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No requests yet. Try the Run tab.", fontSize = 12.sp, color = EdgeloomTextMuted)
                        }
                    }
                }
            } else {
                items(serverLogs, key = { it.id }) { log ->
                    EdgeloomLogItem(log = log)
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun EdgeloomServerControlCard(
    config: ServerConfig,
    lanUrl: String,
    activeModelName: String,
    onToggleServer: (Boolean) -> Unit,
    onCopyUrl: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (config.isRunning) EdgeloomOnlineGreen.copy(alpha = 0.5f) else EdgeloomBorder,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(if (config.isRunning) EdgeloomOnlineGreen else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (config.isRunning) "Status: Running" else "Status: Stopped",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.isRunning) EdgeloomOnlineGreen else EdgeloomTextMuted
                    )
                }

                Switch(
                    checked = config.isRunning,
                    onCheckedChange = onToggleServer,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EdgeloomBackground,
                        checkedTrackColor = EdgeloomPrimaryLime,
                        uncheckedThumbColor = EdgeloomTextMuted,
                        uncheckedTrackColor = EdgeloomCardElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Engine address
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EdgeloomCodeBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, EdgeloomBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Engine address", fontSize = 10.sp, color = EdgeloomTextDim)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = lanUrl,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomPrimaryLime
                        )
                    }

                    IconButton(onClick = onCopyUrl) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EdgeloomPrimaryLime, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Model: $activeModelName", fontSize = 11.sp, color = EdgeloomTextMuted)
                Text("Served: ${config.totalRequestsServed} calls", fontSize = 11.sp, color = EdgeloomTextMuted)
            }
        }
    }
}

@Composable
fun EdgeloomEndpointsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("ENDPOINTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EdgeloomPrimaryLime, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            EdgeloomEndpointRow("POST", "/v1/chat/completions", "JSON & SSE text/event-stream")
            EdgeloomEndpointRow("GET", "/v1/models", "Discovery model catalog")
            EdgeloomEndpointRow("POST", "/v1/images/generations", "Latent Diffusion b64_json")
            EdgeloomEndpointRow("GET", "/health", "Cluster node health")
        }
    }
}

@Composable
fun EdgeloomEndpointRow(method: String, path: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(if (method == "POST") EdgeloomSignalEmber.copy(alpha = 0.2f) else EdgeloomPrimaryLime.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = method,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (method == "POST") EdgeloomSignalEmber else EdgeloomPrimaryLime
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = path, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = EdgeloomTextPrimary)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "· $detail", fontSize = 10.sp, color = EdgeloomTextMuted)
    }
}

@Composable
fun EdgeloomTesterCard(
    testPrompt: String,
    onPromptChange: (String) -> Unit,
    isTesting: Boolean,
    testResult: String?,
    onRunTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("TEST FROM DEVICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EdgeloomPrimaryLime, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = testPrompt,
                onValueChange = onPromptChange,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EdgeloomPrimaryLime,
                    unfocusedBorderColor = EdgeloomBorder,
                    focusedTextColor = EdgeloomTextPrimary,
                    unfocusedTextColor = EdgeloomTextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onRunTest,
                enabled = !isTesting,
                colors = ButtonDefaults.buttonColors(containerColor = EdgeloomPrimaryLime, contentColor = EdgeloomBackground),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = EdgeloomBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calling Local Engine...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Test Request (http://127.0.0.1:8080/v1)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (testResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EdgeloomCodeBackground, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = testResult,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = EdgeloomTextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EdgeloomCodeSnippetsCard(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    baseUrl: String,
    apiKey: String,
    modelId: String,
    onCopyCode: (String) -> Unit
) {
    val tabTitles = listOf("Python", "JavaScript", "cURL")

    val pythonSnippet = """
from openai import OpenAI

client = OpenAI(
    base_url="$baseUrl",
    api_key="$apiKey"
)

response = client.chat.completions.create(
    model="$modelId",
    messages=[{"role": "user", "content": "Explain cluster parallelism"}],
    stream=True
)

for chunk in response:
    print(chunk.choices[0].delta.content or "", end="", flush=True)
    """.trimIndent()

    val jsSnippet = """
import OpenAI from "openai";

const openai = new OpenAI({
  baseURL: "$baseUrl",
  apiKey: "$apiKey"
});

const stream = await openai.chat.completions.create({
  model: "$modelId",
  messages: [{ role: "user", content: "Hello Edgeloom!" }],
  stream: true,
});

for await (const chunk of stream) {
  process.stdout.write(chunk.choices[0]?.delta?.content || "");
}
    """.trimIndent()

    val curlSnippet = """
curl $baseUrl/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $apiKey" \
  -d '{
    "model": "$modelId",
    "messages": [{"role": "user", "content": "Hello on-device AI!"}],
    "stream": true
  }'
    """.trimIndent()

    val currentSnippet = when (selectedTab) {
        0 -> pythonSnippet
        1 -> jsSnippet
        else -> curlSnippet
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("USE IT FROM YOUR CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EdgeloomPrimaryLime, letterSpacing = 1.sp)
                IconButton(onClick = { onCopyCode(currentSnippet) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EdgeloomPrimaryLime, modifier = Modifier.size(15.dp))
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = EdgeloomPrimaryLime,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EdgeloomPrimaryLime
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EdgeloomCodeBackground, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = currentSnippet,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EdgeloomTextPrimary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun EdgeloomLogItem(log: ServerLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (log.statusCode == 200) EdgeloomOnlineGreen.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${log.method} ${log.statusCode}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.statusCode == 200) EdgeloomOnlineGreen else Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = log.path, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = EdgeloomTextPrimary)
                }

                Text(text = "${log.durationMs}ms", fontSize = 11.sp, color = EdgeloomPrimaryLime, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Client: ${log.clientIp}", fontSize = 10.sp, color = EdgeloomTextMuted)
                Text(text = log.nodeProcessing, fontSize = 10.sp, color = EdgeloomOnlineGreen)
            }
        }
    }
}
