package org.example.auditoriapc;

import org.example.auditoriapc.modelo.Auditoria;
import org.example.auditoriapc.recoleccion.Auditor;
import org.example.auditoriapc.recoleccion.ConversorApi;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        Auditoria auditoria = new Auditor().auditar();
        String json = new ConversorApi().aTexto(auditoria);

        // Uso:  --archivo salida.json   guarda el resultado en un archivo
        if (args.length >= 2 && args[0].equals("--archivo")) {
            Files.writeString(Path.of(args[1]), json, StandardCharsets.UTF_8);
            System.out.println("Auditoría guardada en " + args[1]);
        } else {
            System.out.println(json);
        }
    }
}