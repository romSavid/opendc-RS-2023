/*
 * Copyright (c) 2021 AtLarge Research
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

@file:JvmName("TopologyFactories")

package org.opendc.experiments.cloudGaming.topology

import org.opendc.experiments.compute.topology.HostSpec
import org.opendc.simulator.compute.SimPsuFactories
import org.opendc.simulator.compute.model.GraphicsProcessingUnit
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.CpuPowerModel
import org.opendc.simulator.compute.power.CpuPowerModels
import org.opendc.simulator.compute.power.GpuPowerModel
import org.opendc.simulator.compute.power.GpuPowerModels
import java.io.File
import java.io.InputStream
import java.util.SplittableRandom
import java.util.UUID
import java.util.random.RandomGenerator
import kotlin.math.roundToLong

/**
 * A [ClusterSpecReader] that is used to read the cluster definition file.
 */
private val reader = ClusterSpecReader()

/**
 * Construct a topology from the specified [file].
 */
fun clusterTopology(
    file: File,
    random: RandomGenerator = SplittableRandom(0)
): List<HostSpec> {
    return clusterTopology(reader.read(file), random)
}

/**
 * Construct a topology from the specified [input].
 */
fun clusterTopology(
    input: InputStream,
    random: RandomGenerator = SplittableRandom(0)
): List<HostSpec> {
    return clusterTopology(reader.read(input), random)
}

/**
 * Construct a topology from the given list of [clusters].
 */
fun clusterTopology(clusters: List<ClusterSpec>, random: RandomGenerator = SplittableRandom(0)): List<HostSpec> {
    return clusters.flatMap { it.toHostSpecs(random) }
}

/**
 * Helper method to convert a [ClusterSpec] into a list of [HostSpec]s.
 */
private fun ClusterSpec.toHostSpecs(random: RandomGenerator): List<HostSpec> {
    // TODO: maybe add CPU and GPU presets later
//    val cpuSpeed = cpuSpeed // TODO: remove? not sure why is this needed
    val memoryPerHost = memCapacityPerHost.roundToLong()

    val unknownCpuProcessingNode = ProcessingNode("unknown", "unknown", "unknown", cpuCountPerHost)
    val virtualGpuCapacity = (gpuCapacity * gpuCount) / hostCount;
    val unknownMemoryUnit = MemoryUnit("unknown", "unknown", -1.0, memoryPerHost) // TODO: Why is the speed -1?

    val cpuCores = List(cpuCountPerHost) { coreId -> ProcessingUnit(unknownCpuProcessingNode, coreId, cpuCapacity) }
    val vGpus = List(1) { gpuId -> GraphicsProcessingUnit("unknown", "unknown", "unknown", virtualGpuCapacity) } // TODO: currently we added support for one vGPU, that could be changed later

    val machineModel = MachineModel(
        cpuCores,
        vGpus,
        listOf(unknownMemoryUnit)
    )

    return List(hostCount) {
        HostSpec(
            UUID(random.nextLong(), it.toLong()),
            "node-$name-$it",
            mapOf("cluster" to id),
            machineModel,
            SimPsuFactories.simpleGaming(CpuPowerModels.linear(cpuMaxDraw, cpuIdleDraw), GpuPowerModels.linear(gpuMaxDraw, gpuIdleDraw))
        )
    }
}
