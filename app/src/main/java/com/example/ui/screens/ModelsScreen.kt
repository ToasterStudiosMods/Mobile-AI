package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiModelInfo
import com.example.data.model.DownloadState
import com.example.data.model.ModelType
import com.example.ui.theme.EdgeloomBackground
import com.example.ui.theme.EdgeloomBorder
import com.example.ui.theme.EdgeloomCard
import com.example.ui.theme.EdgeloomCardElevated
import com.example.ui.theme.EdgeloomOnlineGreen
import com.example.ui.theme.EdgeloomPrimaryLime
import com.example.ui.theme.EdgeloomSignalEmber
import com.example.ui.theme.EdgeloomSurface
import com.example.ui.theme.EdgeloomTextDim
import com.example.ui.theme.EdgeloomTextMuted
import com.example.ui.theme.EdgeloomTextPrimary
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModelsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val models by viewModel.filteredModels.collectAsState()
    val activeFilter by viewModel.modelFilter.collectAsState()
    val activeLoadedModel by viewModel.activeLoadedModel.collectAsState()
    val poolBudgetGb by viewModel.computePoolUsableGb.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    val displayedModels = if (searchQuery.isBlank()) {
        models
    } else {
        models.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.repoId.contains(searchQuery, ignoreCase = true) ||
                    it.architecture.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EdgeloomBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomDialog = true },
                containerColor = EdgeloomPrimaryLime,
                contentColor = EdgeloomBackground,
                modifier = Modifier.testTag("add_custom_model_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add HF Model")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add HF Model", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
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
                // Edgeloom Title & Subtitle
                Column {
                    Text(
                        text = "MODEL CATALOG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomPrimaryLime,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Models",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Browse and download Hugging Face models, ranked by what actually fits your devices (${String.format("%.1f", poolBudgetGb)} GB pool memory).",
                        fontSize = 12.sp,
                        color = EdgeloomTextMuted,
                        lineHeight = 17.sp
                    )
                }
            }

            item {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search models, architectures, or authors...", fontSize = 12.sp, color = EdgeloomTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EdgeloomTextMuted, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EdgeloomCard,
                        unfocusedContainerColor = EdgeloomCard,
                        focusedBorderColor = EdgeloomPrimaryLime.copy(alpha = 0.5f),
                        unfocusedBorderColor = EdgeloomBorder,
                        focusedTextColor = EdgeloomTextPrimary,
                        unfocusedTextColor = EdgeloomTextPrimary
                    ),
                    singleLine = true
                )
            }

            item {
                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "ALL" to "All",
                        "TEXT" to "Chat & code",
                        "IMAGE" to "Image gen",
                        "DOWNLOADED" to "Installed"
                    ).forEach { (key, label) ->
                        val selected = activeFilter == key
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setModelFilter(key) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EdgeloomPrimaryLime.copy(alpha = 0.15f),
                                selectedLabelColor = EdgeloomPrimaryLime,
                                containerColor = EdgeloomCard,
                                labelColor = EdgeloomTextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = EdgeloomBorder,
                                selectedBorderColor = EdgeloomPrimaryLime
                            )
                        )
                    }
                }
            }

            items(displayedModels, key = { it.id }) { model ->
                EdgeloomModelCard(
                    model = model,
                    poolBudgetGb = poolBudgetGb,
                    isCurrentlyLoaded = model.isLoadedInVram,
                    onDownload = { viewModel.startDownload(model.id) },
                    onPause = { viewModel.pauseDownload(model.id) },
                    onLoad = { viewModel.loadModel(model.id) },
                    onDelete = { viewModel.deleteModel(model.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddCustomDialog) {
        AddCustomModelDialog(
            onConfirm = { repo, file, type ->
                viewModel.addCustomHfModel(repo, file, type)
                showAddCustomDialog = false
            },
            onDismiss = { showAddCustomDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EdgeloomModelCard(
    model: AiModelInfo,
    poolBudgetGb: Float,
    isCurrentlyLoaded: Boolean,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    // Fit status
    val fitStatus = when {
        model.minRamGb <= poolBudgetGb * 0.8f -> "GREAT FIT"
        model.minRamGb <= poolBudgetGb -> "TIGHT FIT"
        else -> "TOO BIG"
    }
    val fitColor = when (fitStatus) {
        "GREAT FIT" -> EdgeloomPrimaryLime
        "TIGHT FIT" -> EdgeloomSignalEmber
        else -> EdgeloomTextDim
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isCurrentlyLoaded) EdgeloomPrimaryLime.copy(alpha = 0.6f) else EdgeloomBorder,
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = model.repoId,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = EdgeloomTextMuted
                    )
                }

                // Fit Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(fitColor.copy(alpha = 0.15f))
                        .border(1.dp, fitColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = fitStatus,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = fitColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = model.description,
                fontSize = 12.sp,
                color = EdgeloomTextMuted,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Specs FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EdgeloomSpecBadge(text = model.parameters, color = EdgeloomTextPrimary)
                EdgeloomSpecBadge(text = model.quantization, color = EdgeloomPrimaryLime)
                EdgeloomSpecBadge(text = model.formattedSize, color = EdgeloomSignalEmber)
                EdgeloomSpecBadge(text = "Needs ${model.minRamGb}GB RAM", color = EdgeloomTextMuted)
            }

            // Download progress bar
            if (model.downloadState == DownloadState.DOWNLOADING || model.downloadState == DownloadState.PAUSED) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (model.downloadState == DownloadState.DOWNLOADING)
                                "Downloading from huggingface.co... ${String.format("%.1f", model.downloadSpeedMbps)} MB/s"
                            else "Paused",
                            fontSize = 11.sp,
                            color = EdgeloomPrimaryLime
                        )
                        Text(
                            text = "${(model.downloadProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { model.downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)),
                        color = EdgeloomPrimaryLime,
                        trackColor = EdgeloomSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (model.downloadState) {
                    DownloadState.NOT_DOWNLOADED -> {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EdgeloomPrimaryLime,
                                contentColor = EdgeloomBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("download_model_${model.id}")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download ${model.formattedSize}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    DownloadState.DOWNLOADING -> {
                        OutlinedButton(
                            onClick = onPause,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EdgeloomTextPrimary),
                            modifier = Modifier.testTag("pause_model_${model.id}")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause", fontSize = 12.sp)
                        }
                    }

                    DownloadState.PAUSED -> {
                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = EdgeloomPrimaryLime, contentColor = EdgeloomBackground),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("resume_model_${model.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", fontSize = 12.sp)
                        }
                    }

                    DownloadState.DOWNLOADED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!isCurrentlyLoaded) {
                                Button(
                                    onClick = onLoad,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EdgeloomPrimaryLime,
                                        contentColor = EdgeloomBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("load_model_${model.id}")
                                ) {
                                    Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Model", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(EdgeloomOnlineGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, EdgeloomOnlineGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).background(EdgeloomOnlineGreen, CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Active in VRAM", fontSize = 12.sp, color = EdgeloomOnlineGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.testTag("delete_model_${model.id}")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = EdgeloomTextDim)
                            }
                        }
                    }

                    DownloadState.ERROR -> {
                        Button(onClick = onDownload) {
                            Text("Retry Download", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EdgeloomSpecBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(EdgeloomCardElevated, RoundedCornerShape(6.dp))
            .border(1.dp, EdgeloomBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AddCustomModelDialog(
    onConfirm: (repoId: String, filename: String, type: ModelType) -> Unit,
    onDismiss: () -> Unit
) {
    var repoId by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ModelType.TEXT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EdgeloomCard,
        title = {
            Text("Add Hugging Face Model", fontWeight = FontWeight.Bold, color = EdgeloomTextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste any Hugging Face model repository ID. GGUF format is supported for text, and ONNX/Safetensors for image models.",
                    fontSize = 12.sp,
                    color = EdgeloomTextMuted
                )

                OutlinedTextField(
                    value = repoId,
                    onValueChange = { repoId = it },
                    label = { Text("Hugging Face Repo ID") },
                    placeholder = { Text("e.g. bartowski/Llama-3.2-3B-Instruct-GGUF") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EdgeloomPrimaryLime,
                        unfocusedBorderColor = EdgeloomBorder,
                        focusedTextColor = EdgeloomTextPrimary,
                        unfocusedTextColor = EdgeloomTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("custom_repo_input")
                )

                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("File Name (Optional)") },
                    placeholder = { Text("e.g. Llama-3.2-3B-Instruct-Q4_K_M.gguf") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EdgeloomPrimaryLime,
                        unfocusedBorderColor = EdgeloomBorder,
                        focusedTextColor = EdgeloomTextPrimary,
                        unfocusedTextColor = EdgeloomTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("custom_filename_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == ModelType.TEXT,
                        onClick = { selectedType = ModelType.TEXT },
                        colors = RadioButtonDefaults.colors(selectedColor = EdgeloomPrimaryLime)
                    )
                    Text("Chat & code", fontSize = 13.sp, color = EdgeloomTextPrimary)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = selectedType == ModelType.IMAGE,
                        onClick = { selectedType = ModelType.IMAGE },
                        colors = RadioButtonDefaults.colors(selectedColor = EdgeloomPrimaryLime)
                    )
                    Text("Image gen", fontSize = 13.sp, color = EdgeloomTextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (repoId.isNotBlank()) {
                        onConfirm(repoId, filename, selectedType)
                    }
                },
                enabled = repoId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EdgeloomPrimaryLime, contentColor = EdgeloomBackground),
                modifier = Modifier.testTag("confirm_add_model_button")
            ) {
                Text("Add Model", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = EdgeloomTextMuted)
            }
        }
    )
}
