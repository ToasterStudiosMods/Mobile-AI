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

        assertEquals("Should have 3 nodes now", 3, updatedNodes.size)

        // Check layer partitioning across nodes
        val activeNodes = updatedNodes.filter { it.status == ClusterNodeStatus.ONLINE }
        assertTrue("All 3 nodes should have layer assignments", activeNodes.all { it.assignedLayers.startsWith("Layers") })
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
}
