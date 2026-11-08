package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiModelInfo
import com.example.data.model.ClusterNode
import com.example.data.model.ClusterNodeStatus
import com.example.data.model.DownloadState
import com.example.data.model.ModelType
import com.example.ui.theme.ChartColor1
import com.example.ui.theme.ChartColor2
import com.example.ui.theme.ChartColor3
import com.example.ui.theme.ChartColor4
import com.example.ui.theme.ChartColor5
import com.example.ui.theme.EdgeloomBackground
import com.example.ui.theme.EdgeloomBorder
import com.example.ui.theme.EdgeloomCard
import com.example.ui.theme.EdgeloomCardElevated
import com.example.ui.theme.EdgeloomOnlineGreen
import com.example.ui.theme.EdgeloomPrimaryGlow
import com.example.ui.theme.EdgeloomPrimaryLime
import com.example.ui.theme.EdgeloomSignalEmber
import com.example.ui.theme.EdgeloomSignalGlow
import com.example.ui.theme.EdgeloomSurface
import com.example.ui.theme.EdgeloomTextDim
import com.example.ui.theme.EdgeloomTextMuted
import com.example.ui.theme.EdgeloomTextPrimary
import com.example.ui.theme.EdgeloomWarningAmber
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val computePoolUsableGb by viewModel.computePoolUsableGb.collectAsState()
    val totalCores by viewModel.totalCoresInPool.collectAsState()
    val clusterNodes by viewModel.clusterNodes.collectAsState()
    val activeTextModel by viewModel.activeTextModel.collectAsState()
    val activeImageModel by viewModel.activeImageModel.collectAsState()
    val models by viewModel.models.collectAsState()
    val serverConfig by viewModel.serverConfig.collectAsState()
    val lanIp by viewModel.lanIp.collectAsState()
    val installedCount by viewModel.installedCount.collectAsState()
    val onDiskGb by viewModel.totalOnDiskGb.collectAsState()
    val downloadingCount by viewModel.downloadingCount.collectAsState()
    val hardwareProfile by viewModel.hardwareProfile.collectAsState()

    val onlineNodes = clusterNodes.filter { it.status == ClusterNodeStatus.ONLINE }

    // Pick top recommended text and image model
    val recommendedText = models.firstOrNull { it.type == ModelType.TEXT && it.isRecommended }
        ?: models.firstOrNull { it.type == ModelType.TEXT }
    val recommendedImage = models.firstOrNull { it.type == ModelType.IMAGE && it.isRecommended }
        ?: models.firstOrNull { it.type == ModelType.IMAGE }

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
                Spacer(modifier = Modifier.height(6.dp))
                // 1. Compute Pool Card (Edgeloom flagship card)
                ComputePoolCard(
                    usableGb = computePoolUsableGb,
                    devicesCount = onlineNodes.size,
                    coresCount = totalCores,
                    gpuInfo = if (hardwareProfile.isVulkanSupported) "Vulkan GPU Acceleration" else "CPU Vector Engine",
                    nodes = onlineNodes,
                    onAddDeviceClick = { viewModel.navigateTo(AppScreen.CLUSTER) }
                )
            }

            item {
                // 2. Active Loaded Models (Chat & Image)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActiveModelOverviewCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Chat,
                        label = "Chat model",
                        name = activeTextModel?.name ?: "None loaded",
                        onClick = {
                            viewModel.setRunSubTab("CHAT")
                            viewModel.navigateTo(AppScreen.RUN)
                        }
                    )

                    ActiveModelOverviewCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Brush,
                        label = "Image model",
                        name = activeImageModel?.name ?: "None loaded",
                        onClick = {
                            viewModel.setRunSubTab("IMAGE")
                            viewModel.navigateTo(AppScreen.RUN)
                        }
                    )
                }
            }

            item {
                // 3. Recommended Now Card
                RecommendedNowCard(
                    textModel = recommendedText,
                    imageModel = recommendedImage,
                    poolBudgetGb = computePoolUsableGb,
                    onAllModelsClick = { viewModel.navigateTo(AppScreen.MODELS) },
                    onDownloadClick = { viewModel.startDownload(it) },
                    onLoadClick = { viewModel.loadModel(it) }
                )
            }

            item {
                // 4. OpenAI-Compatible API Card (Panel Ember)
                OpenAiServerBanner(
                    isRunning = serverConfig.isRunning,
                    host = lanIp,
                    port = serverConfig.port,
                    onClick = { viewModel.navigateTo(AppScreen.SERVER) }
                )
            }

            item {
                // 5. Quick Stats Grid (Installed, On disk, Downloading)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickStatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        label = "Installed",
                        value = "$installedCount"
                    )

                    QuickStatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Storage,
                        label = "On disk",
                        value = "${String.format("%.1f", onDiskGb)} GB"
                    )

                    QuickStatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Download,
                        label = "Downloading",
                        value = "$downloadingCount"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ComputePoolCard(
    usableGb: Float,
    devicesCount: Int,
    coresCount: Int,
    gpuInfo: String,
    nodes: List<ClusterNode>,
    onAddDeviceClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                Brush.radialGradient(
                    listOf(EdgeloomPrimaryLime.copy(alpha = 0.35f), Color.Transparent),
                    radius = 800f
                ),
                RoundedCornerShape(18.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "COMPUTE POOL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EdgeloomPrimaryLime,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", usableGb),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 54.sp,
                    color = EdgeloomPrimaryLime
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GB usable",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EdgeloomTextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$devicesCount ${if (devicesCount == 1) "device" else "devices"} · $coresCount cores · $gpuInfo",
                fontSize = 12.sp,
                color = EdgeloomTextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment proportional device RAM bar
            val chartColors = listOf(ChartColor1, ChartColor2, ChartColor3, ChartColor4, ChartColor5)
            val totalMemory = nodes.sumOf { it.totalMemoryMb }.coerceAtLeast(1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(EdgeloomSurface)
            ) {
                nodes.forEachIndexed { index, node ->
                    val weight = (node.totalMemoryMb.toFloat() / totalMemory).coerceAtLeast(0.05f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(8.dp)
                            .background(chartColors[index % chartColors.size])
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .clickable { onAddDeviceClick() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add a device to unlock bigger models",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EdgeloomPrimaryLime
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = EdgeloomPrimaryLime,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveModelOverviewCard(
    icon: ImageVector,
    label: String,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .border(1.dp, EdgeloomBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (name != "None loaded") EdgeloomPrimaryLime else EdgeloomTextDim,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = EdgeloomTextMuted,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EdgeloomTextPrimary,
                maxLines = 2
            )
        }
    }
}

@Composable
fun RecommendedNowCard(
    textModel: AiModelInfo?,
    imageModel: AiModelInfo?,
    poolBudgetGb: Float,
    onAllModelsClick: () -> Unit,
    onDownloadClick: (String) -> Unit,
    onLoadClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, EdgeloomBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EdgeloomPrimaryLime,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECOMMENDED NOW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "All models",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EdgeloomPrimaryLime,
                    modifier = Modifier.clickable { onAllModelsClick() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            listOfNotNull(textModel, imageModel).forEachIndexed { index, model ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(EdgeloomBorder)
                    )
                }

                RecommendedItemRow(
                    model = model,
                    poolBudgetGb = poolBudgetGb,
                    onDownloadClick = { onDownloadClick(model.id) },
                    onLoadClick = { onLoadClick(model.id) }
                )
            }
        }
    }
}

