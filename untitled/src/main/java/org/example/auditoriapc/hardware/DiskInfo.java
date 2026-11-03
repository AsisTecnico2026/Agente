package org.example.auditoriapc.hardware;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;

import java.util.List;
import oshi.software.os.OSFileStore;
import oshi.software.os.FileSystem;
import oshi.software.os.OperatingSystem;

public class DiskInfo {

    private final List<HWDiskStore> discos;

    public DiskInfo() {
        SystemInfo systemInfo = new SystemInfo();
        this.discos = systemInfo.getHardware()
                .getDiskStores();
    }

    private double convertirAGB(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }

    public List<HWDiskStore> getDiscos() {
        return discos;
    }

    public void mostrarInformacion() {

        System.out.println("\n========== DISCOS ==========");

        for (HWDiskStore disco : discos) {

            System.out.println("\nDisco: " + disco.getName());
            System.out.println("Modelo: " + disco.getModel());
            System.out.println("Fabricante: "
                    + disco.getModel()); // Modelo reportado por el sistema

            System.out.printf(
                    "Capacidad: %.2f GB%n",
                    convertirAGB(disco.getSize())
            );

            System.out.println("Particiones:");

            for (HWPartition particion : disco.getPartitions()) {

                System.out.println("  Nombre: "
                        + particion.getName());

                System.out.println("  Tipo: "
                        + particion.getType());

                System.out.printf(
                        "  Tamaño: %.2f GB%n",
                        convertirAGB(particion.getSize())
                );

                System.out.println("  Punto de montaje: "
                        + particion.getMountPoint());
            }
        }
    }
    // Muestra el espacio utilizado y disponible de las unidades
    public void mostrarEspacioUnidades() {

        SystemInfo systemInfo = new SystemInfo();

        OperatingSystem sistema =
                systemInfo.getOperatingSystem();

        FileSystem fileSystem =
                sistema.getFileSystem();

        List<OSFileStore> unidades =
                fileSystem.getFileStores();

        System.out.println("\n========== ESPACIO DE UNIDADES ==========");

        for (OSFileStore unidad : unidades) {

            long total = unidad.getTotalSpace();
            long disponible = unidad.getUsableSpace();
            long libre = unidad.getFreeSpace();

            // Espacio utilizado = total - espacio libre
            long utilizado = total - libre;

            System.out.println("\nUnidad: " + unidad.getMount());

            System.out.println("Nombre: " + unidad.getName());

            System.out.printf(
                    "Espacio total: %.2f GB%n",
                    convertirAGB(total)
            );

            System.out.printf(
                    "Espacio utilizado: %.2f GB%n",
                    convertirAGB(utilizado)
            );

            System.out.printf(
                    "Espacio disponible: %.2f GB%n",
                    convertirAGB(disponible)
            );

            double porcentajeUso = total > 0
                    ? (utilizado * 100.0) / total
                    : 0;

            System.out.printf(
                    "Porcentaje utilizado: %.2f%%%n",
                    porcentajeUso
            );
        }
    }
}