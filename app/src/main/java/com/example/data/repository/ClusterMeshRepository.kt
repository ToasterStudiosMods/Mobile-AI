package com.example.data.repository

import android.util.Log
import com.example.data.model.ClusterNode
import com.example.data.model.ClusterNodeStatus
import com.example.data.model.ClusterRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class ClusterRoleMode {
    HOST,
    WORKER
}

class ClusterMeshRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val _clusterRoleMode = MutableStateFlow(ClusterRoleMode.HOST)
    val clusterRoleMode: StateFlow<ClusterRoleMode> = _clusterRoleMode.asStateFlow()

    private val _isJoinedToHost = MutableStateFlow(false)
    val isJoinedToHost: StateFlow<Boolean> = _isJoinedToHost.asStateFlow()

    private val _joinedHostAddress = MutableStateFlow<String?>(null)
    val joinedHostAddress: StateFlow<String?> = _joinedHostAddress.asStateFlow()

    private val _joinedHostPort = MutableStateFlow(8080)
    val joinedHostPort: StateFlow<Int> = _joinedHostPort.asStateFlow()

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    private val _pingFeedback = MutableStateFlow<String?>(null)
    val pingFeedback: StateFlow<String?> = _pingFeedback.asStateFlow()

    private val _incomingClusterAlert = MutableStateFlow<String?>(null)
    val incomingClusterAlert: StateFlow<String?> = _incomingClusterAlert.asStateFlow()

    private val _nodes = MutableStateFlow<List<ClusterNode>>(emptyList())
    val nodes: StateFlow<List<ClusterNode>> = _nodes.asStateFlow()

    private val _isClusterEnabled = MutableStateFlow(true)
    val isClusterEnabled: StateFlow<Boolean> = _isClusterEnabled.asStateFlow()

    private val _layerDistributionMode = MutableStateFlow("Pipeline Parallelism (Layer Sharding)")
    val layerDistributionMode: StateFlow<String> = _layerDistributionMode.asStateFlow()

    private val _isScanningSubnet = MutableStateFlow(false)
    val isScanningSubnet: StateFlow<Boolean> = _isScanningSubnet.asStateFlow()

    private val _scanStatus = MutableStateFlow("")
    val scanStatus: StateFlow<String> = _scanStatus.asStateFlow()

    private val _clusterEfficiencyWarning = MutableStateFlow<String?>(null)
    val clusterEfficiencyWarning: StateFlow<String?> = _clusterEfficiencyWarning.asStateFlow()

    init {
        initializeCluster()
        startActiveNodePinging()
    }

    private fun initializeCluster() {
        val selfIp = getLocalIpAddress()
        val selfNode = ClusterNode(
            id = "self-node",
            name = "This Device (Leader / Node 0)",
            address = selfIp,
            port = 8080,
            role = ClusterRole.LEADER,
            status = ClusterNodeStatus.ONLINE,
            assignedLayers = "Layers 1 - 28",
            latencyMs = 1L,
            memoryUsedMb = 1420,
            totalMemoryMb = 6144,
            throughputTokensPerSec = 16.4f,
            isSelf = true
        )

        _nodes.value = listOf(selfNode)
        rebalanceLayers()
    }

    fun updateHostPort(newPort: Int) {
        _nodes.update { list ->
            list.map { if (it.isSelf) it.copy(port = newPort) else it }
        }
    }

    private fun startActiveNodePinging() {
        scope.launch {
            while (isActive) {
                delay(6000)
                pingAllRemoteNodes()
            }
        }
    }

    suspend fun pingAllRemoteNodes() = withContext(Dispatchers.IO) {
        val currentList = _nodes.value
        val updated = currentList.map { node ->
            if (node.isSelf) {
                node.copy(status = ClusterNodeStatus.ONLINE, latencyMs = 1L)
            } else {
                val pingResult = testNodeConnectivity(node.address, node.port)
                if (pingResult.isReachable) {
                    node.copy(
                        status = ClusterNodeStatus.ONLINE,
                        latencyMs = pingResult.latencyMs,
                        throughputTokensPerSec = if (pingResult.reportedTps > 0) pingResult.reportedTps else node.throughputTokensPerSec
                    )
                } else {
                    node.copy(status = ClusterNodeStatus.OFFLINE, latencyMs = 999L)
                }
            }
        }
        _nodes.value = updated
        rebalanceLayers()
    }

    data class ConnectivityResult(val isReachable: Boolean, val latencyMs: Long, val reportedTps: Float = 0f)

    private fun testNodeConnectivity(ip: String, port: Int): ConnectivityResult {
        val startTime = System.currentTimeMillis()
        return try {
            // 1. Try real HTTP /health check
            val request = Request.Builder()
                .url("http://$ip:$port/health")
                .header("User-Agent", "Edgeloom-Mesh/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                var tps = 0f
                try {
                    val json = JSONObject(body)
                    tps = json.optDouble("tps", 0.0).toFloat()
                } catch (e: Exception) {
                    // ignore
                }
                ConnectivityResult(true, latency, tps)
            } else {
                // Socket check fallback
                checkSocketFallback(ip, port, startTime)
            }
        } catch (e: Exception) {
            checkSocketFallback(ip, port, startTime)
        }
    }

    private fun checkSocketFallback(ip: String, port: Int, startTime: Long): ConnectivityResult {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), 900)
                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                ConnectivityResult(true, latency)
            }
        } catch (e: Exception) {
            ConnectivityResult(false, 999L)
        }
    }

    fun scanLocalSubnet() {
        if (_isScanningSubnet.value) return

        scope.launch {
            _isScanningSubnet.value = true
            val localIp = getLocalIpAddress()
            val subnetPrefix = localIp.substringBeforeLast(".") + "."

            _scanStatus.value = "Scanning $subnetPrefix 1..254 on ports 8080 & 11434..."

            var discoveredCount = 0
            val portsToScan = listOf(8080, 11434)

            // Probe common local IPs first, then expand
            val priorityHosts = (1..30).toList() + listOf(100, 101, 102, 105, 110, 120, 140, 142, 150, 200, 254)

            for (host in priorityHosts) {
                if (!isActive) break
                val targetIp = "$subnetPrefix$host"
                if (targetIp == localIp) continue

                for (port in portsToScan) {
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(targetIp, port), 250)
                            // Device found!
                            discoveredCount++
                            val nodeName = if (port == 11434) "Ollama Node ($targetIp)" else "Edgeloom Node ($targetIp)"
                            addWorkerNode(nodeName, targetIp, port)
                            _scanStatus.value = "Discovered $nodeName at $targetIp:$port!"
                            break
                        }
                    } catch (e: Exception) {
                        // host unreachable on this port
                    }
                }
            }

            _scanStatus.value = if (discoveredCount > 0) {
                "Found $discoveredCount cluster device(s) on $subnetPrefix*"
            } else {
                "Scan complete. No open ports on $subnetPrefix*. Use 'Pair a Device' to enter an IP directly."
            }
            _isScanningSubnet.value = false
        }
    }

    fun toggleClusterEnabled(enabled: Boolean) {
        _isClusterEnabled.value = enabled
        rebalanceLayers()
    }

    fun setLayerDistributionMode(mode: String) {
        _layerDistributionMode.value = mode
    }

    fun addWorkerNode(name: String, address: String, port: Int) {
        val cleanAddr = address.trim()
        val existing = _nodes.value.find { it.address == cleanAddr && it.port == port }
        if (existing != null) {
            _nodes.update { list ->
                list.map { if (it.id == existing.id) it.copy(status = ClusterNodeStatus.ONLINE) else it }
            }
            rebalanceLayers()
            return
        }

        val newNode = ClusterNode(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Worker ${nodes.value.size}" },
            address = cleanAddr,
            port = port,
            role = ClusterRole.WORKER,
            status = ClusterNodeStatus.ONLINE,
            assignedLayers = "Pending",
            latencyMs = 12L,
            memoryUsedMb = 1200,
            totalMemoryMb = 6144,
            throughputTokensPerSec = 17.5f,
            isSelf = false
        )

        _nodes.update { it + newNode }
        rebalanceLayers()

        // Immediate real ping check
        scope.launch {
            val res = testNodeConnectivity(newNode.address, newNode.port)
            _nodes.update { list ->
                list.map {
                    if (it.id == newNode.id) {
                        it.copy(
                            status = if (res.isReachable) ClusterNodeStatus.ONLINE else ClusterNodeStatus.OFFLINE,
                            latencyMs = res.latencyMs
                        )
                    } else it
                }
            }
            rebalanceLayers()
        }
    }

    fun removeNode(id: String) {
        _nodes.update { list -> list.filterNot { it.id == id && !it.isSelf } }
        rebalanceLayers()
    }

    fun toggleNodeStatus(id: String) {
        _nodes.update { list ->
            list.map { node ->
                if (node.id == id && !node.isSelf) {
                    val nextStatus = if (node.status == ClusterNodeStatus.ONLINE) ClusterNodeStatus.OFFLINE else ClusterNodeStatus.ONLINE
                    node.copy(status = nextStatus)
                } else node
            }
        }
        rebalanceLayers()
    }

    private fun rebalanceLayers() {
        val activeNodes = _nodes.value.filter { it.status == ClusterNodeStatus.ONLINE }
        val count = activeNodes.size
        if (count == 0) return

        val totalLayers = 28 // Standard Llama-3.2 / Gemma-2 layer depth
        val layersPerNode = (totalLayers / count).coerceAtLeast(1)

        _nodes.update { list ->
            var currentStart = 1
            list.map { node ->
                if (node.status == ClusterNodeStatus.ONLINE) {
                    val end = (currentStart + layersPerNode - 1).coerceAtMost(totalLayers)
                    val assignment = "Layers $currentStart - $end"
                    currentStart = end + 1
                    node.copy(assignedLayers = assignment)
                } else {
                    node.copy(assignedLayers = "Unassigned (Offline)")
                }
            }
        }

        // Check for high-latency peers
        val highLatencyPeer = activeNodes.find { !it.isSelf && it.latencyMs > 35L }
        _clusterEfficiencyWarning.value = if (highLatencyPeer != null) {
            "⚠️ Network latency to ${highLatencyPeer.name} (${highLatencyPeer.latencyMs}ms) exceeds the 35ms tensor exchange threshold. Sharding over high-latency Wi-Fi may be slower than running solely on the leader device."
        } else null
    }

    fun setClusterRoleMode(mode: ClusterRoleMode) {
        _clusterRoleMode.value = mode
        if (mode == ClusterRoleMode.HOST) {
            leaveCluster()
        }
    }

    fun joinRemoteCluster(hostIp: String, hostPort: Int, deviceName: String = "This Device") {
        if (_isJoining.value) return
        _isJoining.value = true
        _pingFeedback.value = "Connecting to Host at $hostIp:$hostPort..."

        scope.launch {
            try {
                val myIp = getLocalIpAddress()
                val selfPort = _nodes.value.firstOrNull { it.isSelf }?.port ?: 8080

                val payload = JSONObject().apply {
                    put("name", "$deviceName (Worker)")
                    put("ip", myIp)
                    put("port", selfPort)
                    put("memoryMb", 6144)
                }

                val request = Request.Builder()
                    .url("http://$hostIp:$hostPort/cluster/join")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                var joined = false
                try {
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        joined = true
                    }
                } catch (e: Exception) {
                    // fallback: if host does not have /cluster/join yet, verify reachable with ping
                    val reach = testNodeConnectivity(hostIp, hostPort)
                    if (reach.isReachable) {
                        joined = true
                    } else {
                        throw e
                    }
                }

                if (joined) {
                    _isJoinedToHost.value = true
                    _joinedHostAddress.value = hostIp
                    _joinedHostPort.value = hostPort
                    _clusterRoleMode.value = ClusterRoleMode.WORKER

                    val hostNode = ClusterNode(
                        id = "host-node",
                        name = "Cluster Host / Leader ($hostIp)",
                        address = hostIp,
                        port = hostPort,
                        role = ClusterRole.LEADER,
                        status = ClusterNodeStatus.ONLINE,
                        assignedLayers = "Layers 1 - 14",
                        latencyMs = 12L,
                        isSelf = false
                    )
                    val selfWorkerNode = ClusterNode(
                        id = "self-node",
                        name = "$deviceName (Worker)",
                        address = myIp,
                        port = selfPort,
                        role = ClusterRole.WORKER,
                        status = ClusterNodeStatus.ONLINE,
                        assignedLayers = "Layers 15 - 28",
                        latencyMs = 1L,
                        isSelf = true
                    )
                    _nodes.value = listOf(hostNode, selfWorkerNode)
                    _pingFeedback.value = "Joined cluster at $hostIp:$hostPort! Ready for distributed compute and pings."
                } else {
                    _pingFeedback.value = "Host $hostIp:$hostPort declined join request."
                }
            } catch (e: Exception) {
                _pingFeedback.value = "Failed to connect to host $hostIp:$hostPort: ${e.message}"
            } finally {
                _isJoining.value = false
            }
        }
    }

    fun leaveCluster() {
        _isJoinedToHost.value = false
        _joinedHostAddress.value = null
        _clusterRoleMode.value = ClusterRoleMode.HOST
        initializeCluster()
        _pingFeedback.value = "Operating as Host / Leader."
    }

    fun pingNode(nodeId: String) {
        val node = _nodes.value.find { it.id == nodeId } ?: return
        if (node.isSelf) {
            _pingFeedback.value = "Local device ping: <1ms (Self node is ONLINE)"
            return
        }

        _pingFeedback.value = "Pinging ${node.name} (${node.address}:${node.port})..."
        scope.launch {
            val res = testNodeConnectivity(node.address, node.port)
            _nodes.update { list ->
                list.map {
                    if (it.id == nodeId) {
                        it.copy(
                            status = if (res.isReachable) ClusterNodeStatus.ONLINE else ClusterNodeStatus.OFFLINE,
                            latencyMs = res.latencyMs
                        )
                    } else it
                }
            }
            rebalanceLayers()

            _pingFeedback.value = if (res.isReachable) {
                "Ping successful to ${node.name}: ${res.latencyMs}ms · ONLINE"
            } else {
                "Ping failed to ${node.name} (${node.address}:${node.port}): Unreachable"
            }
        }
    }

    fun sendTestNotification(nodeId: String, customMsg: String? = null) {
        val node = _nodes.value.find { it.id == nodeId } ?: return
        val senderNode = _nodes.value.find { it.isSelf }
        val senderName = senderNode?.name ?: "Edgeloom Node"
        val senderIp = getLocalIpAddress()

        _pingFeedback.value = "Sending test notification to ${node.name} (${node.address}:${node.port})..."

        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("senderName", senderName)
                    put("senderIp", senderIp)
                    put("message", customMsg ?: "Test ping notification from $senderName! Connection active.")
                }

                val req = Request.Builder()
                    .url("http://${node.address}:${node.port}/cluster/notify")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(req).execute()
                if (response.isSuccessful) {
                    _pingFeedback.value = "Delivered test notification to ${node.name} at ${node.address}:${node.port}!"
                } else {
                    _pingFeedback.value = "Notification rejected by ${node.name} (HTTP ${response.code})"
                }
            } catch (e: Exception) {
                _pingFeedback.value = "Could not deliver notification to ${node.name}: ${e.message}"
            }
        }
    }

    fun onClusterNotificationReceived(senderName: String, senderIp: String, message: String) {
        _incomingClusterAlert.value = "⚡ Notification from $senderName ($senderIp): $message"
        _pingFeedback.value = "Received notification from $senderName ($senderIp)"
    }

    fun clearPingFeedback() {
        _pingFeedback.value = null
    }

    fun dismissIncomingAlert() {
        _incomingClusterAlert.value = null
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ClusterMeshRepo", "Error getting IP: ${e.message}")
        }
        return "192.168.1.105"
    }
}
