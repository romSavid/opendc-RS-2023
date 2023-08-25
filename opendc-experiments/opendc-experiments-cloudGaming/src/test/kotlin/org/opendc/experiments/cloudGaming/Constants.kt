package org.opendc.experiments.cloudGaming

import com.fasterxml.jackson.dataformat.csv.CsvFactory
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.File

val baseDir: File = File("src/test/resources")

val factory = CsvFactory()
    .enable(CsvParser.Feature.ALLOW_COMMENTS)
    .enable(CsvParser.Feature.TRIM_SPACES)

/**
 * The [CsvSchema] that is used to parse the trace file.
 */
val fragmentsSchema = CsvSchema.builder()
    .addColumn("id", CsvSchema.ColumnType.NUMBER)
    .addColumn("timestamp", CsvSchema.ColumnType.NUMBER)
    .addColumn("duration", CsvSchema.ColumnType.NUMBER)
    .addColumn("cpuCores", CsvSchema.ColumnType.NUMBER)
    .addColumn("cpuUsage", CsvSchema.ColumnType.NUMBER)
    .addColumn("gpuCount", CsvSchema.ColumnType.NUMBER)
    .addColumn("gpuUsage", CsvSchema.ColumnType.NUMBER)
    .setAllowComments(true)
    .setUseHeader(true)
    .build()

/**
 * The [CsvSchema] that is used to parse the meta file.
 */
val metaSchema = CsvSchema.builder()
    .addColumn("id", CsvSchema.ColumnType.NUMBER)
    .addColumn("startTime", CsvSchema.ColumnType.NUMBER)
    .addColumn("stopTime", CsvSchema.ColumnType.NUMBER)
    .addColumn("cpuCores", CsvSchema.ColumnType.NUMBER)
    .addColumn("cpuCapacity", CsvSchema.ColumnType.NUMBER)
    .addColumn("gpuCount", CsvSchema.ColumnType.NUMBER)
    .addColumn("gpuCapacity", CsvSchema.ColumnType.NUMBER)
    .addColumn("memCapacity", CsvSchema.ColumnType.NUMBER)
    .setAllowComments(true)
    .setUseHeader(true)
    .build()
