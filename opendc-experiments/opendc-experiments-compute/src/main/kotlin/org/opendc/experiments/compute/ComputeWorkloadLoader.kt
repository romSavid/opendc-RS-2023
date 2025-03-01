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

package org.opendc.experiments.compute

import mu.KotlinLogging
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.trace.Trace
import org.opendc.trace.conv.INTERFERENCE_GROUP_MEMBERS
import org.opendc.trace.conv.INTERFERENCE_GROUP_SCORE
import org.opendc.trace.conv.INTERFERENCE_GROUP_TARGET
import org.opendc.trace.conv.RESOURCE_CPU_CAPACITY
import org.opendc.trace.conv.RESOURCE_CPU_COUNT
import org.opendc.trace.conv.RESOURCE_ID
import org.opendc.trace.conv.RESOURCE_MEM_CAPACITY
import org.opendc.trace.conv.RESOURCE_START_TIME
import org.opendc.trace.conv.RESOURCE_STATE_CPU_USAGE
import org.opendc.trace.conv.RESOURCE_STATE_DURATION
import org.opendc.trace.conv.RESOURCE_STATE_TIMESTAMP
import org.opendc.trace.conv.RESOURCE_STOP_TIME
import org.opendc.trace.conv.TABLE_INTERFERENCE_GROUPS
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import java.io.File
import java.lang.ref.SoftReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * A helper class for loading compute workload traces into memory.
 *
 * @param baseDir The directory containing the traces.
 */
public class ComputeWorkloadLoader(private val baseDir: File) {
    /**
     * The logger for this instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The cache of workloads.
     */
    private val cache = ConcurrentHashMap<String, SoftReference<List<VirtualMachine>>>()

    /**
     * Read the fragments into memory.
     */
    private fun parseFragments(trace: Trace): Map<String, Builder> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCE_STATES)).newReader()

        val idCol = reader.resolve(RESOURCE_ID)
        val timestampCol = reader.resolve(RESOURCE_STATE_TIMESTAMP)
        val durationCol = reader.resolve(RESOURCE_STATE_DURATION)
        val coresCol = reader.resolve(RESOURCE_CPU_COUNT)
        val usageCol = reader.resolve(RESOURCE_STATE_CPU_USAGE)

        val fragments = mutableMapOf<String, Builder>()

        return try {
            while (reader.nextRow()) {
                val id = reader.getString(idCol)!!
                val time = reader.getInstant(timestampCol)!!
                val duration = reader.getDuration(durationCol)!!
                val cores = reader.getInt(coresCol)
                val cpuUsage = reader.getDouble(usageCol)

                val deadlineMs = time.toEpochMilli()
                val timeMs = (time - duration).toEpochMilli()
                val builder = fragments.computeIfAbsent(id) { Builder() }
                builder.add(timeMs, deadlineMs, cpuUsage, cores)
            }

            fragments
        } finally {
            reader.close()
        }
    }

    /**
     * Read the metadata into a workload.
     */
    private fun parseMeta(trace: Trace, fragments: Map<String, Builder>, interferenceModel: VmInterferenceModel): List<VirtualMachine> {
        val reader = checkNotNull(trace.getTable(TABLE_RESOURCES)).newReader()

        val idCol = reader.resolve(RESOURCE_ID)
        val startTimeCol = reader.resolve(RESOURCE_START_TIME)
        val stopTimeCol = reader.resolve(RESOURCE_STOP_TIME)
        val cpuCountCol = reader.resolve(RESOURCE_CPU_COUNT)
        val cpuCapacityCol = reader.resolve(RESOURCE_CPU_CAPACITY)
        val memCol = reader.resolve(RESOURCE_MEM_CAPACITY)

        var counter = 0
        val entries = mutableListOf<VirtualMachine>()

        return try {
            while (reader.nextRow()) {
                val id = reader.getString(idCol)!!
                if (!fragments.containsKey(id)) {
                    continue
                }

                val submissionTime = reader.getInstant(startTimeCol)!!
                val endTime = reader.getInstant(stopTimeCol)!!
                val cpuCount = reader.getInt(cpuCountCol)
                val cpuCapacity = reader.getDouble(cpuCapacityCol)
                val memCapacity = reader.getDouble(memCol) / 1000.0 // Convert from KB to MB
                val uid = UUID.nameUUIDFromBytes("$id-${counter++}".toByteArray())

                val builder = fragments.getValue(id)
                val totalLoad = builder.totalLoad

                entries.add(
                    VirtualMachine(
                        uid,
                        id,
                        cpuCount,
                        cpuCapacity,
                        0.0,
                        memCapacity.roundToLong(),
                        totalLoad,
                        submissionTime,
                        endTime,
                        builder.build(),
                        interferenceModel.getProfile(id)
                    )
                )
            }

            // Make sure the virtual machines are ordered by start time
            entries.sortBy { it.startTime }

            entries
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            reader.close()
        }
    }

    /**
     * Read the interference model associated with the specified [trace].
     */
    private fun parseInterferenceModel(trace: Trace): VmInterferenceModel {
        val reader = checkNotNull(trace.getTable(TABLE_INTERFERENCE_GROUPS)).newReader()

        return try {
            val membersCol = reader.resolve(INTERFERENCE_GROUP_MEMBERS)
            val targetCol = reader.resolve(INTERFERENCE_GROUP_TARGET)
            val scoreCol = reader.resolve(INTERFERENCE_GROUP_SCORE)

            val modelBuilder = VmInterferenceModel.builder()

            while (reader.nextRow()) {
                val members = reader.getSet(membersCol, String::class.java)!!
                val target = reader.getDouble(targetCol)
                val score = reader.getDouble(scoreCol)

                modelBuilder
                    .addGroup(members, target, score)
            }

            modelBuilder.build()
        } finally {
            reader.close()
        }
    }

    /**
     * Load the trace with the specified [name] and [format].
     */
    public fun get(name: String, format: String): List<VirtualMachine> {
        val ref = cache.compute(name) { key, oldVal ->
            val inst = oldVal?.get()
            if (inst == null) {
                val path = baseDir.resolve(key)

                logger.info { "Loading trace $key at $path" }

                val trace = Trace.open(path, format)
                val fragments = parseFragments(trace)
                val interferenceModel = parseInterferenceModel(trace)
                val vms = parseMeta(trace, fragments, interferenceModel)

                SoftReference(vms)
            } else {
                oldVal
            }
        }

        return checkNotNull(ref?.get()) { "Memory pressure" }
    }

    /**
     * Clear the workload cache.
     */
    public fun reset() {
        cache.clear()
    }

    /**
     * A builder for a VM trace.
     */
    private class Builder {
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
         * @param usage CPU usage of this fragment.
         * @param cores Number of cores used.
         */
        fun add(timestamp: Long, deadline: Long, usage: Double, cores: Int) {
            val duration = max(0, deadline - timestamp)
            totalLoad += (usage * duration) / 1000.0 // avg MHz * duration = MFLOPs

            if (timestamp != previousDeadline) {
                // There is a gap between the previous and current fragment; fill the gap
                builder.add(timestamp, 0.0, cores)
            }

            builder.add(deadline, usage, cores)
            previousDeadline = deadline
        }

        /**
         * Build the trace.
         */
        fun build(): SimTrace = builder.build()
    }
}
