package org.opendc.simulator.compute.power;

import org.opendc.simulator.compute.SimMachine;

/**
 * A model for estimating the power usage of a {@link SimMachine} based on the GPU usage.
 */
public interface GpuPowerModel {
    /**
     * Computes GPU power consumption for each host.
     *
     * @param utilization The GPU utilization percentage.
     * @return A double value of GPU power consumption (in W).
     */
    double computePower(double utilization);
}
