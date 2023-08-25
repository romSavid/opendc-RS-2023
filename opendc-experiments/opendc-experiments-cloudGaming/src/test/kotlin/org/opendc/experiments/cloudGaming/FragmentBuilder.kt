package org.opendc.experiments.cloudGaming

import org.opendc.simulator.compute.workload.SimTrace
import kotlin.math.max

class FragmentBuilder {
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
    fun add(timestamp: Long, deadline: Long, cpuUsage: Double, gpuUsage: Double, cpuCores: Int) {
        val duration = max(0, deadline - timestamp)

        totalLoad += (cpuUsage * duration) / 1000.0 // avg MHz * duration = MFLOPs
        totalLoad += (gpuUsage * duration) / 1000.0

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
