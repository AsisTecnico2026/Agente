package org.example.auditoriapc.recoleccion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.auditoriapc.modelo.Auditoria;

import java.util.Locale;

public class ConversorApi {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String aTexto(Auditoria a) {
        return GSON.toJson(aJson(a));
    }

    public JsonObject aJson(Auditoria a) {
        JsonObject raiz = new JsonObject();
        raiz.addProperty("nombre_equipo", nombrePrincipal(a));
        raiz.addProperty("uuid", a.placa().uuid());
        raiz.addProperty("tipo", a.placa().esPortatil() ? "Laptop" : "Desktop");
        raiz.addProperty("fecha_auditoria", a.fecha());
        raiz.add("sistema", sistema(a));

        JsonArray piezas = new JsonArray();

        // Tarjeta madre: aquí también viaja la versión de BIOS
        String bios = a.placa().biosVersion() == null ? null
                : "BIOS " + a.placa().biosVersion()
                + (a.placa().biosFecha() == null ? "" : " (" + a.placa().biosFecha() + ")");
        piezas.add(pieza("Motherboard", a.placa().fabricante(), a.placa().modelo(),
                a.placa().serie(), bios, null));

        JsonObject cpu = new JsonObject();
        cpu.addProperty("nucleo", a.procesador().nucleosFisicos());
        cpu.addProperty("procesador_logicos", a.procesador().nucleosLogicos());
        piezas.add(pieza("Procesador", a.procesador().fabricante(), a.procesador().modelo(),
                null, null, cpu));

        for (var m : a.modulosRam()) {
            JsonObject d = new JsonObject();
            d.addProperty("tipo", m.tipo());
            d.addProperty("capacidad", m.capacidadGb());
            piezas.add(pieza("Memoria RAM", m.fabricante(), null, null,
                    m.ranura() + ", " + m.velocidadMhz() + " MHz", d));
        }

        for (var disco : a.discos()) {
            JsonObject d = new JsonObject();
            d.addProperty("tipo", disco.tipo());
            d.addProperty("capacidad", disco.capacidadGb());
            piezas.add(pieza("Disco duro", null, disco.modelo(), disco.serie(), null, d));
        }

        for (var gpu : a.gpus()) {
            JsonObject d = new JsonObject();
            d.addProperty("capacidad", gb(gpu.memoriaGb()));
            piezas.add(pieza("Tarjeta de video", gpu.fabricante(), gpu.nombre(), null, null, d));
        }

        for (var red : a.adaptadores()) {
            JsonObject d = new JsonObject();
            d.addProperty("tipo", red.tipo());
            d.addProperty("ip", red.ipv4().stream().filter(s -> !s.isBlank()).findFirst().orElse(null));
            d.addProperty("mac", red.mac());
            piezas.add(pieza("Tarjeta de red", null, red.nombre(), red.mac(),
                    Math.round(red.velocidadMbps()) + " Mbps", d));
        }

        raiz.add("componentes", piezas);
        return raiz;
    }

    // Nombre principal: fabricante y modelo de la tarjeta madre
    // (si solo quieres el fabricante, deja únicamente a.placa().fabricante())
    String nombrePrincipal(Auditoria a) {
        String fab = a.placa().fabricante() == null ? "" : a.placa().fabricante();
        String modelo = a.placa().modelo() == null ? "" : a.placa().modelo();
        String nombre = (fab + " " + modelo).trim();
        return nombre.isEmpty() ? a.sistema().hostname() : nombre;
    }

    private JsonObject sistema(Auditoria a) {
        JsonObject s = new JsonObject();
        s.addProperty("nombre_pc", a.sistema().hostname());
        s.addProperty("sistema_operativo", a.sistema().nombre() + " " + a.sistema().version());
        s.addProperty("compilacion", a.sistema().compilacion());
        s.addProperty("arquitectura", a.sistema().bits() + " bits");
        s.addProperty("usuario", a.sistema().usuario());
        s.addProperty("dominio_usuario", a.sistema().dominioUsuario());
        s.addProperty("tipo_cuenta", a.sistema().tipoCuenta());
        s.addProperty("equipo_en_dominio", a.sistema().enDominio());
        s.addProperty("dominio_equipo", a.sistema().dominioEquipo());
        return s;
    }

    private JsonObject pieza(String tipo, String marca, String modelo, String serie,
                             String especificaciones, JsonObject detalle) {
        JsonObject p = new JsonObject();
        p.addProperty("tipo_componente", tipo);
        p.addProperty("marca", marca);
        p.addProperty("modelo", modelo);
        p.addProperty("numero_serie", serie);
        p.addProperty("especificaciones", especificaciones);
        if (detalle != null) p.add("detalle", detalle);
        return p;
    }

    private String gb(double valor) {
        return valor == Math.rint(valor)
                ? (long) valor + " GB"
                : String.format(Locale.ROOT, "%.1f GB", valor);
    }
}