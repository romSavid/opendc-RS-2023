package org.opendc.experiments.cloudGaming

import java.io.File
import java.time.Instant

object CloudGamingTraceGenerator {

    val baseDir: File = File("src/test/resources/trace")

    private val traceEntries = mutableListOf<TraceEntry>()

    /**
     * Helper class for generating trace and meta CSV files.
     *
     * @param hours The number of hours the trace should have.
     * @param usersPerHour A list of the number of players per hour, should have the same number of entries as 'hours'.
     * @param cpuCount The number of CPU cores that is provisioned for each VM.
     * @param cpuUsage The cpu usage of every vm (TODO: make it more flexiable later?).
     * @param cpuCap The cpu capacity in Mhz
     * @param gpuCount The number of vGPUs that is provisioned for each VM. For now it will always be one
     * @param gpuUsage The GPU usage of every VM.
     * @param gpuCap The GPU capacity in MHz.
     * @param memCap The cpu memory capacity in MiB
     * @param outputDir Where to output the traces.
     */
    fun generateTraceCsv(
        hours: Int,
        usersPerHour: List<Int>,
        cpuCount: Int,
        cpuUsage: Double,
        cpuCap: Double,
        gpuCount: Int = 1,
        gpuUsage: Double,
        gpuCap: Double,
        memCap: Long,
        outputDir: String
    ) {
        //TODO: validate number of users is same as number of hours

        // Generate trace entries for each hour
        for (hour in 0 until hours) {
            val timestamp = Instant.now().toEpochMilli() + (hour * (3600000))
            val usersCount = usersPerHour[hour]

            // Generate trace entries for each user in the current hour
            for (userCount in 0 until usersCount) {
                val id = "${userCount + 1}"
                val entry = TraceEntry(id, timestamp, 3600000, cpuCount, cpuUsage, gpuCount, gpuUsage)
                traceEntries.add(entry)
            }
        }

        // Write trace entries to CSV file
        val file = baseDir.resolve("$outputDir/trace.csv")
        file.bufferedWriter().use { writer ->
            writer.write("id,timestamp,duration,cpuCores,cpuUsage,gpuCount,gpuUsage\n")
            for (entry in traceEntries) {
                writer.write("${entry.id},${entry.timestamp},${entry.duration},${entry.cpuCount},${entry.cpuUsage},${entry.gpuCount},${entry.gpuUsage}\n")
            }
        }
        val maxPlayers = usersPerHour.maxOrNull() ?: 0

        generateMetaCsv(maxPlayers, cpuCount, cpuCap, gpuCount, gpuCap, memCap, outputDir)
    }
    private fun generateMetaCsv(
        numVMs: Int,
        cpuCount: Int,
        cpuCap: Double,
        gpuCount: Int,
        gpuCap: Double,
        memCap: Long,
        outputDir: String
    ) {

        val metaRows = mutableListOf<MetaRow>()

        // Generate metadata rows for each VM
        for (i in 1..numVMs) {
            val id = i.toString()
            val startTime = traceEntries.first { it.id == id }.timestamp
            val stopTime = traceEntries.last { it.id == id }.timestamp.plus(3600000) // Currently the scope is by the hour, might be changed later
            val cpuCapacity = cpuCap
            val gpuCapacity = gpuCap
            val memCapacity = memCap

            metaRows.add(MetaRow(id, startTime, stopTime, cpuCount, cpuCapacity, gpuCount, gpuCapacity, memCapacity))
        }

        // Write meta rows to CSV file
        val file = baseDir.resolve("$outputDir/meta.csv")
        file.bufferedWriter().use { writer ->
            writer.write("id,startTime,stopTime,cpuCores,cpuCapacity,gpuCount,gpuCapacity,memCapacity")
            writer.newLine()

            metaRows.forEach { row ->
                writer.write(
                    "${row.id},${row.startTime},${row.stopTime}," +
                        "${row.cpuCount},${row.cpuCapacity},${row.gpuCount},${row.gpuCapacity},${row.memCapacity}"
                )
                writer.newLine()
            }
        }
    }
    private data class TraceEntry(
        val id: String,
        val timestamp: Long,
        val duration: Long,
        val cpuCount: Int,
        val cpuUsage: Double,
        val gpuCount: Int,
        val gpuUsage: Double
    )
    private data class MetaRow(
        val id: String,
        val startTime: Long,
        val stopTime: Long,
        val cpuCount: Int,
        val cpuCapacity: Double,
        val gpuCount: Int,
        val gpuCapacity: Double,
        val memCapacity: Long
    )
}
