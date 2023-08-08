package org.opendc.simulator.compute.model;

    import java.util.Objects;

/**
 * A Graphics card/vGPU
 */
public final class GraphicsProcessingUnit {
    private final String vendor;
    private final String modelName;
    private final String arch;
    private final double frequency;

    /**
     * Construct a {@link GraphicsProcessingUnit} instance.
     *
     * @param vendor The vendor of the storage device.
     * @param modelName The model name of the device.
     * @param arch The micro-architecture of the processor node.
     * @param frequency The clock rate of the GPU in MHz.
     */
    public GraphicsProcessingUnit(String vendor, String modelName, String arch, Double frequency) {
        this.vendor = vendor;
        this.modelName = modelName;
        this.arch = arch;
        this.frequency = frequency;
    }

    /**
     * Return the vendor of the storage device.
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Return the model name of the device.
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Return the micro-architecture of the processor node.
     */
    public String getArchitecture() {
        return arch;
    }

    /**
     * Return the capacity of the processor node.
     */
    public double getFrequency() {
        return frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphicsProcessingUnit that = (GraphicsProcessingUnit) o;
        return vendor.equals(that.vendor)
            && modelName.equals(that.modelName)
            && arch.equals(that.arch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendor, modelName, arch, frequency);
    }

    @Override
    public String toString() {
        return "GraphicsProcessingUnit[vendor='" + vendor + "',modelName='" + modelName + "',arch=" + arch + ",capacity="
            + frequency + "]";
    }
}
