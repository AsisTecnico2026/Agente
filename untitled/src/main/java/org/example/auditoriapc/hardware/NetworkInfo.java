package org.example.auditoriapc.hardware;

import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

import java.util.List;

public class NetworkInfo {

    private final List<NetworkIF> interfaces;

    public NetworkInfo() {
        SystemInfo systemInfo = new SystemInfo();
        this.interfaces = systemInfo.getHardware()
                .getNetworkIFs();
    }

    public void mostrarInformacion() {

        System.out.println("\n========== RED ==========");

        for (NetworkIF red : interfaces) {

            red.updateAttributes();

            System.out.println("\nAdaptador: "
                    + red.getDisplayName());

            System.out.println("Nombre: "
                    + red.getName());

            System.out.println("Dirección MAC: "
                    + red.getMacaddr());

            System.out.println("Direcciones IPv4: "
                    + String.join(", ", red.getIPv4addr()));

            System.out.println("Direcciones IPv6: "
                    + String.join(", ", red.getIPv6addr()));

            System.out.printf(
                    "Velocidad: %.2f Mbps%n",
                    red.getSpeed() / 1_000_000.0
            );

            System.out.printf(
                    "Datos recibidos: %.2f MB%n",
                    red.getBytesRecv() / (1024.0 * 1024.0)
            );

            System.out.printf(
                    "Datos enviados: %.2f MB%n",
                    red.getBytesSent() / (1024.0 * 1024.0)
            );
        }
    }
}