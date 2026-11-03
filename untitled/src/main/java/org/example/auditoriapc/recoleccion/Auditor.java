package org.example.auditoriapc.recoleccion;

import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Firmware;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PhysicalMemory;
import oshi.software.os.OSSession;
import oshi.software.os.OperatingSystem;
import org.example.auditoriapc.modelo.Auditoria;
import org.example.auditoriapc.modelo.Auditoria.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Auditor {

    private static final double GB = 1024.0 * 1024.0 * 1024.0;

    // Fragmentos que delatan filtros de Windows y adaptadores virtuales (no son tarjetas reales)
    private static final List<String> RUIDO_RED = List.of(
            "-wfp", "-qos", "-native wifi", "-virtual wifi", "-virtualbox ndis", "lightweight filter",
            "packet scheduler", "wi-fi direct", "bluetooth", "virtualbox", "vmware", "hyper-v",
            "loopback", "tap-windows", "miniport", "pseudo", "vpn");

    // Textos que los fabricantes dejan por defecto en lugar de un dato real
    private static final List<String> VACIOS = List.of(
            "unknown", "none", "default string", "not specified", "to be filled by o.e.m.",
            "system serial number", "system product name", "n/a");

    public Auditoria auditar() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hw = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();
        return new Auditoria(
                OffsetDateTime.now().toString(),
                placa(hw),
                sistema(os),
                procesador(hw),
                modulosRam(hw),
                discos(hw),
                gpus(hw),
                adaptadores(hw));
    }

    private Placa placa(HardwareAbstractionLayer hw) {
        ComputerSystem cs = hw.getComputerSystem();
        Baseboard placa = cs.getBaseboard();
        Firmware bios = cs.getFirmware();
        // Si el equipo tiene batería, se considera portátil
        boolean portatil = !hw.getPowerSources().isEmpty();
        return new Placa(limpio(placa.getManufacturer()), limpio(placa.getModel()),
                limpio(placa.getSerialNumber()), limpio(cs.getHardwareUUID()),
                limpio(bios.getVersion()), limpio(bios.getReleaseDate()), portatil);
    }

    // Cuentas internas de Windows que no son una persona
    private static final List<String> CUENTAS_SERVICIO = List.of(
            "servicio local", "local service", "servicio de red", "network service",
            "system", "sistema", "dwm-", "umfd-");

    private Sistema sistema(OperatingSystem os) {
        String host = os.getNetworkParams().getHostName();
        String usuario = null;
        String dominioUsuario = null;
        boolean enDominio = false;
        String dominioEquipo = null;

        // Windows informa quién tiene la sesión abierta, aun si el agente corre como servicio
        JsonObject win = consultarWindows();
        if (win != null) {
            enDominio = win.has("enDominio") && !win.get("enDominio").isJsonNull()
                    && win.get("enDominio").getAsBoolean();
            dominioEquipo = texto(win, "dominio");
            String completo = texto(win, "usuario");          // llega como DOMINIO
            if (completo != null) {
                int i = completo.indexOf('\\');
                dominioUsuario = i > 0 ? completo.substring(0, i) : null;
                usuario = i >= 0 ? completo.substring(i + 1) : completo;
            }
        }
        if (usuario == null) {
            usuario = usuarioDeRespaldo(os);
            if (usuario != null) dominioUsuario = System.getenv("USERDOMAIN");
        }

        String tipoCuenta;
        if (usuario == null) tipoCuenta = "Sin sesión";
        else if (dominioUsuario != null && dominioUsuario.equalsIgnoreCase(host)) tipoCuenta = "Local";
        else if (enDominio && dominioUsuario != null) tipoCuenta = "Dominio";
        else tipoCuenta = "No determinado";

        return new Sistema(os.getFamily(), os.getVersionInfo().getVersion(),
                os.getVersionInfo().getBuildNumber(), os.getBitness(), host,
                usuario, dominioUsuario, tipoCuenta, enDominio, dominioEquipo);
    }

    // Consulta a Windows (PowerShell). El resultado viaja en Base64 para evitar problemas de acentos
    private JsonObject consultarWindows() {
        String script = "$c = Get-CimInstance Win32_ComputerSystem; "
                + "$j = [pscustomobject]@{ usuario = $c.UserName; enDominio = $c.PartOfDomain; dominio = $c.Domain } "
                + "| ConvertTo-Json -Compress; "
                + "[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($j))";
        try {
            Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script)
                    .redirectErrorStream(true).start();
            if (!p.waitFor(20, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            String salida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String ultima = salida.lines().reduce((a, b) -> b).orElse("").trim();
            String json = new String(Base64.getDecoder().decode(ultima), StandardCharsets.UTF_8);
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private String texto(JsonObject o, String clave) {
        return (o.has(clave) && !o.get(clave).isJsonNull()) ? limpio(o.get(clave).getAsString()) : null;
    }

    // Plan B si PowerShell no responde: sesiones abiertas, sin contar las cuentas de servicio
    private String usuarioDeRespaldo(OperatingSystem os) {
        try {
            for (OSSession s : os.getSessions()) {
                String u = s.getUserName();
                if (u != null && !u.isBlank() && !esCuentaDeServicio(u)) return u;
            }
        } catch (Exception ignorada) {
            // se usa la variable de entorno
        }
        String u = System.getenv("USERNAME");
        return (u == null || u.endsWith("$") || esCuentaDeServicio(u)) ? null : u;
    }

    private boolean esCuentaDeServicio(String usuario) {
        String u = usuario.toLowerCase();
        for (String c : CUENTAS_SERVICIO) {
            if (u.equals(c) || u.startsWith(c)) return true;
        }
        return false;
    }

    // Si el agente corre como servicio, USERNAME no es la persona: se leen las sesiones abiertas
    private String usuarioActivo(OperatingSystem os) {
        try {
            for (OSSession s : os.getSessions()) {
                if (s.getUserName() != null && !s.getUserName().isBlank()) return s.getUserName();
            }
        } catch (Exception ignorada) {
            // si no se pueden leer las sesiones, se usa la variable de entorno
        }
        String u = System.getenv("USERNAME");
        return (u == null || u.endsWith("$")) ? null : u;
    }

    private Procesador procesador(HardwareAbstractionLayer hw) {
        CentralProcessor cpu = hw.getProcessor();
        CentralProcessor.ProcessorIdentifier id = cpu.getProcessorIdentifier();
        return new Procesador(nombreFabricante(id.getVendor()), limpio(id.getName()),
                cpu.getPhysicalProcessorCount(), cpu.getLogicalProcessorCount());
    }

    private List<ModuloRam> modulosRam(HardwareAbstractionLayer hw) {
        List<ModuloRam> lista = new ArrayList<>();
        for (PhysicalMemory m : hw.getMemory().getPhysicalMemory()) {
            lista.add(new ModuloRam(limpio(m.getBankLabel()), redondear(m.getCapacity() / GB),
                    limpio(m.getManufacturer()), m.getClockSpeed() / 1_000_000L,
                    limpio(m.getMemoryType())));
        }
        return lista;
    }

    private List<Disco> discos(HardwareAbstractionLayer hw) {
        List<Disco> lista = new ArrayList<>();
        for (HWDiskStore d : hw.getDiskStores()) {
            if (d.getSize() <= 0) continue;
            String modelo = limpiarModelo(d.getModel());
            lista.add(new Disco(modelo, limpio(d.getSerial()), redondear(d.getSize() / GB), tipoDisco(modelo)));
        }
        return lista;
    }

    private List<Gpu> gpus(HardwareAbstractionLayer hw) {
        List<Gpu> lista = new ArrayList<>();
        for (GraphicsCard g : hw.getGraphicsCards()) {
            lista.add(new Gpu(limpio(g.getName()), limpio(g.getVendor()), redondear(g.getVRam() / GB)));
        }
        return lista;
    }

    private List<AdaptadorRed> adaptadores(HardwareAbstractionLayer hw) {
        List<AdaptadorRed> lista = new ArrayList<>();
        Set<String> macsVistas = new HashSet<>();
        for (NetworkIF n : hw.getNetworkIFs()) {
            n.updateAttributes();
            if (!esAdaptadorReal(n)) continue;
            if (!macsVistas.add(n.getMacaddr().toLowerCase())) continue; // una sola vez por MAC
            lista.add(new AdaptadorRed(n.getDisplayName(), n.getMacaddr(),
                    Arrays.asList(n.getIPv4addr()), tipoRed(n.getDisplayName()),
                    redondear(n.getSpeed() / 1_000_000.0)));
        }
        return lista;
    }

    private boolean esAdaptadorReal(NetworkIF n) {
        String mac = n.getMacaddr();
        if (mac == null || mac.isBlank() || mac.equalsIgnoreCase("unknown")
                || mac.equals("00:00:00:00:00:00") || n.isKnownVmMacAddr()) return false;
        String texto = (n.getDisplayName() + " " + n.getName()).toLowerCase();
        for (String ruido : RUIDO_RED) {
            if (texto.contains(ruido)) return false;
        }
        return true;
    }

    private String tipoRed(String nombre) {
        String t = nombre == null ? "" : nombre.toLowerCase();
        return (t.contains("wireless") || t.contains("wi-fi") || t.contains("wifi") || t.contains("802.11"))
                ? "Wi-Fi" : "Ethernet";
    }

    private String tipoDisco(String modelo) {
        if (modelo == null) return null;
        String m = modelo.toUpperCase();
        if (m.contains("NVME")) return "NVMe";
        if (m.contains("SSD")) return "SSD";
        return null; // no se puede saber solo con el modelo
    }

    // Quita el texto entre paréntesis del final, como "(Unidades de disco estándar)"
    private String limpiarModelo(String modelo) {
        return modelo == null ? null : limpio(modelo.replaceAll("\\s*\\([^)]*\\)\\s*$", ""));
    }

    private String nombreFabricante(String vendor) {
        if (vendor == null) return null;
        if (vendor.equalsIgnoreCase("AuthenticAMD")) return "AMD";
        if (vendor.equalsIgnoreCase("GenuineIntel")) return "Intel";
        return limpio(vendor);
    }

    private String limpio(String valor) {
        if (valor == null) return null;
        String t = valor.trim();
        return (t.isEmpty() || VACIOS.contains(t.toLowerCase())) ? null : t;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}