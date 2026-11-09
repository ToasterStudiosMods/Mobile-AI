package com.example

import com.example.data.model.ClusterNode
import com.example.data.model.ClusterNodeStatus
import com.example.data.model.ClusterRole
import com.example.data.model.ModelType
import com.example.data.repository.ClusterMeshRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterAndModelUnitTest {

    @Test
    fun testClusterMeshLayerSplitting() {
        val repo = ClusterMeshRepository()
        val initialNodes = repo.nodes.value

        assertTrue("Should have self node initialized", initialNodes.isNotEmpty())
        assertEquals("Self node should be leader", ClusterRole.LEADER, initialNodes.first().role)

        // Add a worker node
        repo.addWorkerNode("Galaxy S24", "192.168.1.155", 8080)
        val updatedNodes = repo.nodes.value

        assertEquals("Should have 2 nodes now", 2, updatedNodes.size)

        // Check layer partitioning across nodes
        val activeNodes = updatedNodes.filter { it.status == ClusterNodeStatus.ONLINE }
        assertTrue("Active nodes should have layer assignments", activeNodes.all { it.assignedLayers.startsWith("Layers") })
    }

    @Test
    fun testClusterNodeToggleAndRemoval() {
        val repo = ClusterMeshRepository()
        repo.addWorkerNode("Test Node", "192.168.1.200", 8080)

        val added = repo.nodes.value.find { it.name == "Test Node" }
        assertNotNull(added)

        repo.toggleNodeStatus(added!!.id)
        val toggled = repo.nodes.value.find { it.id == added.id }
        assertEquals(ClusterNodeStatus.OFFLINE, toggled?.status)

        repo.removeNode(added.id)
        val removed = repo.nodes.value.find { it.id == added.id }
        assertTrue("Node should be removed", removed == null)
    }

    @Test
    fun testWorkerRoleSwitchingAndLeave() {
        val repo = ClusterMeshRepository()
        assertEquals(com.example.data.repository.ClusterRoleMode.HOST, repo.clusterRoleMode.value)

        repo.setClusterRoleMode(com.example.data.repository.ClusterRoleMode.WORKER)
        assertEquals(com.example.data.repository.ClusterRoleMode.WORKER, repo.clusterRoleMode.value)

        repo.leaveCluster()
        assertEquals(com.example.data.repository.ClusterRoleMode.HOST, repo.clusterRoleMode.value)
        org.junit.Assert.assertFalse(repo.isJoinedToHost.value)
    }

    @Test
    fun testClusterNotificationAndFeedback() {
        val repo = ClusterMeshRepository()
        repo.onClusterNotificationReceived("Pixel 8", "192.168.1.120", "Test Ping Alert")

        val alert = repo.incomingClusterAlert.value
        assertNotNull(alert)
        assertTrue(alert!!.contains("Test Ping Alert"))
        assertTrue(alert.contains("Pixel 8"))

        repo.dismissIncomingAlert()
        org.junit.Assert.assertNull(repo.incomingClusterAlert.value)
    }
}
