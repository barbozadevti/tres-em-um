package dev.barboza.tresemum.navegador;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import dev.barboza.tresemum.papeis.RegraVioladaException;

/**
 * Interpreta o que se digita na barra de endereços, como um navegador de verdade:
 * "clima.exemplo" vira "https://clima.exemplo/"; "moqueca capixaba" vira uma busca;
 * esquemas perigosos como "javascript:" são recusados.
 */
public final class Endereco {

    public static final String PAGINA_INICIAL = "about:blank";
    public static final String BUSCA = "https://busca.exemplo/?q=";

    private Endereco() {
    }

    public static String interpretar(String digitado) {
        if (digitado == null || digitado.isBlank()) {
            throw new RegraVioladaException("Digite um endereço ou o que quer buscar.");
        }
        String texto = digitado.strip();
        if (texto.length() > 300) {
            throw new RegraVioladaException("Endereço longo demais.");
        }
        if (texto.equalsIgnoreCase(PAGINA_INICIAL)) {
            return PAGINA_INICIAL;
        }
        String esquema = esquema(texto);
        if (esquema != null && !esquema.equals("http") && !esquema.equals("https")) {
            throw new RegraVioladaException("Endereço não permitido: " + esquema + ":");
        }
        boolean pareceEndereco = esquema != null || texto.toLowerCase(Locale.ROOT).startsWith("localhost")
                || (!texto.contains(" ") && texto.contains(".") && !texto.endsWith("."));
        if (!pareceEndereco) {
            return BUSCA + URLEncoder.encode(texto, StandardCharsets.UTF_8);
        }
        String comEsquema = esquema == null ? "https://" + texto : texto;
        try {
            URI uri = new URI(comEsquema);
            if (uri.getHost() == null) {
                throw new RegraVioladaException("Endereço inválido: " + texto);
            }
            String caminho = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            String consulta = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "") + caminho + consulta;
        } catch (URISyntaxException e) {
            // Não é um endereço válido (ex.: "a.b c"): trata como busca.
            return BUSCA + URLEncoder.encode(texto, StandardCharsets.UTF_8);
        }
    }

    /** Domínio para mostrar na barra, sem "https://" nem "www.". */
    public static String dominio(String url) {
        if (PAGINA_INICIAL.equals(url)) {
            return "";
        }
        String host = URI.create(url).getHost();
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private static String esquema(String texto) {
        int doisPontos = texto.indexOf(':');
        if (doisPontos <= 0) {
            return null;
        }
        String candidato = texto.substring(0, doisPontos).toLowerCase(Locale.ROOT);
        if (!candidato.matches("[a-z][a-z0-9+.-]*")) {
            return null;
        }
        // "localhost:8080" ou "site.com:8080" não são esquemas.
        String resto = texto.substring(doisPontos + 1);
        if (!resto.startsWith("//") && resto.matches("\\d+(/.*)?")) {
            return null;
        }
        return candidato;
    }
}
