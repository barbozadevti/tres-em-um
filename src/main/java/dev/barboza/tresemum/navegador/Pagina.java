package dev.barboza.tresemum.navegador;

import java.util.List;

/** Página carregada numa aba. */
public record Pagina(String url, String titulo, Tipo tipo, String cor, List<Bloco> blocos) {

    public enum Tipo { INICIO, SITE, BUSCA, ERRO }

    public static Pagina inicio() {
        return new Pagina(Endereco.PAGINA_INICIAL, "Página inicial", Tipo.INICIO, "#6366f1", List.of());
    }

    public static Pagina erro(String url, String titulo, String mensagem) {
        return new Pagina(url, titulo, Tipo.ERRO, "#64748b", List.of(new Bloco.Paragrafo(mensagem)));
    }
}
