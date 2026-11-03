package org.example.auditoriapc.modelo;
import java.util.List;

public record Auditoria(
        String fecha,
        Placa placa,
        Sistema sistema,
        Procesador procesador,
        List<ModuloRam> modulosRam,
        List<Disco> discos,
        List<Gpu> gpus,
        List<AdaptadorRed> adaptadores) {

    public record Placa(String fabricante, String modelo, String serie, String uuid,
                        String biosVersion, String biosFecha, boolean esPortatil) { }

    public record Sistema(String nombre, String version, String compilacion, int bits,
                          String hostname, String usuario, String dominioUsuario,
                          String tipoCuenta, boolean enDominio, String dominioEquipo) { }

    public record Procesador(String fabricante, String modelo, int nucleosFisicos, int nucleosLogicos) { }

    public record ModuloRam(String ranura, double capacidadGb, String fabricante,
                            long velocidadMhz, String tipo) { }

    public record Disco(String modelo, String serie, double capacidadGb, String tipo) { }

    public record Gpu(String nombre, String fabricante, double memoriaGb) { }

    public record AdaptadorRed(String nombre, String mac, List<String> ipv4,
                               String tipo, double velocidadMbps) { }
}