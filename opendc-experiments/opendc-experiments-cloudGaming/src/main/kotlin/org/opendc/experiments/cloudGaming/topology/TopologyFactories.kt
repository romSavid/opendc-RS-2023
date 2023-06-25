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

import kotlinx.coroutines.flow.merge
import org.opendc.experiments.compute.topology.HostSpec
import org.opendc.simulator.compute.SimPsuFactories
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
    cpuPowerModel: CpuPowerModel = CpuPowerModels.linear(350.0, 200.0),
    gpuPowerModel: GpuPowerModel = GpuPowerModels.linear(350.0, 200.0),
    random: RandomGenerator = SplittableRandom(0)
): List<HostSpec> {
    return clusterTopology(reader.read(file), cpuPowerModel, gpuPowerModel, random)
}

/**
 * Construct a topology from the specified [input].
 */
fun clusterTopology(
    input: InputStream,
    cpuPowerModel: CpuPowerModel = CpuPowerModels.linear(350.0, 200.0),
    gpuPowerModel: GpuPowerModel = GpuPowerModels.linear(350.0, 200.0),
    random: RandomGenerator = SplittableRandom(0)
): List<HostSpec> {
    return clusterTopology(reader.read(input), cpuPowerModel, gpuPowerModel, random)
}

/**
 * Construct a topology from the given list of [clusters].
 */
fun clusterTopology(clusters: List<ClusterSpec>, cpuPowerModel: CpuPowerModel, gpuPowerModel: GpuPowerModel, random: RandomGenerator = SplittableRandom(0)): List<HostSpec> {
    return clusters.flatMap { it.toHostSpecs(random, cpuPowerModel, gpuPowerModel) }
}

/**
 * Helper method to convert a [ClusterSpec] into a list of [HostSpec]s.
 */
private fun ClusterSpec.toHostSpecs(random: RandomGenerator, cpuPowerModel: CpuPowerModel, gpuPowerModel: GpuPowerModel): List<HostSpec> {
    // TODO: maybe add CPU and GPU presets later
//    val cpuSpeed = cpuSpeed // TODO: remove? not sure why is this needed
    val memoryPerHost = memCapacityPerHost.roundToLong()

    val unknownCpuProcessingNode = ProcessingNode("unknown", "unknown", "unknown", cpuCountPerHost)
    val unknownGpuProcessingNode = ProcessingNode("unknown", "unknown", "unknown", gpuCountPerHost)
    val unknownMemoryUnit = MemoryUnit("unknown", "unknown", -1.0, memoryPerHost) // TODO: Why is the speed -1?

    val cpuCores = List(cpuCountPerHost) { coreId -> ProcessingUnit(unknownCpuProcessingNode, coreId, cpuSpeed) }
    val gpuCores = List(gpuCountPerHost) { coreId -> ProcessingUnit(unknownGpuProcessingNode, coreId, gpuSpeed) }

    val machineModel = MachineModel(
        cpuCores,
        gpuCores,
        listOf(unknownMemoryUnit)
    )

    return List(hostCount) {
        HostSpec(
            UUID(random.nextLong(), it.toLong()),
            "node-$name-$it",
            mapOf("cluster" to id),
            machineModel,
            SimPsuFactories.simpleGaming(cpuPowerModel, gpuPowerModel)
        )
    }
}