@Composable
fun RecommendedItemRow(
    model: AiModelInfo,
    poolBudgetGb: Float,
    onDownloadClick: () -> Unit,
    onLoadClick: () -> Unit
) {
    // Determine fit based on model minRamGb vs pool budget
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(EdgeloomCardElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (model.type == ModelType.TEXT) Icons.AutoMirrored.Filled.Chat else Icons.Default.Brush,
                contentDescription = null,
                tint = EdgeloomTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = EdgeloomTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${model.parameters} · ${model.formattedSize}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = EdgeloomTextMuted
            )
        }

        if (model.downloadState == DownloadState.DOWNLOADED) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(fitColor.copy(alpha = 0.15f))
                    .border(1.dp, fitColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = fitStatus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = fitColor
                )
            }
        } else {
            Button(
                onClick = onDownloadClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EdgeloomPrimaryLime,
                    contentColor = EdgeloomBackground
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = if (model.downloadState == DownloadState.DOWNLOADING)
                        "${(model.downloadProgress * 100).toInt()}%"
                    else "Get",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OpenAiServerBanner(
    isRunning: Boolean,
    host: String,
    port: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                if (isRunning) EdgeloomOnlineGreen.copy(alpha = 0.4f) else EdgeloomBorder,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isRunning) EdgeloomOnlineGreen.copy(alpha = 0.15f) else EdgeloomCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = if (isRunning) EdgeloomOnlineGreen else EdgeloomTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isRunning) EdgeloomOnlineGreen else EdgeloomTextDim, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OpenAI-compatible API",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (isRunning) "http://$host:$port/v1" else "Stopped — tap to start",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EdgeloomTextMuted
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = EdgeloomTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun QuickStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, EdgeloomBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EdgeloomTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = EdgeloomTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = EdgeloomTextMuted
            )
        }
    }
}
