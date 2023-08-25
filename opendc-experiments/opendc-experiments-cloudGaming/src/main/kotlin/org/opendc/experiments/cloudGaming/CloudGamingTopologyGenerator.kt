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
     * @param graphicsCardPerCluster The number of graphics cards per cluster.
     * @param gpuSpeed The GPU speed in GHz.
     * @param memory The memory capacity in GiB.
     * @param numHosts The number of hosts per cluster.
     * @param topologyName name of the topology file.
     * @param cpuIdleDraw The idle power draw of the CPU.
     * @param cpuMaxDraw The max power draw of the CPU.
     * @param gpuIdleDraw The idle power draw of the GPU.
     * @param gpuMaxDraw The max power draw of the GPU.
     */
    fun generateTopologyTxt(
        numClusters: Int,
        cpuCoresPerCluster: Int,
        cpuSpeed: Double,
        graphicsCardPerCluster: Int,
        gpuSpeed: Double,
        memory: Long,
        numHosts: Int,
        topologyName: String,
        cpuIdleDraw: Double,
        cpuMaxDraw: Double,
        gpuIdleDraw: Double,
        gpuMaxDraw: Double
    ) {
        val file = baseDir.resolve("$topologyName.txt")
        file.bufferedWriter().use { writer ->
            writer.write("ClusterID;ClusterName;cpuCores;cpuSpeed;gpuCount;gpuSpeed;Memory;numberOfHosts;cpuIdleDraw;cpuMaxDraw;gpuIdleDraw;gpuMaxDraw\n")
            for (i in 1..numClusters) {
                val clusterID = String.format("A%02d", i)
                val clusterName = clusterID
                writer.write("$clusterID;$clusterName;$cpuCoresPerCluster;$cpuSpeed;$graphicsCardPerCluster;$gpuSpeed;$memory;$numHosts;$cpuIdleDraw;$cpuMaxDraw;$gpuIdleDraw;$gpuMaxDraw\n")
            }
        }
    }
}
