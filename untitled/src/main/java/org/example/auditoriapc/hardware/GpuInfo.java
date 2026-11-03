package org.example.auditoriapc.hardware;

import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;

import java.util.List;

public class GpuInfo {

    private final List<GraphicsCard> tarjetas;

    public GpuInfo() {
        SystemInfo systemInfo = new SystemInfo();
        this.tarjetas = systemInfo.getHardware()
                .getGraphicsCards();
    }

    public void mostrarInformacion() {

        System.out.println("\n========== TARJETA GRÁFICA ==========");

        if (tarjetas.isEmpty()) {
            System.out.println(
                    "No se detectaron tarjetas gráficas."
            );
            return;
        }

        for (GraphicsCard gpu : tarjetas) {

            System.out.println("Nombre: " + gpu.getName());
            System.out.println("Fabricante: " + gpu.getVendor());

            System.out.printf(
                    "Memoria de video: %.2f GB%n",
                    gpu.getVRam()
                            / (1024.0 * 1024.0 * 1024.0)
            );

            System.out.println("ID del dispositivo: "
                    + gpu.getDeviceId());
        }
    }
}