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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TextButton
import com.example.data.repository.ClusterRoleMode
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

    val clusterRoleMode by viewModel.clusterRoleMode.collectAsState()
    val isJoinedToHost by viewModel.isJoinedToHost.collectAsState()
    val joinedHostAddress by viewModel.joinedHostAddress.collectAsState()
    val joinedHostPort by viewModel.joinedHostPort.collectAsState()
    val isJoiningCluster by viewModel.isJoiningCluster.collectAsState()
    val pingFeedback by viewModel.pingFeedback.collectAsState()
    val incomingClusterAlert by viewModel.incomingClusterAlert.collectAsState()
    val lanIp by viewModel.lanIp.collectAsState()

    var showAddNodeDialog by remember { mutableStateOf(false) }

    val onlineNodes = nodes.filter { it.status == ClusterNodeStatus.ONLINE }

    val subnetPrefix = if (lanIp.contains(".")) lanIp.substringBeforeLast(".") + "." else "192.168.1."

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = EdgeloomBackground,
        floatingActionButton = {
            if (clusterRoleMode == ClusterRoleMode.HOST) {
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
                        text = if (clusterRoleMode == ClusterRoleMode.HOST)
                            "Pool memory from devices on your Wi‑Fi. Layers are split by RAM."
                        else
                            "Connect this device as a worker to another phone hosting the model.",
                        fontSize = 12.sp,
                        color = EdgeloomTextMuted
                    )
                }
            }

            // Incoming notification alert banner
            if (incomingClusterAlert != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = EdgeloomCardElevated),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, EdgeloomPrimaryLime),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EdgeloomPrimaryLime, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = incomingClusterAlert!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EdgeloomTextPrimary,
                                    lineHeight = 17.sp
                                )
                            }
                            IconButton(onClick = { viewModel.dismissIncomingAlert() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = EdgeloomTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Ping feedback banner
            if (pingFeedback != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = EdgeloomCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomOnlineGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pingFeedback!!,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = EdgeloomOnlineGreen,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearPingFeedback() }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = EdgeloomTextDim, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Segmented Mode Selector: HOST vs WORKER
            item {
                TabRow(
                    selectedTabIndex = if (clusterRoleMode == ClusterRoleMode.HOST) 0 else 1,
                    containerColor = EdgeloomCard,
                    contentColor = EdgeloomPrimaryLime,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[if (clusterRoleMode == ClusterRoleMode.HOST) 0 else 1]),
                            color = EdgeloomPrimaryLime
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EdgeloomBorder, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = clusterRoleMode == ClusterRoleMode.HOST,
                        onClick = { viewModel.setClusterRoleMode(ClusterRoleMode.HOST) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Host Cluster (Leader)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = clusterRoleMode == ClusterRoleMode.WORKER,
                        onClick = { viewModel.setClusterRoleMode(ClusterRoleMode.WORKER) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Join a Cluster (Worker)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            if (clusterRoleMode == ClusterRoleMode.HOST) {
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
                                    CircularProgressIndicator(
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
                        onRemove = { viewModel.removeClusterNode(node.id) },
                        onPing = { viewModel.pingClusterNode(node.id) },
                        onNotify = { viewModel.sendTestNotification(node.id) }
                    )
                }
            } else {
                // WORKER MODE: Join a Cluster
                item {
                    JoinClusterCard(
                        isJoined = isJoinedToHost,
                        joinedHostAddress = joinedHostAddress,
                        joinedHostPort = joinedHostPort,
                        isJoining = isJoiningCluster,
                        localSubnetPrefix = subnetPrefix,
                        onJoin = { hostIp, hostPort, devName ->
                            viewModel.joinRemoteCluster(hostIp, hostPort, devName)
                        },
                        onPingHost = {
                            val hostNode = nodes.find { !it.isSelf }
                            if (hostNode != null) viewModel.pingClusterNode(hostNode.id)
                        },
                        onNotifyHost = {
                            val hostNode = nodes.find { !it.isSelf }
                            if (hostNode != null) viewModel.sendTestNotification(hostNode.id, "Test notification ping from worker phone!")
                        },
                        onLeave = { viewModel.leaveCluster() }
                    )
                }

                if (isJoinedToHost) {
                    item {
                        Text(
                            text = "CLUSTER PARTICIPANTS (${nodes.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomPrimaryLime,
                            letterSpacing = 1.sp
                        )
                    }

                    items(nodes, key = { it.id }) { node ->
                        EdgeloomDeviceCard(
                            node = node,
                            onToggle = { viewModel.toggleNodeStatus(node.id) },
                            onRemove = { viewModel.removeClusterNode(node.id) },
                            onPing = { viewModel.pingClusterNode(node.id) },
                            onNotify = { viewModel.sendTestNotification(node.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddNodeDialog) {
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
fun JoinClusterCard(
    isJoined: Boolean,
    joinedHostAddress: String?,
    joinedHostPort: Int,
    isJoining: Boolean,
    localSubnetPrefix: String,
    onJoin: (hostIp: String, hostPort: Int, deviceName: String) -> Unit,
    onPingHost: () -> Unit,
    onNotifyHost: () -> Unit,
    onLeave: () -> Unit
) {
    var hostIpText by remember(localSubnetPrefix) { mutableStateOf(localSubnetPrefix) }
    var hostPortText by remember { mutableStateOf("8080") }
    var deviceNameText by remember { mutableStateOf("Android Worker") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isJoined) EdgeloomOnlineGreen.copy(alpha = 0.6f) else EdgeloomBorder,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = EdgeloomCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isJoined) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(EdgeloomOnlineGreen, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STATUS · CONNECTED AS WORKER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EdgeloomOnlineGreen,
                            letterSpacing = 1.sp
                        )
                    }
                    Button(
                        onClick = onLeave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EdgeloomCardElevated,
                            contentColor = EdgeloomSignalEmber
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Connected to Leader: http://$joinedHostAddress:$joinedHostPort",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = EdgeloomTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This device is serving assigned model layers. You can ping the host or trigger a test notification to verify two-way connectivity.",
                    fontSize = 11.sp,
                    color = EdgeloomTextMuted,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPingHost,
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EdgeloomCardElevated,
                            contentColor = EdgeloomPrimaryLime
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeloomPrimaryLime.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ping Host", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNotifyHost,
                        modifier = Modifier.weight(1.2f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EdgeloomPrimaryLime,
                            contentColor = EdgeloomBackground
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Notification", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = "JOIN AN EXISTING CLUSTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EdgeloomPrimaryLime,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter the IP and port of the phone hosting the cluster. This device will connect as a worker and share its RAM for distributed inference.",
                    fontSize = 12.sp,
                    color = EdgeloomTextMuted,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Host / Leader IP Address", fontSize = 11.sp, color = EdgeloomTextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = hostIpText,
                    onValueChange = { hostIpText = it },
                    placeholder = { Text("e.g. 192.168.1.105", fontSize = 12.sp, color = EdgeloomTextDim) },
                    modifier = Modifier.fillMaxWidth().testTag("join_host_ip_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EdgeloomPrimaryLime,
                        unfocusedBorderColor = EdgeloomBorder,
                        focusedTextColor = EdgeloomTextPrimary,
                        unfocusedTextColor = EdgeloomTextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Host Port", fontSize = 11.sp, color = EdgeloomTextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = hostPortText,
                            onValueChange = { hostPortText = it },
                            placeholder = { Text("8080", fontSize = 12.sp, color = EdgeloomTextDim) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EdgeloomPrimaryLime,
                                unfocusedBorderColor = EdgeloomBorder,
                                focusedTextColor = EdgeloomTextPrimary,
                                unfocusedTextColor = EdgeloomTextPrimary
                            ),
                            singleLine = true
                        )
                    }

                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("This Device Name", fontSize = 11.sp, color = EdgeloomTextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = deviceNameText,
                            onValueChange = { deviceNameText = it },
                            placeholder = { Text("Worker Phone", fontSize = 12.sp, color = EdgeloomTextDim) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EdgeloomPrimaryLime,
                                unfocusedBorderColor = EdgeloomBorder,
                                focusedTextColor = EdgeloomTextPrimary,
                                unfocusedTextColor = EdgeloomTextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val port = hostPortText.toIntOrNull() ?: 8080
                        onJoin(hostIpText.trim(), port, deviceNameText.trim().ifBlank { "Worker Phone" })
                    },
                    enabled = hostIpText.isNotBlank() && !isJoining,
                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("join_cluster_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EdgeloomPrimaryLime,
                        contentColor = EdgeloomBackground
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EdgeloomBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting to Host...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect & Join Cluster", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
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
    onRemove: () -> Unit,
    onPing: () -> Unit,
    onNotify: () -> Unit
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
                            Text(
                                text = if (node.role == com.example.data.model.ClusterRole.LEADER) "HOST" else "WORKER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = EdgeloomPrimaryLime
                            )
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
                IconButton(onClick = onPing, modifier = Modifier.testTag("ping_${node.id}")) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = "Ping",
                        tint = EdgeloomPrimaryLime,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(onClick = onNotify, modifier = Modifier.testTag("notify_${node.id}")) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = "Send Test Notification",
                        tint = EdgeloomPrimaryLime,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle",
                        tint = if (isOnline) EdgeloomOnlineGreen else EdgeloomTextDim,
                        modifier = Modifier.size(19.dp)
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
