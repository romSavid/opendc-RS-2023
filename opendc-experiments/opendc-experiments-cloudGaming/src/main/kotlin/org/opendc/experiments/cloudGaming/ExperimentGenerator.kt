package org.opendc.experiments.cloudGaming

import kotlin.math.ceil

object ExperimentGenerator {
    private const val instancesPerCluster = 160

    /**
     * Generates topology and traces for an experiment
     *
     * @param platform The name of the platform.
     * @param cpuUtilization The percentage of a CPU core that is utilized.
     * @param gpuUtilization The percentage of a GPU core that is utilized.
     * @param hours For how long does the trace run.
     * @param usersPerHour A list of the user count per hour.
     */
    fun generateExperiment(
        platform: String,
        cpuUtilization: Double,
        gpuUtilization: Double,
        hours: Int,
        usersPerHour: List<Int>
    ) {
        val cpuCount: Int
        val gpuCount: Int
        val cpuCap: Double
        val gpuCap: Double
        val memCap: Long
        val gameInstancesPerCluster: Int
        val cpuIdleDraw: Double
        val cpuMaxDraw: Double
        val gpuIdleDraw: Double
        val gpuMaxDraw: Double

        // Set preset attributes based on the chosen platform
        when (platform.lowercase()) {
            "xcloud" -> {
                cpuCount = 160
                gpuCount = 20
                cpuCap = 3.8
                gpuCap = 1.825
                memCap = 320L
                gameInstancesPerCluster = instancesPerCluster
                cpuIdleDraw = 12.0
                cpuMaxDraw = 65.0
                gpuIdleDraw = 36.0
                gpuMaxDraw = 200.0
            }
            "psplus" -> {
                cpuCount = 160
                gpuCount = 20
                cpuCap = 3.5
                gpuCap = 2.23
                memCap = 320L
                gameInstancesPerCluster = instancesPerCluster
                cpuIdleDraw = 12.0
                cpuMaxDraw = 65.0
                gpuIdleDraw = 33.0
                gpuMaxDraw = 180.0
            }
            "geforcenow" -> {
                cpuCount = 160
                gpuCount = 40
                cpuCap = 3.5
                gpuCap = 1.32
                memCap = 1280L
                gameInstancesPerCluster = instancesPerCluster
                cpuIdleDraw = 23.0
                cpuMaxDraw = 125.0
                gpuIdleDraw = 27.0
                gpuMaxDraw = 150.0
            }
            "geforcenow4k" -> {
                cpuCount = 160
                gpuCount = 40
                cpuCap = 3.5
                gpuCap = 1.5
                memCap = 1280L
                gameInstancesPerCluster = 40
                cpuIdleDraw = 23.0
                cpuMaxDraw = 125.0
                gpuIdleDraw = 40.0
                gpuMaxDraw = 220.0
            }
            else -> throw IllegalArgumentException("Invalid platform: $platform")
        }

        val maxPlayers = usersPerHour.maxOrNull() ?: 0
//        val numClusters = (maxPlayers + gameInstancesPerCluster - 1) / gameInstancesPerCluster
        val numClusters = ceil(maxPlayers.toDouble() / gameInstancesPerCluster).toInt()
        val partitionedGpuCap = (gpuCap * gpuCount) / gameInstancesPerCluster // the overall gpu capacity divided by the number of hosts

        // Generate topology file
        CloudGamingTopologyGenerator.generateTopologyTxt(
            numClusters = numClusters,
            cpuCoresPerCluster = cpuCount,
            cpuSpeed = cpuCap,
            graphicsCardPerCluster = gpuCount,
            gpuSpeed = gpuCap,
            memory = memCap,
            numHosts = gameInstancesPerCluster,
            topologyName = "$platform-topology",
            cpuIdleDraw = cpuIdleDraw,
            cpuMaxDraw = cpuMaxDraw,
            gpuIdleDraw = gpuIdleDraw,
            gpuMaxDraw = gpuMaxDraw
        )

        // Generate trace file
        CloudGamingTraceGenerator.generateTraceCsv(
            hours = hours,
            usersPerHour = usersPerHour,
            cpuCount = cpuCount / gameInstancesPerCluster,
            cpuUsage = (cpuUtilization * cpuCap * 1000) * (cpuCount / gameInstancesPerCluster), // The CPU usage multiplied by the number of cores - CPU cap is per core. Converting to MHz.
            cpuCap = cpuCap,
            gpuCount = 1, // Currently we support one vGPU per host. But it is possible to change that later
            gpuUsage = (gpuUtilization * partitionedGpuCap * 1000) * 1, // The GPU usage divided by the number of GPUs - GPU cap is per card. The 1 is left there for clarity.
            gpuCap = partitionedGpuCap,
            memCap = memCap,
            outputDir = "$platform-trace"
        )
    }
}
