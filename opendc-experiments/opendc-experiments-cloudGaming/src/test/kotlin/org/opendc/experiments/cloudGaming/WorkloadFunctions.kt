package org.opendc.experiments.cloudGaming

import com.fasterxml.jackson.core.JsonToken
import org.opendc.experiments.compute.VirtualMachine
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToLong

fun parseFragments(path: File): Map<Int, FragmentBuilder> {
    val fragments = mutableMapOf<Int, FragmentBuilder>()

    val parser = factory.createParser(path)
    parser.schema = fragmentsSchema

    var id = 0
    var timestamp = 0L
    var duration = 0L
    var cpuCores = 0
    var cpuUsage = 0.0
    var gpuUsage = 0.0

    while (!parser.isClosed) {
        val token = parser.nextValue()
        if (token == JsonToken.END_OBJECT) {
            val builder = fragments.computeIfAbsent(id) { FragmentBuilder() }
            val deadlineMs = timestamp
            val timeMs = (timestamp - duration)
            builder.add(timeMs, deadlineMs, cpuUsage, gpuUsage, cpuCores)

            id = 0
            timestamp = 0L
            duration = 0
            cpuCores = 0
            cpuUsage = 0.0
            gpuUsage = 0.0

            continue
        }

        when (parser.currentName) {
            "id" -> id = parser.valueAsInt
            "timestamp" -> timestamp = parser.valueAsLong
            "duration" -> duration = parser.valueAsLong
            "cpuCores" -> cpuCores = parser.valueAsInt
            "cpuUsage" -> cpuUsage = parser.valueAsDouble
            "gpuUsage" -> gpuUsage = parser.valueAsDouble
        }
    }

    return fragments
}

fun parseMeta(path: File, fragments: Map<Int, FragmentBuilder>): List<VirtualMachine> {
    val vms = mutableListOf<VirtualMachine>()
    var counter = 0

    val parser = factory.createParser(path)
    parser.schema = metaSchema

    var id = 0
    var startTime = 0L
    var stopTime = 0L
    var cpuCores = 0
    var cpuCapacity = 0.0
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
            "gpuCapacity" -> gpuCapacity = parser.valueAsDouble
            "memCapacity" -> memCapacity = parser.valueAsDouble
        }
    }
    return vms
}
