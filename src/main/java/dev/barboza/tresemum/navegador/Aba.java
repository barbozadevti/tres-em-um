package dev.barboza.tresemum.navegador;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import dev.barboza.tresemum.papeis.EstadoInvalidoException;

/** Uma aba com o próprio histórico de voltar e avançar. */
public class Aba {

    private final int id;
    private final boolean privada;
    private final List<String> historico = new ArrayList<>();
    private int posicao;
    private int atualizacoes;
    private Instant carregadaEm;

    Aba(int id, boolean privada, Instant agora) {
        this.id = id;
        this.privada = privada;
        historico.add(Endereco.PAGINA_INICIAL);
        carregadaEm = agora;
    }

    /** Abrir um endereço descarta o que havia "para frente", como em qualquer navegador. */
    void navegar(String url, Instant agora) {
        if (url.equals(getUrl())) {
            atualizar(agora);
            return;
        }
        historico.subList(posicao + 1, historico.size()).clear();
        historico.add(url);
        posicao = historico.size() - 1;
        atualizacoes = 0;
        carregadaEm = agora;
    }

    void voltar(Instant agora) {
        if (!podeVoltar()) {
            throw new EstadoInvalidoException("Não há página anterior.");
        }
        posicao--;
        atualizacoes = 0;
        carregadaEm = agora;
    }

    void avancar(Instant agora) {
        if (!podeAvancar()) {
            throw new EstadoInvalidoException("Não há página seguinte.");
        }
        posicao++;
        atualizacoes = 0;
        carregadaEm = agora;
    }

    void atualizar(Instant agora) {
        atualizacoes++;
        carregadaEm = agora;
    }

    public boolean podeVoltar() {
        return posicao > 0;
    }

    public boolean podeAvancar() {
        return posicao < historico.size() - 1;
    }

    public String getUrl() {
        return historico.get(posicao);
    }

    public int getId() {
        return id;
    }

    public boolean isPrivada() {
        return privada;
    }

    public List<String> getHistorico() {
        return List.copyOf(historico);
    }

    public int getAtualizacoes() {
        return atualizacoes;
    }

    public Instant getCarregadaEm() {
        return carregadaEm;
    }
}
