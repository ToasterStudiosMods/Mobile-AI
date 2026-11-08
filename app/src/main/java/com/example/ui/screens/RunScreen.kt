package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.ImageGenResult
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
fun RunScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val runSubTab by viewModel.runSubTab.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EdgeloomBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text = "INTERACTIVE STUDIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EdgeloomPrimaryLime,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Run",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = EdgeloomTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chat with and generate images using models running entirely on your own devices.",
                    fontSize = 12.sp,
                    color = EdgeloomTextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tabs: Chat & code vs Image gen
            TabRow(
                selectedTabIndex = if (runSubTab == "CHAT") 0 else 1,
                containerColor = EdgeloomCard,
                contentColor = EdgeloomPrimaryLime,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (runSubTab == "CHAT") 0 else 1]),
                        color = EdgeloomPrimaryLime
                    )
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, EdgeloomBorder, RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = runSubTab == "CHAT",
                    onClick = { viewModel.setRunSubTab("CHAT") },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chat & code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )

                Tab(
                    selected = runSubTab == "IMAGE",
                    onClick = { viewModel.setRunSubTab("IMAGE") },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Image gen", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (runSubTab == "CHAT") {
                RunChatSection(viewModel = viewModel, modifier = Modifier.weight(1f))
            } else {
                RunImageGenSection(viewModel = viewModel, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RunChatSection(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val activeModel by viewModel.activeTextModel.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isGeneratingText.collectAsState()
    val streamingContent by viewModel.currentStreamingContent.collectAsState()
    val tps by viewModel.currentTps.collectAsState()
    val ttft by viewModel.ttftMs.collectAsState()
    val clusterNodes by viewModel.clusterNodes.collectAsState()
    val isClusterEnabled by viewModel.isClusterEnabled.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Telemetry header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).background(EdgeloomOnlineGreen, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeModel?.name ?: "llama-3.2-1b-instruct",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (tps > 0) {
                        Text(
                            text = "${String.format("%.1f", tps)} t/s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomPrimaryLime
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear", tint = EdgeloomTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            items(messages, key = { it.id }) { msg ->
                EdgeloomChatBubble(message = msg)
            }

            if (isGenerating && streamingContent.isNotEmpty()) {
                item {
                    EdgeloomStreamingBubble(content = streamingContent, tps = tps)
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Privacy note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = EdgeloomTextDim, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Runs on this device. Nothing you type leaves your network.",
                fontSize = 10.sp,
                color = EdgeloomTextDim
            )
        }

        // Input bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask anything or request code...", fontSize = 13.sp, color = EdgeloomTextMuted) },
                    modifier = Modifier.weight(1f).testTag("run_chat_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = EdgeloomTextPrimary,
                        unfocusedTextColor = EdgeloomTextPrimary
                    ),
                    maxLines = 4
                )

                if (isGenerating) {
                    IconButton(
                        onClick = { viewModel.stopGeneration() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(EdgeloomSignalEmber, CircleShape)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = EdgeloomTextPrimary)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendChatMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (inputText.isNotBlank()) EdgeloomPrimaryLime else EdgeloomCardElevated, CircleShape)
                            .testTag("run_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) EdgeloomBackground else EdgeloomTextDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EdgeloomChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .background(if (isUser) EdgeloomPrimaryLime.copy(alpha = 0.15f) else EdgeloomCard)
                .border(
                    1.dp,
                    if (isUser) EdgeloomPrimaryLime.copy(alpha = 0.35f) else EdgeloomBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(13.dp)
        ) {
            Column {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCAL MODEL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomPrimaryLime,
                            letterSpacing = 1.sp
                        )
                        if (message.tokensPerSecond > 0) {
                            Text(
                                text = "${String.format("%.1f", message.tokensPerSecond)} t/s",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = EdgeloomTextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.content,
                    fontSize = 13.sp,
                    color = EdgeloomTextPrimary,
                    lineHeight = 19.sp
                )

                if (message.clusterNodesInvolved.size > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚡ Sharded across ${message.clusterNodesInvolved.joinToString(", ")}",
                        fontSize = 10.sp,
                        color = EdgeloomOnlineGreen
                    )
                }
            }
        }
    }
}

@Composable
fun EdgeloomStreamingBubble(content: String, tps: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(14.dp))
                .background(EdgeloomCard)
                .border(1.dp, EdgeloomPrimaryLime.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(13.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 2.dp, color = EdgeloomPrimaryLime)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STREAMING...", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EdgeloomPrimaryLime)
                    }
                    if (tps > 0) {
                        Text("${String.format("%.1f", tps)} t/s", fontSize = 10.sp, color = EdgeloomPrimaryLime, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = content + " █", fontSize = 13.sp, color = EdgeloomTextPrimary, lineHeight = 19.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunImageGenSection(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val config by viewModel.imageGenConfig.collectAsState()
    val isGenerating by viewModel.isGeneratingImage.collectAsState()
    val currentStep by viewModel.imageGenStep.collectAsState()
    val intermediateBitmap by viewModel.intermediateBitmap.collectAsState()
    val results by viewModel.imageResults.collectAsState()

    var promptText by remember(config.prompt) { mutableStateOf(config.prompt) }
    var steps by remember(config.steps) { mutableIntStateOf(config.steps) }
    var guidance by remember(config.guidanceScale) { mutableFloatStateOf(config.guidanceScale) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                        Text(
                            text = "ON-DEVICE DIFFUSION STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomSignalEmber,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Strict Local Policy",
                            fontSize = 10.sp,
                            color = EdgeloomTextDim
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real on-device diffusion requires minimum 6GB RAM and downloaded UNet/ONNX weights (~1.8GB). Cloud AI fallbacks (Gemini/Pollinations) are completely disabled to ensure zero data leakage.",
                        fontSize = 11.sp,
                        color = EdgeloomTextMuted,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DIFFUSION PROMPT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomPrimaryLime,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = {
                            promptText = it
                            viewModel.updateImagePrompt(it)
                        },
                        placeholder = { Text("Describe the scene you want to synthesize...", fontSize = 13.sp, color = EdgeloomTextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("image_prompt_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EdgeloomPrimaryLime.copy(alpha = 0.5f),
                            unfocusedBorderColor = EdgeloomBorder,
                            focusedTextColor = EdgeloomTextPrimary,
                            unfocusedTextColor = EdgeloomTextPrimary
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Denoising Steps: $steps", fontSize = 12.sp, color = EdgeloomTextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("CFG: ${String.format("%.1f", guidance)}", fontSize = 12.sp, color = EdgeloomPrimaryLime)
                    }

                    Slider(
                        value = steps.toFloat(),
                        onValueChange = {
                            steps = it.toInt()
                            viewModel.updateImageSettings(steps, guidance, 512, 512)
                        },
                        valueRange = 5f..30f,
                        steps = 25,
                        colors = SliderDefaults.colors(thumbColor = EdgeloomPrimaryLime, activeTrackColor = EdgeloomPrimaryLime)
                    )

                    Button(
                        onClick = { viewModel.generateImage() },
                        enabled = !isGenerating && promptText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_image_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EdgeloomPrimaryLime,
                            contentColor = EdgeloomBackground
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EdgeloomBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Denoising Step $currentStep / $steps...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Image (On-Device)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (isGenerating || intermediateBitmap != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, EdgeloomPrimaryLime.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isGenerating) "PROGRESSIVE PASS: STEP $currentStep / $steps" else "SYNTHESIS READY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EdgeloomPrimaryLime
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (intermediateBitmap != null) {
                            Image(
                                bitmap = intermediateBitmap!!.asImageBitmap(),
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        items(results, key = { it.id }) { res ->
            GeneratedImageCard(result = res)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun GeneratedImageCard(result: ImageGenResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (result.bitmap != null) {
                Image(
                    bitmap = result.bitmap.asImageBitmap(),
                    contentDescription = result.config.prompt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EdgeloomCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "IMAGE GENERATION UNAVAILABLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomSignalEmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.clusterNodeContributions,
                            fontSize = 11.sp,
                            color = EdgeloomTextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"${result.config.prompt}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EdgeloomTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${result.config.steps} Steps · ${result.durationMs}ms",
                    fontSize = 11.sp,
                    color = EdgeloomTextMuted
                )
                Text(
                    text = result.clusterNodeContributions,
                    fontSize = 10.sp,
                    color = EdgeloomOnlineGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
