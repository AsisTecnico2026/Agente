package org.example.auditoriapc.servicio;

import com.google.gson.JsonObject;
import org.example.auditoriapc.comunicacion.ClienteApi;
import org.example.auditoriapc.comunicacion.ClienteApi.ApiException;
import org.example.auditoriapc.config.Configuracion;
import org.example.auditoriapc.recoleccion.Auditor;

public class ServicioAgente {

    private static final long CONSULTA_SEGUNDOS = 120;      // cada cuánto revisa solicitudes e inicios de sesión
    private static final long AUDITORIA_HORAS = 24;         // auditoría automática aunque no pase nada
    private static final long ESPERA_TOKEN_SEGUNDOS = 600;  // si el token no sirve, reintenta cada 10 minutos

    private volatile boolean activo = true;

    public void detener() {
        activo = false;
    }

    public void ejecutar() {
        if (!Configuracion.existe()) {
            Registro.escribir("Este equipo no está activado. Ejecuta --activar URL_API CLAVE y vuelve a iniciar.");
            return;
        }
        Configuracion cfg;
        String token;
        try {
            cfg = Configuracion.cargar();
            token = cfg.leerToken();
        } catch (Exception e) {
            Registro.escribir("No se pudo leer la configuración: " + e.getMessage());
            return;
        }
        ClienteApi cliente;
        try {
            cliente = new ClienteApi(cfg.apiUrl);
        } catch (IllegalStateException e) {
            Registro.escribir(e.getMessage());
            return;
        }

        // Si Windows apaga el servicio, queda constancia en el registro
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            activo = false;
            Registro.escribir("Agente detenido.");
        }));

        Registro.escribir("Agente iniciado. API: " + cfg.apiUrl);
        Auditor auditor = new Auditor();

        long ultimaAuditoria = 0;   // 0 = todavía no se envió ninguna, así que la primera se hace al arrancar
        String ultimoUsuario = null;
        while (activo) {
            long espera = CONSULTA_SEGUNDOS;
            try {
                String usuarioActual = auditor.usuarioConectado();

                String motivo = null;
                if (ultimaAuditoria == 0) {
                    motivo = "inicio del agente";
                } else if (cliente.consultarTareas(token).get("auditar").getAsBoolean()) {
                    motivo = "solicitud manual";
                } else if (usuarioActual != null && !usuarioActual.equalsIgnoreCase(ultimoUsuario)) {
                    motivo = "inicio de sesión";
                } else if (System.currentTimeMillis() - ultimaAuditoria >= AUDITORIA_HORAS * 3_600_000L) {
                    motivo = "auditoría periódica";
                }

                if (motivo != null) {
                    JsonObject r = Inventario.enviar(cfg.apiUrl, token);
                    ultimaAuditoria = System.currentTimeMillis();
                    Registro.escribir("Auditoría enviada (" + motivo + "): "
                            + r.get("creados").getAsInt() + " piezas nuevas, "
                            + r.get("actualizados").getAsInt() + " actualizadas"
                            + (r.get("equipo_nuevo").getAsBoolean() ? ". Equipo nuevo registrado." : "."));
                }
                ultimoUsuario = usuarioActual;
            } catch (ApiException e) {
                Registro.escribir("La API respondió " + e.codigo + ": " + e.getMessage());
                if (e.codigo == 401) {
                    Registro.escribir("El token ya no sirve. Hay que activar el agente de nuevo con --activar.");
                    espera = ESPERA_TOKEN_SEGUNDOS;
                }
            } catch (InterruptedException e) {
                activo = false;
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Pasa al encender el equipo si la red todavía no está lista: se reintenta solo
                Registro.escribir("Sin conexión con la API (" + e.getMessage() + "). Se reintenta.");
            }
            dormir(espera);
        }
    }

    private void dormir(long segundos) {
        try {
            Thread.sleep(segundos * 1000);
        } catch (InterruptedException e) {
            activo = false;
            Thread.currentThread().interrupt();
        }
    }
}