package org.example.auditoriapc.hardware;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CpuInfo {

    private final CentralProcessor cpu;

    public CpuInfo() {
        SystemInfo systemInfo = new SystemInfo();
        this.cpu = systemInfo.getHardware().getProcessor();
    }

    public String getFabricante() {
        return cpu.getProcessorIdentifier().getVendor();
    }

    public String getModelo() {
        return cpu.getProcessorIdentifier().getName();
    }

    public int getNucleosFisicos() {
        return cpu.getPhysicalProcessorCount();
    }

    public int getNucleosLogicos() {
        return cpu.getLogicalProcessorCount();
    }
}