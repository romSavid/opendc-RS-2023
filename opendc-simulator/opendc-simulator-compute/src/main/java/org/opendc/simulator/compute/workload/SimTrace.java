/*
 * Copyright (c) 2022 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.simulator.compute.workload;

import java.util.Arrays;
import java.util.List;

import org.opendc.simulator.compute.SimGraphicsProcessingUnit;
import org.opendc.simulator.compute.SimMachineContext;
import org.opendc.simulator.compute.SimProcessingUnit;
import org.opendc.simulator.flow2.FlowGraph;
import org.opendc.simulator.flow2.FlowStage;
import org.opendc.simulator.flow2.FlowStageLogic;
import org.opendc.simulator.flow2.OutPort;

/**
 * A workload trace that describes the resource utilization over time in a collection of {@link SimTraceFragment}s.
 */
public final class SimTrace {
    private final double[] cpuUsageCol;
    private final double[] gpuUsageCol;
    private final long[] deadlineCol;
    private final int[] cpuCoresCol;
    private final int size;

    /**
     * Construct a {@link SimTrace} instance.
     *
     * @param cpuUsageCol The column containing the CPU usage of each fragment (in MHz).
     * @param gpuUsageCol The column containing the GPU usage of each fragment (in MHz).
     * @param deadlineCol The column containing the ending timestamp for each fragment (in epoch millis).
     * @param cpuCoresCol The column containing the utilized CPU cores.
     * @param size The number of fragments in the trace.
     */
    private SimTrace(double[] cpuUsageCol, double[] gpuUsageCol, long[] deadlineCol, int[] cpuCoresCol, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Invalid trace size");
        } else if (cpuUsageCol.length < size) {
            throw new IllegalArgumentException("Invalid number of cpu usage entries");
        } else if (gpuUsageCol.length < size) {
            throw new IllegalArgumentException("Invalid number of gpu usage entries");
        } else if (deadlineCol.length < size) {
            throw new IllegalArgumentException("Invalid number of deadline entries");
        } else if (cpuCoresCol.length < size) {
            throw new IllegalArgumentException("Invalid number of cpu core entries");
        }

