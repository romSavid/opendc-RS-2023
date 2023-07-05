package org.opendc.experiments.cloudGaming

object ExperimentGenerator {
    private const val xcloudGameInstancesPerCluster = 100
    private const val geforceNowGameInstancesPerCluster = 160
    private const val psNowGameInstancesPerCluster = 50

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

        // Set preset attributes based on the chosen platform
        when (platform.lowercase()) {
            "xcloud" -> {
                cpuCount = 8
                gpuCount = 1
                cpuCap = 2400.0
                gpuCap = 1800.0
                memCap = 8192L
                gameInstancesPerCluster = xcloudGameInstancesPerCluster
            }
            "psnow" -> {
                cpuCount = 12
                gpuCount = 2
                cpuCap = 3000.0
                gpuCap = 2200.0
                memCap = 12288L
                gameInstancesPerCluster = psNowGameInstancesPerCluster
            }
            "geforce now" -> {
                cpuCount = 16
                gpuCount = 4
                cpuCap = 3600.0
                gpuCap = 2800.0
                memCap = 16384L
                gameInstancesPerCluster = geforceNowGameInstancesPerCluster
            }
            else -> throw IllegalArgumentException("Invalid platform: $platform")
        }

        val maxPlayers = usersPerHour.maxOrNull() ?: 0
        val numClusters = (maxPlayers + gameInstancesPerCluster - 1) / gameInstancesPerCluster

        // Generate topology file
        CloudGamingTopologyGenerator.generateTopologyTxt(
            numClusters = numClusters,
            cpuCoresPerCluster = cpuCount,
            cpuSpeed = cpuCap / cpuCount,
            graphicsCard = GraphicsCard(gpuCount, gpuCap / gpuCount),
            graphicsCardPerCluster = gpuCount,
            memory = memCap,
            numHosts = 1,
            topologyName = "$platform-topology"
        )

        // Generate trace file
        CloudGamingTraceGenerator.generateTraceCsv(
            hours = hours,
            usersPerHour = usersPerHour,
            cpuCount = cpuCount,
            cpuUsage = cpuUtilization,
            cpuCap = cpuCap,
            gpuCount = gpuCount,
            gpuUsage = gpuUtilization,
            gpuCap = gpuCap,
            memCap = memCap,
            outputDir = "$platform-trace"
        )
    }
}
