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

package org.opendc.experiments.cloudGaming.topology

/**
 * Definition of a compute cluster modeled in the simulation.
 *
 * @param id A unique identifier representing the compute cluster.
 * @param name The name of the cluster.
 * @param cpuCoresCount The total number of CPU cores in the cluster.
 * @param cpuCapacity The capacity of a CPU in the cluster in MHz.
 * @param gpuCount The total number of GPUs in the cluster.
 * @param gpuCapacity The capacity of a GPU in the cluster in MHz.
 * @param memCapacity The total memory capacity of the cluster (in MiB).
 * @param hostCount The number of hosts in the cluster.
 * @param memCapacityPerHost The memory capacity per host in the cluster (MiB).
 * @param cpuCountPerHost The number of CPU cores per host in the cluster.
 * @param cpuIdleDraw The idle power draw of the CPU.
 * @param cpuMaxDraw The max power draw of the CPU.
 * @param gpuIdleDraw The idle power draw of the GPU.
 * @param gpuMaxDraw The max power draw of the GPU.
 */
public data class ClusterSpec(
    val id: String,
    val name: String,
    val cpuCoresCount: Int,
    val cpuCapacity: Double,
    val gpuCount: Int,
    val gpuCapacity: Double,
    val memCapacity: Double,
    val hostCount: Int,
    val memCapacityPerHost: Double,
    val cpuCountPerHost: Int,
    val cpuIdleDraw: Double,
    val cpuMaxDraw: Double,
    val gpuIdleDraw: Double,
    val gpuMaxDraw: Double
)
