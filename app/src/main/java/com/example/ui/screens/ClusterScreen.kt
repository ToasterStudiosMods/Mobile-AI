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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.ClusterNode
import com.example.data.model.ClusterNodeStatus
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
import com.example.ui.theme.EdgeloomPrimaryLime
import com.example.ui.theme.EdgeloomSignalEmber
import com.example.ui.theme.EdgeloomSurface
import com.example.ui.theme.EdgeloomTextDim
import com.example.ui.theme.EdgeloomTextMuted
import com.example.ui.theme.EdgeloomTextPrimary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ClusterScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val nodes by viewModel.clusterNodes.collectAsState()
    val isClusterEnabled by viewModel.isClusterEnabled.collectAsState()
    val poolBudgetGb by viewModel.computePoolUsableGb.collectAsState()

    var showAddNodeDialog by remember { mutableStateOf(false) }

    val onlineNodes = nodes.filter { it.status == ClusterNodeStatus.ONLINE }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EdgeloomBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNodeDialog = true },
                containerColor = EdgeloomPrimaryLime,
                contentColor = EdgeloomBackground,
                modifier = Modifier.testTag("pair_device_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Pair")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pair a Device", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                // Title
                Column {
                    Text(
                        text = "MESH NETWORK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomPrimaryLime,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cluster",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pool memory from devices on your Wi‑Fi. Layers are split by RAM.",
                        fontSize = 12.sp,
                        color = EdgeloomTextMuted
                    )
                }
            }

            item {
                // Layer Sharding Section
                EdgeloomLayerSplitCard(nodes = onlineNodes)
            }

            item {
                // Master Enable Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, EdgeloomBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cluster Workload Splitting",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EdgeloomTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isClusterEnabled) "Active · Pooling ${String.format("%.1f", poolBudgetGb)} GB across ${onlineNodes.size} devices" else "Disabled · Running purely locally",
                                fontSize = 11.sp,
                                color = if (isClusterEnabled) EdgeloomPrimaryLime else EdgeloomTextMuted
                            )
                        }

                        Switch(
                            checked = isClusterEnabled,
                            onCheckedChange = { viewModel.toggleCluster(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EdgeloomBackground,
                                checkedTrackColor = EdgeloomPrimaryLime,
                                uncheckedThumbColor = EdgeloomTextMuted,
                                uncheckedTrackColor = EdgeloomCardElevated
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DEVICES IN POOL (${nodes.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomPrimaryLime,
                        letterSpacing = 1.sp
                    )

                    val isScanning by viewModel.isScanningSubnet.collectAsState()
                    val scanStatus by viewModel.scanStatus.collectAsState()

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { viewModel.scanSubnet() },
                            enabled = !isScanning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EdgeloomCardElevated,
                                contentColor = EdgeloomPrimaryLime
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp).testTag("scan_subnet_button")
                        ) {
                            if (isScanning) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = EdgeloomPrimaryLime
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scanning LAN...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan Wi-Fi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                val scanStatus by viewModel.scanStatus.collectAsState()
                if (scanStatus.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EdgeloomCardElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, EdgeloomBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = scanStatus,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = EdgeloomPrimaryLime
                        )
                    }
                }
            }

            item {
                val efficiencyWarning by viewModel.clusterEfficiencyWarning.collectAsState()
                if (efficiencyWarning != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EdgeloomSignalEmber.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, EdgeloomSignalEmber.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = efficiencyWarning!!,
                            fontSize = 11.sp,
                            color = EdgeloomSignalEmber,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            items(nodes, key = { it.id }) { node ->
                EdgeloomDeviceCard(
                    node = node,
                    onToggle = { viewModel.toggleNodeStatus(node.id) },
                    onRemove = { viewModel.removeClusterNode(node.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddNodeDialog) {
        val lanIp by viewModel.lanIp.collectAsState()
        val serverConfig by viewModel.serverConfig.collectAsState()

        PairDeviceDialog(
            hostIp = lanIp,
            currentHostPort = serverConfig.port,
            onUpdateHostPort = { newPort -> viewModel.updateHostingPort(newPort) },
            onAdd = { name, ip, port ->
                viewModel.addClusterNode(name, ip, port)
                showAddNodeDialog = false
            },
            onDismiss = { showAddNodeDialog = false }
        )
    }
}

@Composable
fun EdgeloomLayerSplitCard(nodes: List<ClusterNode>) {
    val chartColors = listOf(ChartColor1, ChartColor2, ChartColor3, ChartColor4, ChartColor5)
    val totalLayers = 28

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
                Text(
                    text = "LAYER SPLIT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EdgeloomPrimaryLime,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$totalLayers layers total",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EdgeloomTextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-segment layer bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                nodes.forEachIndexed { index, node ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(chartColors[index % chartColors.size]),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = node.assignedLayers,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tokens in -> Layer 1", fontSize = 10.sp, color = EdgeloomTextDim)
                Text("Layer $totalLayers -> Projected out", fontSize = 10.sp, color = EdgeloomTextDim)
            }
        }
    }
}

@Composable
fun EdgeloomDeviceCard(
    node: ClusterNode,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val isOnline = node.status == ClusterNodeStatus.ONLINE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (isOnline) EdgeloomBorder else Color.DarkGray, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EdgeloomCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        node.name.contains("Tablet", ignoreCase = true) -> Icons.Default.Tablet
                        node.name.contains("Laptop", ignoreCase = true) || node.name.contains("Mac", ignoreCase = true) -> Icons.Default.Computer
                        else -> Icons.Default.PhoneAndroid
                    },
                    contentDescription = null,
                    tint = if (isOnline) EdgeloomPrimaryLime else EdgeloomTextDim,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = node.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextPrimary
                    )
                    if (node.isSelf) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(EdgeloomPrimaryLime.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("HOST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EdgeloomPrimaryLime)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${node.address}:${node.port} · ${node.assignedLayers}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EdgeloomTextMuted
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${node.totalMemoryMb / 1024} GB RAM · ${node.latencyMs}ms ping",
                    fontSize = 10.sp,
                    color = if (isOnline) EdgeloomOnlineGreen else EdgeloomTextDim
                )
            }

            if (!node.isSelf) {
                IconButton(onClick = onToggle) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle",
                        tint = if (isOnline) EdgeloomOnlineGreen else EdgeloomTextDim,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = EdgeloomTextDim, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun PairDeviceDialog(
    hostIp: String,
    currentHostPort: Int,
    onUpdateHostPort: (Int) -> Unit,
    onAdd: (name: String, ip: String, port: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedToClipboard by remember { mutableStateOf(false) }

    var myHostPortText by remember(currentHostPort) { mutableStateOf(currentHostPort.toString()) }
    var hostPortApplied by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var ip by remember {
        val prefix = if (hostIp.contains(".")) hostIp.substringBeforeLast(".") + "." else "192.168.1."
        mutableStateOf(prefix)
    }
    var portText by remember { mutableStateOf("8080") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EdgeloomCard,
        title = {
            Text("Cluster Pairing & Host Settings", fontWeight = FontWeight.Bold, color = EdgeloomTextPrimary, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Section 1: This Device (Host Info & Port Selection)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EdgeloomPrimaryLime.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = EdgeloomCardElevated)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "THIS DEVICE (HOST)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EdgeloomPrimaryLime,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("$hostIp:$myHostPortText"))
                                    copiedToClipboard = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy host address",
                                    tint = if (copiedToClipboard) EdgeloomOnlineGreen else EdgeloomTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "http://$hostIp:$myHostPortText",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = myHostPortText,
                                onValueChange = {
                                    myHostPortText = it
                                    hostPortApplied = false
                                },
                                label = { Text("Hosting Port", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EdgeloomPrimaryLime,
                                    unfocusedBorderColor = EdgeloomBorder,
                                    focusedTextColor = EdgeloomTextPrimary,
                                    unfocusedTextColor = EdgeloomTextPrimary
                                ),
                                modifier = Modifier.weight(1f).height(54.dp)
                            )

                            Button(
                                onClick = {
                                    val newP = myHostPortText.toIntOrNull() ?: 8080
                                    onUpdateHostPort(newP)
                                    hostPortApplied = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hostPortApplied) EdgeloomOnlineGreen else EdgeloomPrimaryLime,
                                    contentColor = EdgeloomBackground
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(if (hostPortApplied) "Saved" else "Set Port", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To pair from another device, enter the IP & Port above on that device.",
                            fontSize = 10.sp,
                            color = EdgeloomTextDim
                        )
                    }
                }

                // Section 2: Connect to a Peer Device (Worker)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CONNECT A PEER WORKER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EdgeloomTextMuted,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Peer Name (Optional)") },
                        placeholder = { Text("e.g. MacBook Pro, Second Phone") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EdgeloomPrimaryLime,
                            unfocusedBorderColor = EdgeloomBorder,
                            focusedTextColor = EdgeloomTextPrimary,
                            unfocusedTextColor = EdgeloomTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = { Text("Peer IP") },
                            placeholder = { Text("192.168.1.xxx") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EdgeloomPrimaryLime,
                                unfocusedBorderColor = EdgeloomBorder,
                                focusedTextColor = EdgeloomTextPrimary,
                                unfocusedTextColor = EdgeloomTextPrimary
                            ),
                            modifier = Modifier.weight(2f)
                        )

                        OutlinedTextField(
                            value = portText,
                            onValueChange = { portText = it },
                            label = { Text("Port") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EdgeloomPrimaryLime,
                                unfocusedBorderColor = EdgeloomBorder,
                                focusedTextColor = EdgeloomTextPrimary,
                                unfocusedTextColor = EdgeloomTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ip.isNotBlank()) {
                        onAdd(name.ifBlank { "Worker ${ip.takeLast(3)}" }, ip, portText.toIntOrNull() ?: 8080)
                    }
                },
                enabled = ip.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EdgeloomPrimaryLime, contentColor = EdgeloomBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Pair Device", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EdgeloomTextMuted)
            }
        }
    )
}
