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

package org.opendc.simulator.compute.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A description of the physical or virtual machine on which a bootable image runs.
 */
public final class MachineModel {
    private final List<ProcessingUnit> cpus;
    private final List<ProcessingUnit> gpus;
    private final List<MemoryUnit> memory;
    private final List<NetworkAdapter> net;
    private final List<StorageDevice> storage;

    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpus The list of processing units available to the image.
     * @param gpus The list of graphics processing units available to the image. // TODO: Might not be needed
     * @param memory The list of memory units available to the image.
     * @param net A list of network adapters available to the machine.
     * @param storage A list of storage devices available to the machine.
     */
    public MachineModel(
            Iterable<ProcessingUnit> cpus,
            Iterable<ProcessingUnit> gpus,
            Iterable<MemoryUnit> memory,
            Iterable<NetworkAdapter> net,
            Iterable<StorageDevice> storage) {
        this.cpus = new ArrayList<>();
        cpus.forEach(this.cpus::add);

        this.gpus = new ArrayList<>();
        gpus.forEach(this.gpus::add);

        this.memory = new ArrayList<>();
        memory.forEach(this.memory::add);

        this.net = new ArrayList<>();
        net.forEach(this.net::add);

        this.storage = new ArrayList<>();
        storage.forEach(this.storage::add);
    }

    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpus The list of processing units available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(Iterable<ProcessingUnit> cpus, Iterable<MemoryUnit> memory) {
        this(cpus, Collections.emptyList(), memory, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Construct a {@link MachineModel} instance.
     *
     * @param cpus The list of processing units available to the image.
     * @param gpus The list of processing units available to the image.
     * @param memory The list of memory units available to the image.
     */
    public MachineModel(Iterable<ProcessingUnit> cpus, Iterable<ProcessingUnit> gpus, Iterable<MemoryUnit> memory) {
        this(cpus, gpus, memory, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Optimize the [MachineModel] by merging all resources of the same type into a single resource with the combined
     * capacity. Such configurations can be simulated more efficiently by OpenDC.
     */
    public MachineModel optimize() {
        ProcessingUnit originalCpu = cpus.get(0);
        ProcessingUnit originalGpu = gpus.get(0);

        double cpuFreq = 0.0;
        double gpuFreq = 0.0;

        for (ProcessingUnit cpu : cpus) {
            cpuFreq += cpu.getFrequency();
        }

        for (ProcessingUnit gpu : gpus) {
            gpuFreq += gpu.getFrequency();
        }

        ProcessingNode originalCpuNode = originalCpu.getNode();
        ProcessingNode originalGpuNode = originalGpu.getNode();

        ProcessingNode cpuNode = new ProcessingNode(
            originalCpuNode.getVendor(), originalCpuNode.getModelName(), originalCpuNode.getArchitecture(), 1);
        ProcessingUnit cpuUnit = new ProcessingUnit(cpuNode, originalCpu.getId(), cpuFreq);

        ProcessingNode gpuNode = new ProcessingNode(
            originalGpuNode.getVendor(), originalGpuNode.getModelName(), originalGpuNode.getArchitecture(), 1);
        ProcessingUnit gpuUnit = new ProcessingUnit(gpuNode, originalGpu.getId(), gpuFreq);

        long memorySize = 0;
        for (MemoryUnit mem : memory) {
            memorySize += mem.getSize();
        }
        MemoryUnit memoryUnit = new MemoryUnit("Generic", "Generic", 3200.0, memorySize);

        return new MachineModel(List.of(cpuUnit), List.of(gpuUnit), List.of(memoryUnit));
    }

    /**
     * Return the processing units of this machine.
     */
    public List<ProcessingUnit> getCpus() {
        return Collections.unmodifiableList(cpus);
    }

    /**
     * Return the graphics processing units of this machine.
     */
    public List<ProcessingUnit> getGpus() {
        return Collections.unmodifiableList(gpus);
    }

    /**
     * Return the memory units of this machine.
     */
    public List<MemoryUnit> getMemory() {
        return Collections.unmodifiableList(memory);
    }

    /**
     * Return the network adapters of this machine.
     */
    public List<NetworkAdapter> getNetwork() {
        return Collections.unmodifiableList(net);
    }

    /**
     * Return the storage devices of this machine.
     */
    public List<StorageDevice> getStorage() {
        return Collections.unmodifiableList(storage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MachineModel that = (MachineModel) o;
        return cpus.equals(that.cpus)
                && gpus.equals(that.gpus)
                && memory.equals(that.memory)
                && net.equals(that.net)
                && storage.equals(that.storage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpus, gpus, memory, net, storage);
    }

    @Override
    public String toString() {
        return "MachineModel[cpus=" + cpus + ",gpus=" + gpus + ",memory=" + memory + ",net=" + net + ",storage=" + storage + "]";
    }
}
