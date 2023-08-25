package org.opendc.compute.service.driver.telemetry;

public record GuestGpuStats (double capacity, double demand, double usage, double utilization){ }
