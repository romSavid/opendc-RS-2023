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

package org.opendc.compute.service;

import org.opendc.compute.service.driver.Host;

/**
 * A view of a {@link Host} as seen from the {@link ComputeService}.
 */
public class HostView {
    private final Host host;
    int instanceCount;
    long availableMemory;
    int provisionedCores;
    double availableGpuCapacity;

    /**
     * Construct a {@link HostView} instance.
     *
     * @param host The host to create a view of.
     */
    public HostView(Host host) {
        this.host = host;
        this.availableMemory = host.getModel().memoryCapacity();
    }

    /**
     * The {@link Host} this is a view of.
     */
    public Host getHost() {
        return host;
    }

    /**
     * Return the number of instances on this host.
     */
    public int getInstanceCount() {
        return instanceCount;
    }

    /**
     * Return the available memory of the host.
     */
    public long getAvailableMemory() {
        return availableMemory;
    }

    /**
     * Return the provisioned cores on the host.
     */
    public int getProvisionedCores() {
        return provisionedCores;
    }
    public double getAvailableGpuCapacity() { return availableGpuCapacity; }

    @Override
    public String toString() {
        return "HostView[host=" + host + "]";
    }
}
