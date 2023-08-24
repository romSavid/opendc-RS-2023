/*
 * Copyright (c) 2020 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.experiments.cloudGaming

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.dataformat.csv.CsvFactory
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.experiments.cloudGaming.topology.clusterTopology
import org.opendc.experiments.compute.ComputeWorkloadLoader
import org.opendc.experiments.compute.VirtualMachine
import org.opendc.experiments.compute.registerComputeMonitor
import org.opendc.experiments.compute.replay
import org.opendc.experiments.compute.setupComputeService
import org.opendc.experiments.compute.setupHosts
import org.opendc.experiments.compute.telemetry.ComputeMonitor
import org.opendc.experiments.compute.telemetry.table.HostTableReader
import org.opendc.experiments.compute.telemetry.table.ServiceTableReader
import org.opendc.experiments.compute.topology.HostSpec
import org.opendc.experiments.provisioner.Provisioner
import org.opendc.simulator.compute.power.CpuPowerModel
import org.opendc.simulator.compute.power.CpuPowerModels
import org.opendc.simulator.compute.power.GpuPowerModel
import org.opendc.simulator.compute.power.GpuPowerModels
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.simulator.kotlin.runSimulation
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * An integration test suite for the Cloud Gaming experiments.
 */
class CloudGamingIntegrationTest {
    /**
     * The monitor used to keep track of the metrics.
     */
    private lateinit var monitor: TestComputeMonitor

    /**
     * The [FilterScheduler] to use for all experiments.
     */
    private lateinit var computeScheduler: FilterScheduler

