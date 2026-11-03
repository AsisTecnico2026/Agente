package org.example.auditoriapc.system;

import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

public class WindowsInfo {

    private final OperatingSystem sistema;

    public WindowsInfo() {
        SystemInfo systemInfo = new SystemInfo();
        this.sistema = systemInfo.getOperatingSystem();
    }

    public void mostrarInformacion() {

        // Obtener variables de Windows
        String equipo = System.getenv("COMPUTERNAME");
        String usuario = System.getenv("USERNAME");
        String dominio = System.getenv("USERDOMAIN");
        String dominioDNS = System.getenv("USERDNSDOMAIN");

        System.out.println("\n========== WINDOWS ==========");

        System.out.println("Sistema operativo: "
                + sistema.getFamily());

        System.out.println("Fabricante: "
                + sistema.getManufacturer());

        System.out.println("Versión: "
                + sistema.getVersionInfo().getVersion());

        System.out.println("Compilación: "
                + sistema.getVersionInfo().getBuildNumber());

        System.out.println("Arquitectura: "
                + sistema.getBitness() + " bits");

        System.out.println("Nombre del equipo: "
                + equipo);

        System.out.println("\n--- USUARIO Y DOMINIO ---");

        System.out.println("Usuario conectado: "
                + usuario);

        System.out.println("Dominio o equipo de inicio de sesión: "
                + dominio);

        if (dominioDNS != null && !dominioDNS.isBlank()) {

            System.out.println("Dominio DNS: "
                    + dominioDNS);

            System.out.println("Tipo de cuenta: Dominio");

        } else if (equipo != null && dominio != null
                && equipo.equalsIgnoreCase(dominio)) {

            System.out.println("Tipo de cuenta: Local");

        } else {

            System.out.println("Tipo de cuenta: No determinado");
        }
    }
}