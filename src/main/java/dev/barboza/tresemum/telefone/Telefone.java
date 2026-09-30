package dev.barboza.tresemum.telefone;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import dev.barboza.tresemum.papeis.AparelhoTelefonico;
import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.RegraVioladaException;

/**
 * Telefone simulado: nenhuma ligação real é feita. Quem recebe a ligação "atende" depois de alguns
 * segundos (alguns contatos nunca atendem); ligação recebida e não atendida vira chamada perdida
 * e deixa recado na caixa postal. O tempo vem do relógio, e {@link #sincronizar()} aplica o que já aconteceu.
 */
public class Telefone implements AparelhoTelefonico {

    public static final Duration ATENDE_APOS = Duration.ofSeconds(4);
    public static final Duration DESISTE_APOS = Duration.ofSeconds(25);
    public static final Duration TOCA_ATE = Duration.ofSeconds(20);
    public static final int MAXIMO_DE_RECENTES = 30;

    private static final List<String> RECADOS = List.of(
            "Te liguei pra saber se o almoço de domingo continua de pé. Me dá um retorno quando puder, beijo!",
            "Queria te contar uma novidade, mas é melhor falar pessoalmente. Me liga quando der.",
            "Passei pra lembrar da reunião amanhã às nove. Se não puder, me avisa, tá bom?",
            "Estou saindo agora e chego em uns vinte minutos. Qualquer coisa me chama no celular.");

    private final Agenda agenda;
    private final Clock relogio;
    private Chamada atual;
    private final List<Chamada> recentes = new ArrayList<>();
    private final List<Recado> recados = new ArrayList<>();
    private Recado recadoEmReproducao;
    private Instant recentesVistosEm = Instant.EPOCH;
    private long sequencia;

    public Telefone(Agenda agenda, Clock relogio) {
        this.agenda = agenda;
        this.relogio = relogio;
    }

    @Override
    public void ligar(String numero) {
        sincronizar();
        Numero destino = Numero.de(numero);
        if (atual != null) {
            throw new EstadoInvalidoException("Já existe uma chamada em curso.");
        }
        atual = new Chamada(++sequencia, destino, agenda.porNumero(destino).orElse(null),
                Chamada.Direcao.EFETUADA, relogio.instant());
    }

    /** Simula alguém ligando para o aparelho. */
    public void receberChamada(String numero) {
        sincronizar();
        Numero origem = Numero.de(numero);
        if (atual != null) {
            throw new EstadoInvalidoException("A linha está ocupada.");
        }
        atual = new Chamada(++sequencia, origem, agenda.porNumero(origem).orElse(null),
                Chamada.Direcao.RECEBIDA, relogio.instant());
    }

    @Override
    public void atender() {
        sincronizar();
        exigirChamadaTocando();
        atual.atendida(relogio.instant());
    }

    @Override
    public void recusar() {
        sincronizar();
        exigirChamadaTocando();
        Chamada recusada = atual;
        encerrarAtual(Chamada.Desfecho.RECUSADA, relogio.instant());
        deixarRecado(recusada, relogio.instant());
    }

    @Override
    public void encerrar() {
        sincronizar();
        if (atual == null) {
            throw new EstadoInvalidoException("Não há chamada para encerrar.");
        }
        switch (atual.getEstado()) {
            case CHAMANDO -> encerrarAtual(Chamada.Desfecho.CANCELADA, relogio.instant());
            case TOCANDO -> recusar();
            case EM_ANDAMENTO -> encerrarAtual(Chamada.Desfecho.CONCLUIDA, relogio.instant());
            case ENCERRADA -> throw new IllegalStateException("Chamada encerrada não fica como atual.");
        }
    }

    /** Correio de voz: toca o recado não ouvido mais antigo. */
    @Override
    public void iniciarCorreioVoz() {
        Recado proximo = recados.stream()
                .filter(r -> !r.isOuvido())
                .min(Comparator.comparing(Recado::getRecebidoEm))
                .orElseThrow(() -> new EstadoInvalidoException("Nenhum recado novo."));
        ouvir(proximo);
    }

    /** Correio de voz visual: ouve qualquer recado, na ordem que quiser. */
    public void ouvirRecado(long id) {
        ouvir(recado(id));
    }

    public void apagarRecado(long id) {
        Recado recado = recado(id);
        recados.remove(recado);
        if (recado == recadoEmReproducao) {
            recadoEmReproducao = null;
        }
    }

    public void alternarMudo() {
        exigirChamada().alternarMudo();
    }

    public void alternarVivaVoz() {
        exigirChamada().alternarVivaVoz();
    }

    public void enviarTom(String tecla) {
        exigirChamada().enviarTom(tecla);
    }

    /** Aplica o que o tempo já decidiu: o outro lado atendeu, desistiu ou a ligação recebida caiu. */
    public void sincronizar() {
        if (atual == null) {
            return;
        }
        Instant inicio = atual.getInicio();
        Duration decorrido = Duration.between(inicio, relogio.instant());
        switch (atual.getEstado()) {
            case CHAMANDO -> {
                boolean atende = atual.getContato().map(Contato::atende).orElse(true);
                if (atende && decorrido.compareTo(ATENDE_APOS) >= 0) {
                    atual.atendida(inicio.plus(ATENDE_APOS));
                } else if (!atende && decorrido.compareTo(DESISTE_APOS) >= 0) {
                    encerrarAtual(Chamada.Desfecho.NAO_ATENDIDA, inicio.plus(DESISTE_APOS));
                }
            }
            case TOCANDO -> {
                if (decorrido.compareTo(TOCA_ATE) >= 0) {
                    Chamada perdida = atual;
                    encerrarAtual(Chamada.Desfecho.PERDIDA, inicio.plus(TOCA_ATE));
                    deixarRecado(perdida, inicio.plus(TOCA_ATE));
                }
            }
            case EM_ANDAMENTO, ENCERRADA -> { }
        }
    }

