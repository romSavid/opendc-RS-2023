package org.opendc.simulator.compute.power;

/**
 * A collection {@link GpuPowerModel} implementations.
 */
public class GpuPowerModels {
    private GpuPowerModels() {}

    /**
     * Construct a constant {@link GpuPowerModel}.
     *
     * @param power The power consumption of the server at all times (in W).
     */
    public static GpuPowerModel constant(double power) {
        return new ConstantPowerModel(power);
    }

    /**
     * Construct a square root {@link GpuPowerModel} that is adapted from CloudSim.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     */
    public static GpuPowerModel sqrt(double maxPower, double idlePower) {
        return new GpuPowerModels.SqrtPowerModel(maxPower, idlePower);
    }

    /**
     * Construct a linear {@link GpuPowerModel} that is adapted from CloudSim.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     */
    public static GpuPowerModel linear(double maxPower, double idlePower) {
        return new LinearPowerModel(maxPower, idlePower);
    }

    /**
     * Construct a cubic {@link GpuPowerModel} that is adapted from CloudSim.
     *
     * @param maxPower The maximum power draw of the server in W.
     * @param idlePower The power draw of the server at its lowest utilization level in W.
     */
    public static GpuPowerModel cubic(double maxPower, double idlePower) {
        return new GpuPowerModels.CubicPowerModel(maxPower, idlePower);
    }

    private static final class ConstantPowerModel implements GpuPowerModel {
        private final double power;

        ConstantPowerModel(double power) {
            this.power = power;
        }

        @Override
        public double computePower(double utilization) {
            return power;
        }

        @Override
        public String toString() {
            return "ConstantPowerModel[power=" + power + "]";
        }
    }

    private abstract static class MaxIdlePowerModel implements GpuPowerModel {
        protected final double maxPower;
        protected final double idlePower;

        MaxIdlePowerModel(double maxPower, double idlePower) {
            this.maxPower = maxPower;
            this.idlePower = idlePower;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[max=" + maxPower + ",idle=" + idlePower + "]";
        }
    }

    private static final class SqrtPowerModel extends GpuPowerModels.MaxIdlePowerModel {
        private final double factor;

        SqrtPowerModel(double maxPower, double idlePower) {
            super(maxPower, idlePower);
            this.factor = (maxPower - idlePower) / Math.sqrt(100);
        }

        @Override
        public double computePower(double utilization) {
            return idlePower + factor * Math.sqrt(utilization * 100);
        }
    }

    private static final class LinearPowerModel extends MaxIdlePowerModel {
        private final double factor;

        LinearPowerModel(double maxPower, double idlePower) {
            super(maxPower, idlePower);
            this.factor = (maxPower - idlePower) / 100;
        }

        @Override
        public double computePower(double utilization) {
            return idlePower + factor * utilization * 100;
        }
    }

    private static final class CubicPowerModel extends GpuPowerModels.MaxIdlePowerModel {
        private final double factor;

        CubicPowerModel(double maxPower, double idlePower) {
            super(maxPower, idlePower);
            this.factor = (maxPower - idlePower) / Math.pow(100, 3);
        }

        @Override
        public double computePower(double utilization) {
            return idlePower + factor * Math.pow(utilization * 100, 3);
        }
    }
}
