package org.example.auditoriapc.servicio;

import org.example.auditoriapc.config.Configuracion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Registro {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Registro() { }

    public static synchronized void escribir(String mensaje) {
        String linea = LocalDateTime.now().format(FORMATO) + "  " + mensaje;
        System.out.println(linea);
        try {
            Path archivo = Configuracion.archivo().resolveSibling("agente.log");
            Files.createDirectories(archivo.getParent());
            // Si el archivo pasa de 1 MB, el anterior se guarda como agente.log.1
            if (Files.exists(archivo) && Files.size(archivo) > 1_000_000) {
                Files.move(archivo, archivo.resolveSibling("agente.log.1"), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(archivo, linea + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignorada) {
            // si no se puede escribir el archivo, igual queda en la consola
        }
    }
}