package org.opendc.experiments.cloudGaming

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.experiments.cloudGaming.topology.clusterTopology
import org.opendc.experiments.compute.VirtualMachine
import org.opendc.experiments.compute.registerComputeMonitor
import org.opendc.experiments.compute.replay
import org.opendc.experiments.compute.setupComputeService
import org.opendc.experiments.compute.setupHosts
import org.opendc.experiments.compute.topology.HostSpec
import org.opendc.experiments.provisioner.Provisioner
import org.opendc.simulator.kotlin.runSimulation
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Run cloud gaming experiments
 */
class CloudGamingExperiments {
    /**
     * The monitor used to keep track of the metrics.
     */
    private lateinit var monitor: TestComputeMonitor

    /**
     * The [FilterScheduler] to use for all experiments.
     */
    private lateinit var computeScheduler: FilterScheduler

    /**
     * Set up the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        monitor = TestComputeMonitor()
        computeScheduler = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
    }

    /**
     * Run an experiment using the experiment generator
     */
    @Test
    fun testBasicRun() = runSimulation {

        // choose a platform, for an example, xcloud, psplus, geforcenow
        val platform = "xcloud"

        val tracesDir = "${platform}-trace"

        val usersPerHour = listOf(1777, 1693, 1560, 1406, 1242, 1106, 1045, 1011, 973, 938,
            949, 1031, 1182, 1357, 1563, 1779, 1925, 2013, 2074, 2096, 2029, 1961, 1890, 1826) //ip.inpvp // average 1517 users

        ExperimentGenerator.generateExperiment(platform, 0.3, 0.6, 24, usersPerHour)

        val seed = 1L
        val workload = getWorkload(tracesDir)
        val topology = createTopology("${platform}-topology")
        val monitor = monitor

        Provisioner(dispatcher, seed).use { provisioner ->
            provisioner.runSteps(
                setupComputeService(serviceDomain = "compute.opendc.org", { computeScheduler }),
                registerComputeMonitor(serviceDomain = "compute.opendc.org", monitor),
                setupHosts(serviceDomain = "compute.opendc.org", topology)
            )

            val service = provisioner.registry.resolve("compute.opendc.org", ComputeService::class.java)!!
            service.replay(timeSource, workload, seed)
        }

        // write results to a file
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        val current = LocalDateTime.now().format(formatter)
        val resultFileName = "${platform}_${current}"
        val file = baseDir.resolve("results/${resultFileName}.txt")
        file.createNewFile()
        file.bufferedWriter().use { writer ->
            writer.write("Number of VMs successfully deployed;idleTime;activeTime;stealTime;lostTime;cpuDemand;cpuLimit;energyUsage;powerUsage;cpuUtilization\n")
            writer.write("${monitor.attemptsSuccess};${monitor.idleTime};${monitor.activeTime};${monitor.stealTime};${monitor.lostTime};${monitor.cpuDemand};${monitor.cpuLimit};${monitor.energyUsage};${monitor.powerUsage};${monitor.cpuUtilization}\n")
        }

        println(
            "Scheduler " +
                "Success=${monitor.attemptsSuccess} " +
                "Failure=${monitor.attemptsFailure} " +
                "Error=${monitor.attemptsError} " +
                "Pending=${monitor.serversPending} " +
                "Active=${monitor.serversActive}"
        )

        println(
            "Results:" +
                "idleTime=${monitor.idleTime} " +
                "activeTime=${monitor.activeTime} " +
                "stealTime=${monitor.stealTime} " +
                "lostTime=${monitor.lostTime} " +
                "energyUse=${monitor.energyUsage} " +
                "upTime=${monitor.uptime}"
        )

        println(
            "Results:" +
                "downTime=${monitor.downtime} " +
                "cpuLimit=${monitor.cpuLimit} " +
                "cpuUsage=${monitor.cpuUsage} " +
                "cpuDemand=${monitor.cpuDemand} " +
                "cpuUtilization=${monitor.cpuUtilization} " +
                "powerUsage=${monitor.powerUsage}"
        )
    }
    private fun getWorkload(workloadDir: String) : List<VirtualMachine> {
        val traceFile = baseDir.resolve("trace/$workloadDir/trace.csv")
        val metaFile = baseDir.resolve("trace/$workloadDir/meta.csv")
        val fragments = parseFragments(traceFile) // takes traces and sticks them together (:
        return parseMeta(metaFile, fragments)
    }

    /**
     * Obtain the topology factory for the test.
     */
    private fun createTopology(name: String = "topology"): List<HostSpec> {
        val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/env/$name.txt"))
        return stream.use { clusterTopology(stream) }
    }
}
