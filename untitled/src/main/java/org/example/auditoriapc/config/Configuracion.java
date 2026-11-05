package org.example.auditoriapc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.platform.win32.Crypt32Util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class Configuracion {

    public String apiUrl;
    public String identificador;
    public int idAgente;

    private String tokenProtegido;

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    // DPAPI: protección para todos los usuarios del equipo
    private static final int CRYPTPROTECT_LOCAL_MACHINE = 0x4;

    public static Path archivo() {
        String base = System.getenv("PROGRAMDATA");

        if (base == null || base.isBlank()) {
            base = System.getProperty("user.home");
        }

        return Path.of(base, "SFMantto", "agente.json");
    }

    public static boolean existe() {
        return Files.exists(archivo());
    }

    public static Configuracion cargar() throws IOException {
        return GSON.fromJson(
                Files.readString(archivo(), StandardCharsets.UTF_8),
                Configuracion.class
        );
    }

    public void guardar() throws IOException {
        Path f = archivo();

        Files.createDirectories(f.getParent());

        Files.writeString(
                f,
                GSON.toJson(this),
                StandardCharsets.UTF_8
        );
    }

    public void guardarToken(String token) {
        byte[] cifrado = Crypt32Util.cryptProtectData(
                token.getBytes(StandardCharsets.UTF_8),
                CRYPTPROTECT_LOCAL_MACHINE
        );

        this.tokenProtegido =
                Base64.getEncoder().encodeToString(cifrado);
    }

    public String leerToken() {
        if (tokenProtegido == null || tokenProtegido.isBlank()) {
            return null;
        }

        byte[] claro = Crypt32Util.cryptUnprotectData(
                Base64.getDecoder().decode(tokenProtegido)
        );

        return new String(claro, StandardCharsets.UTF_8);
    }
}
