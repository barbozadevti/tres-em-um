package dev.barboza.tresemum.navegador;

import java.util.List;

/** Pedaço de uma página da "internet" do simulador. A API converte cada tipo com um switch exaustivo. */
public sealed interface Bloco {

    record Cabecalho(String titulo, String subtitulo) implements Bloco { }

    record Paragrafo(String texto) implements Bloco { }

    record Lista(String titulo, List<String> itens, boolean numerada) implements Bloco { }

    /** Número grande com legenda (temperatura, placar...). */
    record Destaque(String valor, String legenda) implements Bloco { }

    record Cartoes(String titulo, List<Cartao> itens) implements Bloco { }

    record Cartao(String titulo, String texto, String url) { }
}