    /** Histórico e recados de exemplo, para o aparelho não começar vazio. */
    public void carregarExemplos() {
        Instant agora = relogio.instant();
        registrarPassada("bruno", Chamada.Direcao.EFETUADA, agora.minus(Duration.ofHours(26)), Duration.ofMinutes(12), Chamada.Desfecho.CONCLUIDA);
        registrarPassada("pizzaria", Chamada.Direcao.EFETUADA, agora.minus(Duration.ofHours(20)), Duration.ofSeconds(95), Chamada.Desfecho.CONCLUIDA);
        registrarPassada("mae", Chamada.Direcao.RECEBIDA, agora.minus(Duration.ofHours(5)), Duration.ofMinutes(8), Chamada.Desfecho.CONCLUIDA);
        registrarPassada("carla", Chamada.Direcao.RECEBIDA, agora.minus(Duration.ofHours(3)), Duration.ZERO, Chamada.Desfecho.PERDIDA);
        registrarPassada("ana", Chamada.Direcao.RECEBIDA, agora.minus(Duration.ofMinutes(40)), Duration.ZERO, Chamada.Desfecho.PERDIDA);
        Contato carla = agenda.porId("carla").orElseThrow();
        Contato ana = agenda.porId("ana").orElseThrow();
        recados.addFirst(new Recado(++sequencia, carla.numero(), carla.nome(), agora.minus(Duration.ofHours(3)),
                "Oi, aqui é a Carla, do escritório. O cliente aprovou a proposta! Me liga pra gente combinar os próximos passos."));
        recados.addFirst(new Recado(++sequencia, ana.numero(), ana.nome(), agora.minus(Duration.ofMinutes(40)),
                "Oi, é a Ana! Vi que você está montando um iPhone em Java. Quero ver funcionando, me liga!"));
    }

    private void registrarPassada(String contatoId, Chamada.Direcao direcao, Instant inicio, Duration duracao, Chamada.Desfecho desfecho) {
        Contato contato = agenda.porId(contatoId).orElseThrow();
        Chamada chamada = new Chamada(++sequencia, contato.numero(), contato, direcao, inicio);
        if (!duracao.isZero()) {
            chamada.atendida(inicio.plusSeconds(5));
        }
        chamada.encerrada(desfecho, inicio.plusSeconds(5).plus(duracao));
        recentes.addFirst(chamada);
    }

    private void ouvir(Recado recado) {
        recado.marcarOuvido();
        recadoEmReproducao = recado;
    }

    private void encerrarAtual(Chamada.Desfecho desfecho, Instant quando) {
        atual.encerrada(desfecho, quando);
        recentes.addFirst(atual);
        if (recentes.size() > MAXIMO_DE_RECENTES) {
            recentes.removeLast();
        }
        atual = null;
    }

    private void deixarRecado(Chamada chamada, Instant quando) {
        String quem = chamada.getContato().map(c -> "Oi, aqui é " + primeiroNome(c) + ". ").orElse("Oi, tudo bem? ");
        String texto = quem + RECADOS.get((int) (chamada.getId() % RECADOS.size()));
        recados.addFirst(new Recado(++sequencia, chamada.getNumero(), chamada.nomeOuNumero(), quando.plusSeconds(30), texto));
    }

    private static String primeiroNome(Contato c) {
        return c.nome().split(" ")[0];
    }

    private Chamada exigirChamada() {
        sincronizar();
        if (atual == null) {
            throw new EstadoInvalidoException("Não há chamada em curso.");
        }
        return atual;
    }

    private void exigirChamadaTocando() {
        if (atual == null || atual.getEstado() != Chamada.Estado.TOCANDO) {
            throw new EstadoInvalidoException("Não há chamada tocando.");
        }
    }

    private Recado recado(long id) {
        return recados.stream().filter(r -> r.getId() == id).findFirst()
                .orElseThrow(() -> new RegraVioladaException("Recado não encontrado."));
    }

    public Optional<Chamada> getChamadaAtual() {
        sincronizar();
        return Optional.ofNullable(atual);
    }

    public List<Chamada> getRecentes() {
        sincronizar();
        return List.copyOf(recentes);
    }

    public List<Recado> getRecados() {
        sincronizar();
        return List.copyOf(recados);
    }

    public long recadosNaoOuvidos() {
        sincronizar();
        return recados.stream().filter(r -> !r.isOuvido()).count();
    }

    /** Chamadas perdidas desde a última vez que a lista de recentes foi aberta (o número no ícone). */
    public long chamadasPerdidas() {
        sincronizar();
        return recentes.stream()
                .filter(c -> c.getDesfecho().orElse(null) == Chamada.Desfecho.PERDIDA)
                .filter(c -> c.getInicio().isAfter(recentesVistosEm))
                .count();
    }

    public void marcarRecentesComoVistos() {
        recentesVistosEm = relogio.instant();
    }

    public Optional<Recado> getRecadoEmReproducao() {
        return Optional.ofNullable(recadoEmReproducao);
    }

    public Agenda getAgenda() {
        return agenda;
    }
}
