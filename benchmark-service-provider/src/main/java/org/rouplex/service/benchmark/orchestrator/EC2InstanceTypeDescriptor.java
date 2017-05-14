package org.rouplex.service.benchmark.orchestrator;

import com.google.gson.annotations.SerializedName;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
class EC2InstanceTypeDescriptor {
    @SerializedName("compute_units")
    double computeUnits;
    int cores;
    int gpus;
    int ramMB;
    int[] storageGB;
    @SerializedName("i/o")
    String io;
    @SerializedName("ebs_optimized_iopsMbps")
    int ebsOptimizedIopsMbps;
    int[] arch;

    double cost;

    EC2InstanceTypeDescriptor(int computeUnits, int cores, int gpus, int ramMB, int[] storageGB, String io, int ebsOptimizedIopsMbps, int[] arch) {
    }

    EC2InstanceTypeDescriptor() {
    }

    public double getComputeUnits() {
        return computeUnits;
    }

    public void setComputeUnits(double computeUnits) {
        this.computeUnits = computeUnits;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public int getGpus() {
        return gpus;
    }

    public void setGpus(int gpus) {
        this.gpus = gpus;
    }

    public int getRamMB() {
        return ramMB;
    }

    public void setRamMB(int ramMB) {
        this.ramMB = ramMB;
    }

    public EC2InstanceTypeDescriptor withRamMB(int ramMB) {
        this.ramMB = ramMB;
        return this;
    }

    public int[] getStorageGB() {
        return storageGB;
    }

    public void setStorageGB(int[] storageGB) {
        this.storageGB = storageGB;
    }

    public String getIo() {
        return io;
    }

    public void setIo(String io) {
        this.io = io;
    }

    public int getEbsOptimizedIopsMbps() {
        return ebsOptimizedIopsMbps;
    }

    public void setEbsOptimizedIopsMbps(int ebsOptimizedIopsMbps) {
        this.ebsOptimizedIopsMbps = ebsOptimizedIopsMbps;
    }

    public int[] getArch() {
        return arch;
    }

    public void setArch(int[] arch) {
        this.arch = arch;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public EC2InstanceTypeDescriptor withCost(double cost) {
        this.cost = cost;
        return this;
    }
}
