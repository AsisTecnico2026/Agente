package org.example.auditoriapc.hardware;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.PhysicalMemory;

import java.util.List;
public class RamInfo {

    private final GlobalMemory memoria;

    public RamInfo() {
        SystemInfo systemInfo = new SystemInfo();
        this.memoria = systemInfo.getHardware().getMemory();
    }

    // Convierte bytes a GB
    private double convertirAGB(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    // Memoria RAM total reconocida por el sistema
    public double getMemoriaTotalGB() {
        return convertirAGB(memoria.getTotal());
    }

    // Memoria RAM disponible actualmente
    public double getMemoriaDisponibleGB() {
        return convertirAGB(memoria.getAvailable());
    }

    // Información de los módulos físicos de RAM
    public List<PhysicalMemory> getModulosFisicos() {
        return memoria.getPhysicalMemory();
    }

    // Cantidad de módulos detectados
    public int getCantidadModulos() {
        return getModulosFisicos().size();
    }

}