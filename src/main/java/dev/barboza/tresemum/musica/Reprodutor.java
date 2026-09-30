package dev.barboza.tresemum.musica;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.RegraVioladaException;
import dev.barboza.tresemum.papeis.ReprodutorMusical;

/**
 * Reprodutor com fila, modo aleatório e repetição. A posição não é guardada a cada segundo:
 * é calculada pelo relógio (tempo acumulado + tempo desde o último "play"), e {@link #sincronizar()}
 * passa para a próxima faixa quando a atual termina.
 */
public class Reprodutor implements ReprodutorMusical {

    public enum Estado { PARADO, TOCANDO, PAUSADO }

    public enum Repeticao {
        DESLIGADA, TODAS, UMA;

        Repeticao seguinte() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Em "anterior", depois deste tempo a faixa recomeça em vez de voltar para a anterior. */
    public static final Duration LIMITE_PARA_RECOMECAR = Duration.ofSeconds(3);

    private final Biblioteca biblioteca;
    private final Clock relogio;
    private final Random sorteio;

    private List<Faixa> fila;
    private int indice = -1;
    private Estado estado = Estado.PARADO;
    private Duration acumulado = Duration.ZERO;
    private Instant desde;
    private boolean aleatorio;
    private Repeticao repeticao = Repeticao.DESLIGADA;

    public Reprodutor(Biblioteca biblioteca, Clock relogio, Random sorteio) {
        this.biblioteca = biblioteca;
        this.relogio = relogio;
        this.sorteio = sorteio;
        this.fila = new ArrayList<>(biblioteca.todas());
    }

    @Override
    public void selecionarMusica(String musica) {
        Faixa faixa = biblioteca.porId(musica)
                .orElseThrow(() -> new RegraVioladaException("Música não encontrada: " + musica));
        sincronizar();
        indice = fila.indexOf(faixa);
        reiniciarFaixa();
        if (estado == Estado.PAUSADO) {
            estado = Estado.PARADO;
        }
    }

    @Override
    public void tocar() {
        sincronizar();
        if (estado == Estado.TOCANDO) {
            throw new EstadoInvalidoException("A música já está tocando.");
        }
        if (indice < 0) {
            indice = 0;
        }
        estado = Estado.TOCANDO;
        desde = relogio.instant();
    }

    @Override
    public void pausar() {
        sincronizar();
        if (estado != Estado.TOCANDO) {
            throw new EstadoInvalidoException("Nada está tocando.");
        }
        acumulado = posicao();
        estado = Estado.PAUSADO;
    }

    @Override
    public void proxima() {
        sincronizar();
        exigirFaixa();
        boolean ultima = indice == fila.size() - 1;
        indice = (indice + 1) % fila.size();
        reiniciarFaixa();
        if (ultima && repeticao == Repeticao.DESLIGADA && estado == Estado.TOCANDO) {
            estado = Estado.PARADO;
        }
    }

    @Override
    public void anterior() {
        sincronizar();
        exigirFaixa();
        if (posicao().compareTo(LIMITE_PARA_RECOMECAR) <= 0) {
            indice = (indice - 1 + fila.size()) % fila.size();
        }
        reiniciarFaixa();
    }

    /** Pula para um ponto da faixa atual (a barra de progresso). */
    public void buscar(Duration ponto) {
        sincronizar();
        exigirFaixa();
        Duration limite = fila.get(indice).duracao();
        if (ponto.isNegative() || ponto.compareTo(limite) > 0) {
            throw new RegraVioladaException("Posição fora da música.");
        }
        acumulado = ponto;
        desde = relogio.instant();
        if (estado == Estado.PARADO) {
            estado = Estado.PAUSADO;
        }
    }

    /** Liga ou desliga o aleatório. A faixa atual continua tocando e vira a primeira da nova fila. */
    public void alternarAleatorio() {
        sincronizar();
        Faixa atual = faixaAtual().orElse(null);
        aleatorio = !aleatorio;
        fila = new ArrayList<>(biblioteca.todas());
        if (aleatorio) {
            Collections.shuffle(fila, sorteio);
            if (atual != null) {
                fila.remove(atual);
                fila.addFirst(atual);
            }
        }
        indice = atual == null ? -1 : fila.indexOf(atual);
    }

    public void alternarRepeticao() {
        repeticao = repeticao.seguinte();
    }

    /** Avança a fila conforme o tempo que passou; chamado antes de qualquer leitura ou ação. */
    public void sincronizar() {
        while (estado == Estado.TOCANDO) {
            Duration posicao = posicao();
            Duration duracao = fila.get(indice).duracao();
            if (posicao.compareTo(duracao) < 0) {
                return;
            }
            Duration sobra = posicao.minus(duracao);
            if (repeticao != Repeticao.UMA) {
                if (indice == fila.size() - 1 && repeticao == Repeticao.DESLIGADA) {
                    indice = 0;
                    reiniciarFaixa();
                    estado = Estado.PARADO;
                    return;
                }
                indice = (indice + 1) % fila.size();
            }
            acumulado = sobra;
            desde = relogio.instant();
        }
    }

    public Duration posicao() {
        if (estado == Estado.TOCANDO) {
            return acumulado.plus(Duration.between(desde, relogio.instant()));
        }
        return acumulado;
    }

    public Optional<Faixa> faixaAtual() {
        return indice < 0 ? Optional.empty() : Optional.of(fila.get(indice));
    }

    public Estado getEstado() {
        return estado;
    }

    public boolean isAleatorio() {
        return aleatorio;
    }

    public Repeticao getRepeticao() {
        return repeticao;
    }

    public List<Faixa> getFila() {
        return List.copyOf(fila);
    }

    public Biblioteca getBiblioteca() {
        return biblioteca;
    }

    private void reiniciarFaixa() {
        acumulado = Duration.ZERO;
        desde = relogio.instant();
    }

    private void exigirFaixa() {
        if (indice < 0) {
            throw new EstadoInvalidoException("Nenhuma música selecionada.");
        }
    }
}
