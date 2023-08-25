package org.opendc.compute.service.driver.telemetry;

public record HostGpuStats(double capacity, double demand, double usage, double utilization) { }
