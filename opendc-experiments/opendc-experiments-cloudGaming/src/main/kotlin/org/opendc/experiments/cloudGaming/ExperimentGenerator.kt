package org.opendc.experiments.cloudGaming

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
        val gpuCoresPerCard: Int
        val cpuCap: Double
        val gpuCap: Double
        val memCap: Long
        val graphicsCardPerCluster: Int
        val gameInstancesPerCluster: Int

        // Set preset attributes based on the chosen platform
        when (platform.lowercase()) {
            "xcloud" -> {
                cpuCount = 160
                gpuCoresPerCard = 3328
                graphicsCardPerCluster = 20
                cpuCap = 3.8
                gpuCap = 1825.0
                memCap = 320L
                gameInstancesPerCluster = instancesPerCluster
            }
            "psnow" -> {
                cpuCount = 160
                gpuCoresPerCard = 52060
                graphicsCardPerCluster = 20
                cpuCap = 3.5
                gpuCap = 2230.0
                memCap = 320L
                gameInstancesPerCluster = instancesPerCluster
            }
            "geforce now" -> {
                cpuCount = 160
                gpuCoresPerCard = 143360
                graphicsCardPerCluster = 40
                cpuCap = 3.5
                gpuCap = 1320.0
                memCap = 1280L
                gameInstancesPerCluster = instancesPerCluster
            }
            else -> throw IllegalArgumentException("Invalid platform: $platform")
        }

        val maxPlayers = usersPerHour.maxOrNull() ?: 0
        val numClusters = (maxPlayers + gameInstancesPerCluster - 1) / gameInstancesPerCluster
        val gpuCapPerCore = gpuCap / gpuCoresPerCard;
        val gpuCoresCountPerVm = gpuCoresPerCard * graphicsCardPerCluster / gameInstancesPerCluster

        // Generate topology file
        CloudGamingTopologyGenerator.generateTopologyTxt(
            numClusters = numClusters,
            cpuCoresPerCluster = cpuCount,
            cpuSpeed = cpuCap,
            graphicsCard = GraphicsCard(gpuCoresPerCard, gpuCapPerCore),
            graphicsCardPerCluster = graphicsCardPerCluster,
            memory = memCap,
            numHosts = 1,
            topologyName = "$platform-topology"
        )

        // Generate trace file
        CloudGamingTraceGenerator.generateTraceCsv(
            hours = hours,
            usersPerHour = usersPerHour,
            cpuCount = cpuCount / gameInstancesPerCluster,
            cpuUsage = (cpuUtilization * cpuCap * 1000) * (cpuCount / gameInstancesPerCluster), // The CPU usage multiplied by the number of cores - CPU cap is per . Converting to MHz.
            cpuCap = cpuCap,
            gpuCount = gpuCoresCountPerVm,
            gpuUsage = (gpuUtilization * gpuCapPerCore) * gpuCoresCountPerVm, // The GPU usage divided by the number of cores - GPU cap is per card
            gpuCap = gpuCap,
            memCap = memCap,
            outputDir = "$platform-trace"
        )
    }
}
