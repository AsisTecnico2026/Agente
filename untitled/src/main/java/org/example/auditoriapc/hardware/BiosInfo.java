package org.example.auditoriapc.hardware;

import oshi.SystemInfo;
import oshi.hardware.Firmware;

public class BiosInfo {

    private final Firmware firmware;

    public BiosInfo() {
        SystemInfo systemInfo = new SystemInfo();
        this.firmware = systemInfo.getHardware()
                .getComputerSystem()
                .getFirmware();
    }

    public void mostrarInformacion() {

        System.out.println("\n========== BIOS / UEFI ==========");

        System.out.println("Fabricante: "
                + firmware.getManufacturer());

        System.out.println("Nombre: "
                + firmware.getName());

        System.out.println("Versión: "
                + firmware.getVersion());

        System.out.println("Fecha de lanzamiento: "
                + firmware.getReleaseDate());
    }
}