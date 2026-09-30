package dev.barboza.tresemum.navegador;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.NavegadorInternet;
import dev.barboza.tresemum.papeis.RegraVioladaException;

/**
 * Navegador com abas (até 8), abas privadas, histórico por aba e favoritos.
 * Abas privadas não entram no histórico de páginas visitadas.
 */
public class Navegador implements NavegadorInternet {

    public static final int MAXIMO_DE_ABAS = 8;
    public static final int MAXIMO_DE_VISITADAS = 20;

    private final Web web;
    private final Clock relogio;
    private final List<Aba> abas = new ArrayList<>();
    private final Set<String> favoritos = new LinkedHashSet<>();
    private final List<String> visitadas = new ArrayList<>();
    private Aba atual;
    private int sequencia;

    public Navegador(Web web, Clock relogio) {
        this.web = web;
        this.relogio = relogio;
        abrirAba(false);
        favoritos.add("https://noticias.exemplo/");
        favoritos.add("https://clima.exemplo/");
        favoritos.add("https://keynote.exemplo/");
        favoritos.add("https://barboza.dev/");
    }

    @Override
    public void exibirPagina(String url) {
        String endereco = Endereco.interpretar(url);
        atual.navegar(endereco, relogio.instant());
        registrarVisita(endereco);
    }

    @Override
    public void adicionarNovaAba() {
        abrirAba(false);
    }

    public void adicionarAbaPrivada() {
        abrirAba(true);
    }

    @Override
    public void atualizarPagina() {
        atual.atualizar(relogio.instant());
    }

    @Override
    public void voltar() {
        atual.voltar(relogio.instant());
    }

    @Override
    public void avancar() {
        atual.avancar(relogio.instant());
    }

    public void selecionarAba(int id) {
        atual = aba(id);
    }

    /** Fecha a aba; fechar a última abre uma nova em branco, como no celular. */
    public void fecharAba(int id) {
        Aba aba = aba(id);
        int indice = abas.indexOf(aba);
        abas.remove(aba);
        if (abas.isEmpty()) {
            abrirAba(false);
        } else if (aba == atual) {
            atual = abas.get(Math.min(indice, abas.size() - 1));
        }
    }

    /** Adiciona ou tira a página atual dos favoritos. */
    public void alternarFavorito() {
        String url = atual.getUrl();
        if (Endereco.PAGINA_INICIAL.equals(url)) {
            throw new EstadoInvalidoException("Abra uma página para favoritar.");
        }
        if (!favoritos.remove(url)) {
            favoritos.add(url);
        }
    }

    public Pagina paginaAtual() {
        return web.abrir(atual.getUrl());
    }

    public Pagina pagina(String url) {
        return web.abrir(url);
    }

    private void abrirAba(boolean privada) {
        if (abas.size() >= MAXIMO_DE_ABAS) {
            throw new RegraVioladaException("Limite de " + MAXIMO_DE_ABAS + " abas. Feche uma para abrir outra.");
        }
        atual = new Aba(++sequencia, privada, relogio.instant());
        abas.add(atual);
    }

    private void registrarVisita(String url) {
        if (atual.isPrivada() || url.startsWith(Endereco.BUSCA)) {
            return;
        }
        visitadas.remove(url);
        visitadas.addFirst(url);
        if (visitadas.size() > MAXIMO_DE_VISITADAS) {
            visitadas.removeLast();
        }
    }

    private Aba aba(int id) {
        return abas.stream().filter(a -> a.getId() == id).findFirst()
                .orElseThrow(() -> new RegraVioladaException("Aba não encontrada."));
    }

    public List<Aba> getAbas() {
        return List.copyOf(abas);
    }

    public Aba getAbaAtual() {
        return atual;
    }

    public List<String> getFavoritos() {
        return List.copyOf(favoritos);
    }

    public boolean isFavorita() {
        return favoritos.contains(atual.getUrl());
    }

    public List<String> getVisitadas() {
        return List.copyOf(visitadas);
    }
}
