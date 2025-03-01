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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
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

    @Test
    fun testBasicRun() = runSimulation {

        val tracesDir = "xcloud-trace"

        val usersPerHour = listOf(1777, 1693, 1560, 1406, 1242, 1106, 1045, 1011, 973, 938,
            949, 1031, 1182, 1357, 1563, 1779, 1925, 2013, 2074, 2096, 2029, 1961, 1890, 1826) //ip.inpvp // average 1517 users


        ExperimentGenerator.generateExperiment("xcloud", 0.3, 0.6, 24, usersPerHour)

        val seed = 1L
        val workload = getWorkload(tracesDir)
        val topology = createTopology("xcloud-topology")
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

        assertAll(
            { Assertions.assertEquals(true, monitor.attemptsSuccess == 2096, "There should be at least 5 VMs scheduled") },
            { Assertions.assertEquals(0, monitor.serversActive, "All VMs should finish after a run") },
            { Assertions.assertEquals(0, monitor.attemptsFailure, "No VM should be unscheduled") },
            { Assertions.assertEquals(0, monitor.serversPending, "No VM should not be in the queue") },
            { Assertions.assertEquals(2.46848E9, monitor.cpuLimit) { "Incorrect CPU limit" } },
            { Assertions.assertEquals(194880000000 , monitor.uptime) { "Incorrect uptime" } },
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
