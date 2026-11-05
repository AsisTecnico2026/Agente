package org.example.auditoriapc.servicio;

import com.google.gson.JsonObject;
import org.example.auditoriapc.comunicacion.ClienteApi;
import org.example.auditoriapc.modelo.Auditoria;
import org.example.auditoriapc.recoleccion.Auditor;
import org.example.auditoriapc.recoleccion.ConversorApi;

public final class Inventario {

    private Inventario() { }

    public static JsonObject enviar(String apiUrl, String token) throws Exception {
        Auditoria auditoria = new Auditor().auditar();
        JsonObject inventario = new ConversorApi().aJson(auditoria);
        return new ClienteApi(apiUrl).enviarInventario(token, inventario);
    }
}