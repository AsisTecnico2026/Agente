package org.example.auditoriapc.recoleccion;

import org.example.auditoriapc.modelo.Auditoria;

public final class Identificador {

    private Identificador() { }

    public static String de(Auditoria a) {
        String uuid = a.placa().uuid();
        if (uuidValido(uuid)) return uuid.toLowerCase();
        String serie = a.placa().serie();
        if (serie != null) return "SN-" + serie;
        return "PC-" + a.sistema().hostname();
    }

    // Descarta los UUID de relleno (todo ceros o todo F)
    private static boolean uuidValido(String u) {
        if (u == null) return false;
        String limpio = u.replace("-", "").toLowerCase();
        return limpio.length() >= 16
                && !limpio.chars().allMatch(c -> c == '0')
                && !limpio.chars().allMatch(c -> c == 'f');
    }
}