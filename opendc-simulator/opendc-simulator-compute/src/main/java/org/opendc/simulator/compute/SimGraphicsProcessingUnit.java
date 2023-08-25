package org.opendc.simulator.compute;

    import org.opendc.simulator.compute.model.GraphicsProcessingUnit;
    import org.opendc.simulator.flow2.sink.FlowSink;

/**
 * A simulated graphics processing unit.
 */
public interface SimGraphicsProcessingUnit extends FlowSink {
    /**
     * Return the base clock frequency of the processing unit (in MHz).
     */
    double getFrequency();

    /**
     * Adjust the base clock frequency of the graphics processing unit.
     *
     * <p>
     * The GPU may or may not round the new frequency to one of its pre-defined frequency steps.
     *
     * @param frequency The new frequency to set the clock of the processing unit to.
     * @throws UnsupportedOperationException if the base clock cannot be adjusted.
     */
    void setFrequency(double frequency);

    /**
     * The demand on the processing unit.
     */
    double getDemand();

    /**
     * The speed of the processing unit.
     */
    double getSpeed();

    /**
     *  The model representing the static properties of the processing unit.
     */
    GraphicsProcessingUnit getModel();
}
