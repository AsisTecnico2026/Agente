package org.example.auditoriapc;

import com.google.gson.JsonObject;
import org.example.auditoriapc.comunicacion.ClienteApi;
import org.example.auditoriapc.comunicacion.ClienteApi.ApiException;
import org.example.auditoriapc.config.Configuracion;
import org.example.auditoriapc.modelo.Auditoria;
import org.example.auditoriapc.recoleccion.Auditor;
import org.example.auditoriapc.recoleccion.ConversorApi;
import org.example.auditoriapc.recoleccion.Identificador;
import org.example.auditoriapc.servicio.Inventario;
import org.example.auditoriapc.servicio.ServicioAgente;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static final String VERSION = "1.0";

    public static void main(String[] args) {
        String modo = args.length > 0 ? args[0] : "--imprimir";
        try {
            switch (modo) {
                case "--activar" -> activar(args);
                case "--enviar" -> enviar();
                case "--imprimir" -> imprimir(args);
                case "--servicio" -> new ServicioAgente().ejecutar();
                default -> uso();
            }
        } catch (ApiException e) {
            System.err.println("La API respondió " + e.codigo + ": " + e.getMessage());
            if (e.codigo == 401 && modo.equals("--enviar")) {
                System.err.println("El token ya no sirve. Vuelve a activar el agente con --activar.");
            }
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void uso() {
        System.out.println("Uso:");
        System.out.println("  --activar URL_API CLAVE   activa este equipo con una clave");
        System.out.println("  --enviar                  audita y manda el inventario a la API");
        System.out.println("  --imprimir [archivo]      muestra el JSON de la auditoría (sin enviarlo)");
        System.out.println("  --servicio                queda corriendo: audita al iniciar y atiende solicitudes de la web");
    }

    private static void activar(String[] args) throws Exception {
        if (args.length < 3) {
            uso();
            return;
        }
        String url = args[1];
        Auditoria auditoria = new Auditor().auditar();
        String identificador = Identificador.de(auditoria);

        JsonObject r = new ClienteApi(url)
                .activar(args[2], identificador, auditoria.sistema().hostname(), VERSION);

        Configuracion cfg = new Configuracion();
        cfg.apiUrl = url;
        cfg.identificador = identificador;
        cfg.idAgente = r.get("id_agente").getAsInt();
        cfg.guardarToken(r.get("token").getAsString());
        cfg.guardar();
        System.out.println("Agente activado. Configuración guardada en " + Configuracion.archivo());
    }

    private static void enviar() throws Exception {
        if (!Configuracion.existe()) {
            System.err.println("Este equipo no está activado. Usa --activar URL_API CLAVE");
            System.exit(1);
        }
        Configuracion cfg = Configuracion.cargar();
        JsonObject r = Inventario.enviar(cfg.apiUrl, cfg.leerToken());

        boolean nuevo = r.get("equipo_nuevo").getAsBoolean();
        System.out.println("Equipo: " + r.get("nombre_equipo").getAsString()
                + (nuevo ? " (equipo nuevo: se creó una alerta)" : " (ya estaba registrado)"));
        System.out.println("Piezas creadas: " + r.get("creados").getAsInt()
                + ", actualizadas: " + r.get("actualizados").getAsInt()
                + ", dadas de baja: " + r.get("dados_de_baja").getAsInt());
    }

    private static void imprimir(String[] args) throws Exception {
        String json = new ConversorApi().aTexto(new Auditor().auditar());
        if (args.length >= 2) {
            Files.writeString(Path.of(args[1]), json, StandardCharsets.UTF_8);
            System.out.println("Auditoría guardada en " + args[1]);
        } else {
            System.out.println(json);
        }
    }
}