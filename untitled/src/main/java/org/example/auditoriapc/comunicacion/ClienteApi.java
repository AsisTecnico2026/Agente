package org.example.auditoriapc.comunicacion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.auditoriapc.config.Configuracion;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;

public class ClienteApi {

    public static class ApiException extends Exception {
        public final int codigo;

        public ApiException(int codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }
    }

    private static final Gson GSON = new Gson();
    private final String base;
    private final HttpClient http;

    public ClienteApi(String apiUrl) {
        this.base = apiUrl.replaceAll("/+$", "");
        exigirCifrado(this.base);
        try {
            this.http = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(10))
                    .sslContext(contextoConfiable())
                    .sslParameters(parametrosTls())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo preparar la conexión segura: " + e.getMessage(), e);
        }
    }

    // Los datos solo viajan por HTTPS. HTTP se acepta únicamente en el mismo equipo (pruebas)
    // o si se define la variable de entorno SFMANTTO_PERMITIR_HTTP=true
    private static void exigirCifrado(String url) {
        String u = url.toLowerCase();
        boolean https = u.startsWith("https://");
        boolean local = u.startsWith("http://localhost") || u.startsWith("http://127.0.0.1");
        boolean permitido = "true".equalsIgnoreCase(System.getenv("SFMANTTO_PERMITIR_HTTP"));
        if (!https && !local && !permitido) {
            throw new IllegalStateException(
                    "La dirección de la API debe empezar con https:// para que los datos viajen cifrados.");
        }
    }

    // Si existe ca.crt junto a la configuración, solo se confía en esa autoridad certificadora
    private static SSLContext contextoConfiable() throws Exception {
        Path ca = Configuracion.archivo().resolveSibling("ca.crt");
        if (!Files.exists(ca)) return SSLContext.getDefault();

        CertificateFactory fabrica = CertificateFactory.getInstance("X.509");
        Certificate certificado;
        try (InputStream in = Files.newInputStream(ca)) {
            certificado = fabrica.generateCertificate(in);
        }
        KeyStore almacen = KeyStore.getInstance(KeyStore.getDefaultType());
        almacen.load(null, null);
        almacen.setCertificateEntry("sfmantto-ca", certificado);

        TrustManagerFactory confianza = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        confianza.init(almacen);
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(null, confianza.getTrustManagers(), null);
        return contexto;
    }

    private static SSLParameters parametrosTls() {
        SSLParameters p = new SSLParameters();
        p.setProtocols(new String[] {"TLSv1.3", "TLSv1.2"});
        p.setEndpointIdentificationAlgorithm("HTTPS"); // exige que la dirección esté en el certificado
        return p;
    }

    public JsonObject activar(String clave, String identificador, String nombrePc, String version)
            throws IOException, InterruptedException, ApiException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("clave", clave);
        cuerpo.addProperty("identificador", identificador);
        cuerpo.addProperty("nombre_pc", nombrePc);
        cuerpo.addProperty("version_agente", version);
        return enviar("/agente/activar", cuerpo, null);
    }

    public JsonObject enviarInventario(String token, JsonObject inventario)
            throws IOException, InterruptedException, ApiException {
        return enviar("/agente/inventario", inventario, token);
    }

    // Pregunta si alguien pidió una auditoría desde la web: responde { "auditar": true o false }
    public JsonObject consultarTareas(String token)
            throws IOException, InterruptedException, ApiException {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(base + "/agente/tareas"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .GET();
        return responder(peticion);
    }

    private JsonObject enviar(String ruta, JsonObject cuerpo, String token)
            throws IOException, InterruptedException, ApiException {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(cuerpo), StandardCharsets.UTF_8));
        if (token != null) peticion.header("Authorization", "Bearer " + token);
        return responder(peticion);
    }

    private JsonObject responder(HttpRequest.Builder peticion)
            throws IOException, InterruptedException, ApiException {
        HttpResponse<String> r = http.send(peticion.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        JsonObject json;
        try {
            json = JsonParser.parseString(r.body()).getAsJsonObject();
        } catch (Exception e) {
            json = new JsonObject();
        }
        if (r.statusCode() >= 300) {
            String mensaje = json.has("error") ? json.get("error").getAsString() : "Error " + r.statusCode();
            throw new ApiException(r.statusCode(), mensaje);
        }
        return json;
    }
}