        this.cpuUsageCol = cpuUsageCol;
        this.gpuUsageCol = gpuUsageCol;
        this.deadlineCol = deadlineCol;
        this.cpuCoresCol = cpuCoresCol;
        this.size = size;
    }

    /**
     * Construct a {@link SimTrace} instance.
     *
     * @param cpuUsageCol The column containing the CPU usage of each fragment (in MHz).
     * @param deadlineCol The column containing the ending timestamp for each fragment (in epoch millis).
     * @param cpuCoresCol The column containing the utilized CPU cores.
     * @param size The number of fragments in the trace.
     */
    private SimTrace(double[] cpuUsageCol, long[] deadlineCol, int[] cpuCoresCol, int size) {
        double[] gpuUsage = new double[size];
        Arrays.fill(gpuUsage, 0.0);

        if (size < 0) {
            throw new IllegalArgumentException("Invalid trace size");
        } else if (cpuUsageCol.length < size) {
            throw new IllegalArgumentException("Invalid number of usage entries");
        } else if (deadlineCol.length < size) {
            throw new IllegalArgumentException("Invalid number of deadline entries");
        } else if (cpuCoresCol.length < size) {
            throw new IllegalArgumentException("Invalid number of core entries");
        }

        this.cpuUsageCol = cpuUsageCol;
        this.gpuUsageCol = gpuUsage;
        this.deadlineCol = deadlineCol;
        this.cpuCoresCol = cpuCoresCol;
        this.size = size;
    }

    /**
     * Construct a {@link SimWorkload} for this trace.
     *
     * @param offset The offset for the timestamps.
     */
    public SimWorkload createWorkload(long offset) {
        return new Workload(offset, cpuUsageCol, gpuUsageCol, deadlineCol, cpuCoresCol, size, 0);
    }

    /**
     * Create a new {@link Builder} instance with the specified initial capacity.
     */
    public static Builder builder(int initialCapacity) {
        return new Builder(initialCapacity);
    }

    /**
     * Create a new {@link Builder} instance with a default initial capacity.
     */
    public static Builder builder() {
        return builder(256);
    }

    /**
     * Construct a {@link SimTrace} from the specified fragments.
     *
     * @param fragments The array of fragments to construct the trace from.
     */
    public static SimTrace ofFragments(SimTraceFragment... fragments) {
        final Builder builder = builder(fragments.length);

        for (SimTraceFragment fragment : fragments) {
            builder.add(fragment.timestamp + fragment.duration, fragment.cpuUsage, fragment.gpuUsage, fragment.cpuCores);
        }

        return builder.build();
    }

    /**
     * Construct a {@link SimTrace} from the specified fragments.
     *
     * @param fragments The fragments to construct the trace from.
     */
    public static SimTrace ofFragments(List<SimTraceFragment> fragments) {
        final Builder builder = builder(fragments.size());

        for (SimTraceFragment fragment : fragments) {
            builder.add(fragment.timestamp + fragment.duration, fragment.cpuUsage, fragment.gpuUsage, fragment.cpuCores);
        }

        return builder.build();
    }

    /**
     * Builder class for a {@link SimTrace}.
     */
    public static final class Builder {
        private double[] cpuUsageCol;
        private double[] gpuUsageCol;
        private long[] deadlineCol;
        private int[] cpuCoresCol;

        private int size;
        private boolean isBuilt;

        /**
         * Construct a new {@link Builder} instance.
         */
        private Builder(int initialCapacity) {
            this.cpuUsageCol = new double[initialCapacity];
            this.gpuUsageCol = new double[initialCapacity];
            this.deadlineCol = new long[initialCapacity];
            this.cpuCoresCol = new int[initialCapacity];
        }

        /**
         * Add a fragment to the trace.
         *
         * @param deadline The timestamp at which the fragment ends (in epoch millis).
         * @param cpuUsage The CPU usage at this fragment.
         * @param gpuUsage The GPU usage at this fragment.
         * @param cpuCores The number of CPU cores used during this fragment.
         */
        public void add(long deadline, double cpuUsage, double gpuUsage, int cpuCores) {
            if (isBuilt) {
                recreate();
            }

            int size = this.size;
            double[] cpuUsageCol = this.cpuUsageCol;
            double[] gpuUsageCol = this.gpuUsageCol;

            if (size == cpuUsageCol.length) {
                grow();
                cpuUsageCol = this.cpuUsageCol;
                gpuUsageCol = this.gpuUsageCol;
            }

            deadlineCol[size] = deadline;
            cpuUsageCol[size] = cpuUsage;
            gpuUsageCol[size] = gpuUsage;
            cpuCoresCol[size] = cpuCores;

            this.size++;
        }

        /**
         * Add a fragment to the trace.
         * @param deadline The timestamp at which the fragment ends (in epoch millis).
         * @param cpuUsage The CPU usage at this fragment.
         * @param cpuCores The number of CPU cores used during this fragment.
         */
        public void add(long deadline, double cpuUsage, int cpuCores) {
            if (isBuilt) {
                recreate();
            }

            int size = this.size;
            double[] cpuUsageCol = this.cpuUsageCol;
            double[] gpuUsageCol = this.gpuUsageCol;

            if (size == cpuUsageCol.length) {
                grow();
                cpuUsageCol = this.cpuUsageCol;
                gpuUsageCol = this.gpuUsageCol;
            }

            deadlineCol[size] = deadline;
            cpuUsageCol[size] = cpuUsage;
            gpuUsageCol[size] = 0.0;
            cpuCoresCol[size] = cpuCores;

            this.size++;
        }

        /**
         * Build the {@link SimTrace} instance.
         */
        public SimTrace build() {
            isBuilt = true;
            return new SimTrace(cpuUsageCol, gpuUsageCol, deadlineCol, cpuCoresCol, size);
        }

        /**
         * Helper method to grow the capacity of the trace.
         */
        private void grow() {
            int arraySize = cpuUsageCol.length;
            int newSize = arraySize + (arraySize >> 1);

            cpuCoresCol = Arrays.copyOf(cpuCoresCol, newSize);
            deadlineCol = Arrays.copyOf(deadlineCol, newSize);
            cpuUsageCol = Arrays.copyOf(cpuUsageCol, newSize);
            gpuUsageCol = Arrays.copyOf(gpuUsageCol, newSize);
        }

        /**
         * Clone the columns of the trace.
         *
         * <p>
         * This is necessary when a {@link SimTrace} has been built already, but the user is again adding entries to
         * the builder.
         */
        private void recreate() {
            isBuilt = false;
            cpuUsageCol = cpuUsageCol.clone();
            gpuUsageCol = gpuUsageCol.clone();
            deadlineCol = deadlineCol.clone();
            cpuCoresCol = cpuCoresCol.clone();
        }
    }

    /**
     * Implementation of {@link SimWorkload} that executes a trace.
     */
    private static class Workload implements SimWorkload {
        private WorkloadStageLogic logic;

        private final long offset;
        private final double[] cpuUsageCol;
        private final double[] gpuUsageCol;
        private final long[] deadlineCol;
        private final int[] cpuCoresCol;
        private final int size;
        private final int index;

        private Workload(
            long offset, double[] cpuUsageCol, double[] gpuUsageCol,
            long[] deadlineCol, int[] cpuCoresCol, int size, int index) {

            this.offset = offset;
            this.cpuUsageCol = cpuUsageCol;
            this.gpuUsageCol = gpuUsageCol;
            this.deadlineCol = deadlineCol;
            this.cpuCoresCol = cpuCoresCol;
            this.size = size;
            this.index = index;
        }

        private Workload(long offset, double[] cpuUsageCol, long[] deadlineCol, int[] cpuCoresCol, int size, int index) {
            double[] gpuUsage = new double[size];
            Arrays.fill(gpuUsage, 0.0);

            this.offset = offset;
            this.cpuUsageCol = cpuUsageCol;
            this.gpuUsageCol = gpuUsage;
            this.deadlineCol = deadlineCol;
            this.cpuCoresCol = cpuCoresCol;
            this.size = size;
            this.index = index;
        }

        @Override
        public void onStart(SimMachineContext ctx) {
            final WorkloadStageLogic logic;
            if (ctx.getCpus().size() == 1 && ctx.getGpus().size() == 0) {
                logic = new SingleWorkloadLogic(ctx, offset, cpuUsageCol, deadlineCol, size, index);
            } else {
                logic = new MultiWorkloadLogic(ctx, offset, cpuUsageCol, gpuUsageCol, deadlineCol, cpuCoresCol, size, index);
            }
            this.logic = logic;
        }

        @Override
        public void onStop(SimMachineContext ctx) {
            final WorkloadStageLogic logic = this.logic;

            if (logic != null) {
                this.logic = null;
                logic.getStage().close();
            }
        }

        @Override
        public SimWorkload snapshot() {
            final WorkloadStageLogic logic = this.logic;
            int index = this.index;

            if (logic != null) {
                index = logic.getIndex();
            }

            return new Workload(offset, cpuUsageCol, gpuUsageCol, deadlineCol, cpuCoresCol, size, index);
        }
    }

    /**
     * Interface to represent the {@link FlowStage} that simulates the trace workload.
     */
    private interface WorkloadStageLogic extends FlowStageLogic {
        /**
         * Return the {@link FlowStage} belonging to this instance.
         */
        FlowStage getStage();

        /**
         * Return the current index of the workload.
         */
        int getIndex();
    }

    /**
     * Implementation of {@link FlowStageLogic} for just a single CPU resource.
     */
    private static class SingleWorkloadLogic implements WorkloadStageLogic {
        private final FlowStage stage;
        private final OutPort output;
        private int index;

        private final long offset;
        private final double[] usageCol;
        private final long[] deadlineCol;
        private final int size;

        private final SimMachineContext ctx;

        private SingleWorkloadLogic(
            SimMachineContext ctx, long offset, double[] usageCol, long[] deadlineCol, int size, int index) {
            this.ctx = ctx;
            this.offset = offset;
            this.usageCol = usageCol;
            this.deadlineCol = deadlineCol;
            this.size = size;
            this.index = index;

            final FlowGraph graph = ctx.getGraph();
            final List<? extends SimProcessingUnit> cpus = ctx.getCpus();

            stage = graph.newStage(this);

            final SimProcessingUnit cpu = cpus.get(0);
            final OutPort output = stage.getOutlet("cpu");
            this.output = output;

            graph.connect(output, cpu.getInput());
        }

        @Override
        public long onUpdate(FlowStage ctx, long now) {
            int size = this.size;
            long offset = this.offset;
            long nowOffset = now - offset;

            int index = this.index;

            long[] deadlines = deadlineCol;
            long deadline = deadlines[index];

            while (deadline <= nowOffset) {
                if (++index >= size) {
                    return doStop(ctx);
                }
                deadline = deadlines[index];
            }

            this.index = index;
            this.output.push((float) usageCol[index]);
            return deadline + offset;
        }

        @Override
        public FlowStage getStage() {
            return stage;
        }

        @Override
        public int getIndex() {
            return index;
        }

        /**
         * Helper method to stop the execution of the workload.
         */
        private long doStop(FlowStage ctx) {
            final SimMachineContext machineContext = this.ctx;
            if (machineContext != null) {
                machineContext.shutdown();
            }
            ctx.close();
            return Long.MAX_VALUE;
        }
    }

    /**
     * Implementation of {@link FlowStageLogic} for multiple CPUs.
     */
    private static class MultiWorkloadLogic implements WorkloadStageLogic {
        private final FlowStage stage;
        private final OutPort[] cpuOutputs;
        private final OutPort[] gpuOutputs;
        private int index;
        private final int cpuCoreCount;
        private final int gpuCount;

        private final long offset;
        private final double[] cpuUsageCol;
        private final double[] gpuUsageCol;
        private final long[] deadlineCol;
        private final int[] cpuCoresCol;
        private final int size;

        private final SimMachineContext ctx;

        private MultiWorkloadLogic(
            SimMachineContext ctx,
            long offset,
            double[] cpuUsageCol,
            double[] gpuUsageCol,
            long[] deadlineCol,
            int[] cpuCoresCol,
            int size,
            int index) {
            this.ctx = ctx;
            this.offset = offset;
            this.cpuUsageCol = cpuUsageCol;
            this.gpuUsageCol = gpuUsageCol;
            this.deadlineCol = deadlineCol;
            this.cpuCoresCol = cpuCoresCol;
            this.size = size;
            this.index = index;

            final FlowGraph graph = ctx.getGraph();
            final List<? extends SimProcessingUnit> cpus = ctx.getCpus();
            final List<? extends SimGraphicsProcessingUnit> gpus = ctx.getGpus();

            stage = graph.newStage(this);
            cpuCoreCount = cpus.size();
            gpuCount = gpus.size();

            final OutPort[] cpuOutputs = new OutPort[cpus.size()];
            final OutPort[] gpuOutputs = new OutPort[gpus.size()];
            this.cpuOutputs = cpuOutputs;
            this.gpuOutputs = gpuOutputs;

            for (int i = 0; i < cpus.size(); i++) {
                final SimProcessingUnit cpu = cpus.get(i);
                final OutPort output = stage.getOutlet("cpu" + i);

                graph.connect(output, cpu.getInput());
                cpuOutputs[i] = output;
            }

            for (int i = 0; i < gpus.size(); i++) {
                final SimGraphicsProcessingUnit gpu = gpus.get(i);
                final OutPort output = stage.getOutlet("gpu" + i);

                graph.connect(output, gpu.getInput());
                gpuOutputs[i] = output;
            }
        }

        @Override
        public long onUpdate(FlowStage ctx, long now) {
            int size = this.size;
            long offset = this.offset;
            long nowOffset = now - offset;

            int index = this.index;

            long[] deadlines = deadlineCol;
            long deadline = deadlines[index];

            while (deadline <= nowOffset && ++index < size) {
                deadline = deadlines[index];
            }

            if (index >= size) {
                final SimMachineContext machineContext = this.ctx;
                if (machineContext != null) {
                    machineContext.shutdown();
                }
                ctx.close();
                return Long.MAX_VALUE;
            }

            this.index = index;

            int cpuCores = Math.min(cpuCoreCount, cpuCoresCol[index]);
            float cpuUsage = (float) cpuUsageCol[index] / cpuCores;

            final OutPort[] cpuOutputs = this.cpuOutputs;

            for (int i = 0; i < cpuCores; i++) {
                cpuOutputs[i].push(cpuUsage);
            }

            for (int i = cpuCores; i < cpuOutputs.length; i++) {
                cpuOutputs[i].push(0.f);
            }

            float gpuUsage = (float) gpuUsageCol[index];

            final OutPort[] gpuOutputs = this.gpuOutputs;

            for (int i = 0; i < gpuCount; i++) {
                gpuOutputs[i].push(gpuUsage);
            }

            for (int i = gpuCount; i < gpuOutputs.length; i++) {
                gpuOutputs[i].push(0.f);
            }

            return deadline + offset;
        }

        @Override
        public FlowStage getStage() {
            return stage;
        }

        @Override
        public int getIndex() {
            return index;
        }
    }
}
