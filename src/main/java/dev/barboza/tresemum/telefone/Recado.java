package dev.barboza.tresemum.telefone;

import java.time.Duration;
import java.time.Instant;

/** Mensagem na caixa postal. No correio de voz visual, pode ser ouvida em qualquer ordem. */
public class Recado {

    private final long id;
    private final Numero de;
    private final String nome;
    private final Instant recebidoEm;
    private final String transcricao;
    private boolean ouvido;

    Recado(long id, Numero de, String nome, Instant recebidoEm, String transcricao) {
        this.id = id;
        this.de = de;
        this.nome = nome;
        this.recebidoEm = recebidoEm;
        this.transcricao = transcricao;
    }

    /** Duração estimada pela fala: cerca de 2,5 palavras por segundo. */
    public Duration duracao() {
        int palavras = transcricao.trim().split("\\s+").length;
        return Duration.ofSeconds(Math.max(3, Math.round(palavras / 2.5)));
    }

    void marcarOuvido() {
        ouvido = true;
    }

    public long getId() {
        return id;
    }

    public Numero getDe() {
        return de;
    }

    public String getNome() {
        return nome;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }

    public String getTranscricao() {
        return transcricao;
    }

    public boolean isOuvido() {
        return ouvido;
    }
}