    /**
     * The [ComputeWorkloadLoader] responsible for loading the traces.
     */
//    private lateinit var workloadLoader: ComputeWorkloadLoader

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
//        workloadLoader = ComputeWorkloadLoader(File("src/test/resources/trace")) // Maybe I should use this instead?
    }

    /**
     * Test a small simulation setup.
     */
    @Test
    fun testSmall() = runSimulation {
        val seed = 1L
        val workload = getWorkload("myTraces")
        val topology = createTopology("small")
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

    /**
     * Test a generated csv.
     */
    @Test
    fun testGenCsv() = runSimulation {

        val tracesDir = "genTraces"
//        val usersPerHour = listOf(10, 15, 12, 10)

        val usersPerHour = listOf(1777, 1693, 1560, 1406, 1242, 1106, 1045, 1011, 973, 938,
            949, 1031, 1182, 1357, 1563, 1779, 1925, 2013, 2074, 2096, 2029, 1961, 1890, 1826) //ip.inpvp // average 1517 users

        // generate new topology
        CloudGamingTopologyGenerator.generateTopologyTxt(14, 160, 3.8, 20, 1.825, 320, 5, "testTopo", 12.0, 65.0, 36.0, 200.0)
        // generate new trace
        CloudGamingTraceGenerator.generateTraceCsv(24, usersPerHour, 32, 1140.0, 3.8, 1, 7300.0, 7.3, 320, tracesDir)


//        // generate new topology
//        CloudGamingTopologyGenerator.generateTopologyTxt(14, 160, 3.8, 20, 1.825, 320, 80, "testTopo", 12.0, 65.0, 36.0, 200.0)
//        // generate new trace
//        CloudGamingTraceGenerator.generateTraceCsv(24, usersPerHour, 2, 1140.0, 3.8, 1, 456.25, 0.45625, 320, tracesDir)

        // generate new topology
//        CloudGamingTopologyGenerator.generateTopologyTxt(14, 160, 3.8, 20, 1.825, 320, 160, "testTopo", 12.0, 65.0, 36.0, 200.0)
//        // generate new trace
//        CloudGamingTraceGenerator.generateTraceCsv(24, usersPerHour, 1, 1140.0, 3.8, 1, 228.125, 0.228125, 320, tracesDir)

        val seed = 1L
        val workload = getWorkload(tracesDir)
        val topology = createTopology("testTopo")
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

    @Test
    fun testBasicRun() = runSimulation {

        val tracesDir = "geforcenow4k-trace"
//        val usersPerHour = listOf(23454, 23438, 22608, 20648, 18228, 16121, 15079, 14475, 13902, 13821,
//            14014, 14871, 16859, 18973, 21268, 23725, 25575, 26880, 28135, 28866, 28096, 26084, 24258, 23429)

        val usersPerHour = listOf(1777, 1693, 1560, 1406, 1242, 1106, 1045, 1011, 973, 938,
            949, 1031, 1182, 1357, 1563, 1779, 1925, 2013, 2074, 2096, 2029, 1961, 1890, 1826) //ip.inpvp // average 1517 users

//        val usersPerHour = listOf(160, 160, 160, 160)

//        val usersPerHour = listOf(2345, 2343, 2260, 2064, 1822, 1612, 1507, 1447, 1390, 1382,
//            1401, 1487, 1685, 1897, 2126, 2372, 2557, 2688, 2813, 2886, 2809, 2608, 2425, 2342)

        ExperimentGenerator.generateExperiment("geforcenow4k", 0.4, 0.99, 24, usersPerHour)

        val seed = 1L
        val workload = getWorkload(tracesDir)
        val topology = createTopology("geforcenow4k-topology")
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

    private val baseDir: File = File("src/test/resources/trace")

    private fun getWorkload(workloadDir: String) : List<VirtualMachine> {
        val traceFile = baseDir.resolve("$workloadDir/trace.csv")
        val metaFile = baseDir.resolve("$workloadDir/meta.csv")
        val fragments = parseFragments(traceFile) // takes traces and sticks them together (:
        return parseMeta(metaFile, fragments)
    }

    private val factory = CsvFactory()
        .enable(CsvParser.Feature.ALLOW_COMMENTS)
        .enable(CsvParser.Feature.TRIM_SPACES)

    private fun parseFragments(path: File): Map<Int, FragmentBuilder> {
        val fragments = mutableMapOf<Int, FragmentBuilder>()

        val parser = factory.createParser(path)
        parser.schema = fragmentsSchema

        var id = 0
        var timestamp = 0L
        var duration = 0L
        var cpuCores = 0
        var cpuUsage = 0.0
//        var gpuCount = 0
        var gpuUsage = 0.0

        while (!parser.isClosed) {
            val token = parser.nextValue()
            if (token == JsonToken.END_OBJECT) {
                val builder = fragments.computeIfAbsent(id) { FragmentBuilder() }
                val deadlineMs = timestamp
                val timeMs = (timestamp - duration)
                builder.add(timeMs, deadlineMs, cpuUsage, gpuUsage, cpuCores)

//                println("FRAGMENTS\n" +
//                    "id $id\n" +
//                    "timestamp $timestamp\n" +
//                    "duration $duration\n" +
//                    "cpuCores $cpuCores\n" +
//                    "cpuUsage $cpuUsage\n"+
//                    "gpuUsage $gpuUsage\n"+
//                    "timeMs $timeMs\n"+
//                    "deadlineMs $deadlineMs\n")
                id = 0
                timestamp = 0L
                duration = 0
                cpuCores = 0
                cpuUsage = 0.0
//                gpuCount = 0
                gpuUsage = 0.0

                continue
            }

            when (parser.currentName) {
                "id" -> id = parser.valueAsInt
                "timestamp" -> timestamp = parser.valueAsLong
                "duration" -> duration = parser.valueAsLong
                "cpuCores" -> cpuCores = parser.valueAsInt
                "cpuUsage" -> cpuUsage = parser.valueAsDouble
//                "gpuCount" -> gpuCount = parser.valueAsInt
                "gpuUsage" -> gpuUsage = parser.valueAsDouble
            }
        }

        return fragments
    }

    // TODO: taken from ComputeWorkloadLoader. Either use to original, or if there are too many change, move to a new location.
    private fun parseMeta(path: File, fragments: Map<Int, FragmentBuilder>): List<VirtualMachine> {
        val vms = mutableListOf<VirtualMachine>()
        var counter = 0

        val parser = factory.createParser(path)
        parser.schema = metaSchema

        var id = 0
        var startTime = 0L
        var stopTime = 0L
        var cpuCores = 0
        var cpuCapacity = 0.0
//        var gpuCount = 0
        var gpuCapacity = 0.0
        var memCapacity = 0.0

        while (!parser.isClosed) {
            val token = parser.nextValue()
            if (token == JsonToken.END_OBJECT) {
                if (!fragments.containsKey(id)) {
                    continue
                }
                val builder = fragments.getValue(id)
                val totalLoad = builder.totalLoad
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())

//                val totalCores = cpuCores + gpuCores // TODO: also here, no idea if this is a smart idea
//                val
//                println("adding VM:\n" +
//                    "UID $uid\n" +
//                    "ID $id\n" +
//                    "cpuCount $cpuCount\n" +
//                    "cpuCapacity $cpuCapacity\n" +
//                    "memCapacity $memCapacity\n" +
//                    "totalLoad $totalLoad\n" +
//                    "startTime $startTime\n" +
//                    "stopTime $stopTime\n")
                vms.add(
                    VirtualMachine(
                        uid,
                        id.toString(),
                        cpuCores,
                        cpuCapacity,
                        gpuCapacity,
                        memCapacity.roundToLong(),
                        totalLoad,
                        Instant.ofEpochMilli(startTime),
                        Instant.ofEpochMilli(stopTime),
                        builder.build(),
                        null
                    )
                )

                id = 0
                startTime = 0L
                stopTime = 0
                cpuCores = 0
                cpuCapacity = 0.0
//                gpuCount = 0
                gpuCapacity = 0.0
                memCapacity = 0.0

                continue
            }

            when (parser.currentName) {
                "id" -> id = parser.valueAsInt
                "startTime" -> startTime = parser.valueAsLong
                "stopTime" -> stopTime = parser.valueAsLong
                "cpuCores" -> cpuCores = parser.valueAsInt
                "cpuCapacity" -> cpuCapacity = parser.valueAsDouble
//                "gpuCount" -> gpuCount = parser.valueAsInt
                "gpuCapacity" -> gpuCapacity = parser.valueAsDouble
                "memCapacity" -> memCapacity = parser.valueAsDouble
            }
        }

        return vms
    }

    private class FragmentBuilder {
        /**
         * The total load of the trace.
         */
        @JvmField var totalLoad: Double = 0.0

        /**
         * The internal builder for the trace.
         */
        private val builder = SimTrace.builder()

        /**
         * The deadline of the previous fragment.
         */
        private var previousDeadline = Long.MIN_VALUE

        /**
         * Add a fragment to the trace.
         *
         * @param timestamp Timestamp at which the fragment starts (in epoch millis).
         * @param deadline Timestamp at which the fragment ends (in epoch millis).
         * @param cpuUsage CPU usage of this fragment.
         * @param gpuUsage GPU usage of this fragment.
         * @param cpuCores Number of cores used.
         */
        //TODO: change to fit my schema
        fun add(timestamp: Long, deadline: Long, cpuUsage: Double, gpuUsage: Double, cpuCores: Int) {
            val duration = max(0, deadline - timestamp)
            // TODO: This is probably not right. Not sure what is totalLoad and what are we supposed to do with it. check later.
            totalLoad += (cpuUsage * duration) / 1000.0 // avg MHz * duration = MFLOPs
            totalLoad += (gpuUsage * duration) / 1000.0 // avg MHz * duration = MFLOPs

            if (timestamp != previousDeadline) {
                // There is a gap between the previous and current fragment; fill the gap
                builder.add(timestamp, 0.0, 0.0, cpuCores)
            }

            builder.add(deadline, cpuUsage, gpuUsage, cpuCores)
            previousDeadline = deadline
        }

        /**
         * Build the trace.
         */
        fun build(): SimTrace = builder.build()
    }

    /**
     * Obtain the topology factory for the test.
     */
    private fun createTopology(name: String = "topology"): List<HostSpec> {
        val stream = checkNotNull(object {}.javaClass.getResourceAsStream("/env/$name.txt"))
        return stream.use { clusterTopology(stream) }
    }

    class TestComputeMonitor : ComputeMonitor {
        var attemptsSuccess = 0
        var attemptsFailure = 0
        var attemptsError = 0
        var serversPending = 0
        var serversActive = 0

        override fun record(reader: ServiceTableReader) {
            attemptsSuccess = reader.attemptsSuccess
            attemptsFailure = reader.attemptsFailure
            attemptsError = reader.attemptsError
            serversPending = reader.serversPending
            serversActive = reader.serversActive
        }

        var idleTime = 0L
        var activeTime = 0L
        var stealTime = 0L
        var lostTime = 0L
        var energyUsage = 0.0
        var uptime = 0L
        var downtime = 0L
        var cpuLimit = 0.0
        var cpuUsage = 0.0
        var cpuDemand = 0.0
        var cpuUtilization = 0.0
        var powerUsage = 0.0

        override fun record(reader: HostTableReader) {
            idleTime += reader.cpuIdleTime
            activeTime += reader.cpuActiveTime
            stealTime += reader.cpuStealTime
            lostTime += reader.cpuLostTime
            energyUsage += reader.powerTotal
            uptime += reader.uptime
            downtime += reader.downtime
            cpuLimit += reader.cpuLimit
            cpuUsage += reader.cpuUsage
            cpuDemand += reader.cpuDemand
            cpuUtilization += reader.cpuUtilization
            powerUsage += reader.powerUsage
        }
    }

    /**
     * The [CsvSchema] that is used to parse the trace file.
     */
    // TODO: add explanations to all of the params
    private val fragmentsSchema = CsvSchema.builder()
        .addColumn("id", CsvSchema.ColumnType.NUMBER)
        .addColumn("timestamp", CsvSchema.ColumnType.NUMBER)
        .addColumn("duration", CsvSchema.ColumnType.NUMBER)
        .addColumn("cpuCores", CsvSchema.ColumnType.NUMBER)
        .addColumn("cpuUsage", CsvSchema.ColumnType.NUMBER)
        .addColumn("gpuCores", CsvSchema.ColumnType.NUMBER)
        .addColumn("gpuUsage", CsvSchema.ColumnType.NUMBER)
        .setAllowComments(true)
        .setUseHeader(true)
        .build()

    /**
     * The [CsvSchema] that is used to parse the meta file.
     */
    // TODO: add explanations to all of the params
    private val metaSchema = CsvSchema.builder()
        .addColumn("id", CsvSchema.ColumnType.NUMBER)
        .addColumn("startTime", CsvSchema.ColumnType.NUMBER)
        .addColumn("stopTime", CsvSchema.ColumnType.NUMBER)
        .addColumn("cpuCores", CsvSchema.ColumnType.NUMBER)
        .addColumn("cpuCapacity", CsvSchema.ColumnType.NUMBER)
        .addColumn("gpuCores", CsvSchema.ColumnType.NUMBER)
        .addColumn("gpuCapacity", CsvSchema.ColumnType.NUMBER)
        .addColumn("memCapacity", CsvSchema.ColumnType.NUMBER)
        .setAllowComments(true)
        .setUseHeader(true)
        .build()

}
