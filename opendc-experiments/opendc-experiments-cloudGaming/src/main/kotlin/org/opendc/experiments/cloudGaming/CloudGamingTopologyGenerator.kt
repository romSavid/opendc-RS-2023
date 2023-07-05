package org.opendc.experiments.cloudGaming

import java.io.File

/**
 * Represents the graphics card configuration.
 *
 * @param coreCount The number of graphics card cores.
 * @param baseClockSpeed The base clock speed of the graphics card in MHz.
 */
data class GraphicsCard(val coreCount: Int, val baseClockSpeed: Double)
object CloudGamingTopologyGenerator {
    private val baseDir: File = File("src/test/resources/env")

    /**
     * Helper class for generating topology information.
     *
     * @param numClusters The number of clusters in the topology.
     * @param cpuCoresPerCluster The number of CPU cores per cluster.
     * @param cpuSpeed The CPU speed in GHz.
     * @param graphicsCard Represents the graphics card configuration.
     * @param graphicsCardPerCluster The number of graphics cards per cluster.
     * @param memory The memory capacity in GiB.
     * @param numHosts The number of hosts per cluster.
     * @param topologyName name of the topology file.
     */
    fun generateTopologyTxt(
        numClusters: Int,
        cpuCoresPerCluster: Int,
        cpuSpeed: Double,
        graphicsCard: GraphicsCard,
        graphicsCardPerCluster: Int,
        memory: Long,
        numHosts: Int,
        topologyName: String
    ) {
        val file = baseDir.resolve("$topologyName.txt")
        file.bufferedWriter().use { writer ->
            writer.write("ClusterID;ClusterName;cpuCores;cpuSpeed;gpuCores;gpuSpeed;Memory;numberOfHosts\n")
            for (i in 1..numClusters) {
                val clusterID = String.format("A%02d", i)
                val clusterName = clusterID
                writer.write("$clusterID;$clusterName;$cpuCoresPerCluster;$cpuSpeed;${graphicsCard.coreCount * graphicsCardPerCluster};${graphicsCard.baseClockSpeed.toDouble() / graphicsCard.coreCount};$memory;$numHosts\n")
            }
        }
    }
}
