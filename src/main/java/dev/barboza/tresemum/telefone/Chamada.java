package dev.barboza.tresemum.telefone;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.RegraVioladaException;

/** Uma ligação, feita ou recebida, com os estados validados por uma máquina de estados. */
public class Chamada {

    public enum Direcao { EFETUADA, RECEBIDA }

    public enum Estado {
        /** Ligação feita, esperando o outro lado atender. */
        CHAMANDO,
        /** Ligação recebida, tocando no aparelho. */
        TOCANDO,
        EM_ANDAMENTO,
        ENCERRADA;

        public Set<Estado> proximos() {
            return switch (this) {
                case CHAMANDO, TOCANDO -> EnumSet.of(EM_ANDAMENTO, ENCERRADA);
                case EM_ANDAMENTO -> EnumSet.of(ENCERRADA);
                case ENCERRADA -> EnumSet.noneOf(Estado.class);
            };
        }
    }

    public enum Desfecho {
        CONCLUIDA("Concluída"),
        NAO_ATENDIDA("Não atendida"),
        CANCELADA("Cancelada"),
        PERDIDA("Perdida"),
        RECUSADA("Recusada");

        private final String nome;

        Desfecho(String nome) {
            this.nome = nome;
        }

        public String nome() {
            return nome;
        }
    }

    private final long id;
    private final Numero numero;
    private final Contato contato;
    private final Direcao direcao;
    private final Instant inicio;
    private Estado estado;
    private Instant atendidaEm;
    private Instant encerradaEm;
    private Desfecho desfecho;
    private boolean mudo;
    private boolean vivaVoz;
    private final StringBuilder tons = new StringBuilder();

    Chamada(long id, Numero numero, Contato contato, Direcao direcao, Instant inicio) {
        this.id = id;
        this.numero = numero;
        this.contato = contato;
        this.direcao = direcao;
        this.inicio = inicio;
        this.estado = direcao == Direcao.EFETUADA ? Estado.CHAMANDO : Estado.TOCANDO;
    }

    void atendida(Instant quando) {
        ir(Estado.EM_ANDAMENTO);
        atendidaEm = quando;
    }

    void encerrada(Desfecho desfecho, Instant quando) {
        ir(Estado.ENCERRADA);
        this.desfecho = desfecho;
        encerradaEm = quando;
    }

    private void ir(Estado novo) {
        if (!estado.proximos().contains(novo)) {
            throw new EstadoInvalidoException("A chamada não pode passar de " + estado + " para " + novo + ".");
        }
        estado = novo;
    }

    void alternarMudo() {
        exigirEmAndamento();
        mudo = !mudo;
    }

    void alternarVivaVoz() {
        if (estado == Estado.ENCERRADA) {
            throw new EstadoInvalidoException("A chamada já terminou.");
        }
        vivaVoz = !vivaVoz;
    }

    /** Tom do teclado durante a chamada (ex.: menu "digite 1 para..."). */
    void enviarTom(String tecla) {
        exigirEmAndamento();
        if (tecla == null || !tecla.matches("[0-9*#]")) {
            throw new RegraVioladaException("Tecla inválida: use 0 a 9, * ou #.");
        }
        tons.append(tecla);
    }

    private void exigirEmAndamento() {
        if (estado != Estado.EM_ANDAMENTO) {
            throw new EstadoInvalidoException("Só durante a chamada.");
        }
    }

    public Duration duracao(Instant agora) {
        if (atendidaEm == null) {
            return Duration.ZERO;
        }
        return Duration.between(atendidaEm, encerradaEm != null ? encerradaEm : agora);
    }

    public String nomeOuNumero() {
        return contato != null ? contato.nome() : numero.formatado();
    }

    public long getId() {
        return id;
    }

    public Numero getNumero() {
        return numero;
    }

    public Optional<Contato> getContato() {
        return Optional.ofNullable(contato);
    }

    public Direcao getDirecao() {
        return direcao;
    }

    public Estado getEstado() {
        return estado;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Optional<Instant> getAtendidaEm() {
        return Optional.ofNullable(atendidaEm);
    }

    public Optional<Desfecho> getDesfecho() {
        return Optional.ofNullable(desfecho);
    }

    public boolean isMudo() {
        return mudo;
    }

    public boolean isVivaVoz() {
        return vivaVoz;
    }

    public String getTons() {
        return tons.toString();
    }
}
