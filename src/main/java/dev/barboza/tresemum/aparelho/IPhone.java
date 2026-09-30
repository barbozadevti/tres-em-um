package dev.barboza.tresemum.aparelho;

import java.time.Clock;
import java.util.Random;
import java.util.function.Function;

import dev.barboza.tresemum.musica.Biblioteca;
import dev.barboza.tresemum.musica.Reprodutor;
import dev.barboza.tresemum.navegador.Navegador;
import dev.barboza.tresemum.navegador.Web;
import dev.barboza.tresemum.papeis.AparelhoTelefonico;
import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.NavegadorInternet;
import dev.barboza.tresemum.papeis.ReprodutorMusical;
import dev.barboza.tresemum.telefone.Agenda;
import dev.barboza.tresemum.telefone.Telefone;

/**
 * "Um iPod, um telefone e um navegador. Não são três aparelhos: é um só."
 * <p>
 * O iPhone assume os três papéis por composição: cada interface é atendida por um objeto especialista
 * ({@link Reprodutor}, {@link Telefone}, {@link Navegador}), acessado por um proxy que registra as chamadas.
 * O que o aparelho acrescenta é a coordenação entre os papéis: a música pausa quando uma ligação
 * começa e volta sozinha quando ela termina, como na apresentação de 2007.
 * <p>
 * Métodos sincronizados: cada visitante tem o próprio aparelho, mas pode haver pedidos simultâneos.
 */
public class IPhone implements ReprodutorMusical, AparelhoTelefonico, NavegadorInternet {

    private final Reprodutor reprodutor;
    private final Telefone telefone;
    private final Navegador navegador;
    private final Rastro rastro;

    private final ReprodutorMusical comoReprodutor;
    private final AparelhoTelefonico comoTelefone;
    private final NavegadorInternet comoNavegador;

    /** A música foi pausada por uma ligação e deve voltar quando ela terminar. */
    private boolean musicaInterrompida;

    public IPhone(Clock relogio, Random sorteio) {
        this.rastro = new Rastro(relogio);
        this.reprodutor = new Reprodutor(new Biblioteca(), relogio, sorteio);
        this.telefone = new Telefone(Agenda.exemplo(), relogio);
        this.navegador = new Navegador(new Web(), relogio);
        this.comoReprodutor = Rastreador.rastrear(ReprodutorMusical.class, reprodutor, rastro);
        this.comoTelefone = Rastreador.rastrear(AparelhoTelefonico.class, telefone, rastro);
        this.comoNavegador = Rastreador.rastrear(NavegadorInternet.class, navegador, rastro);
    }

    /** Aparelho com histórico de ligações e recados, para a demonstração. */
    public static IPhone comExemplos(Clock relogio, Random sorteio) {
        IPhone iphone = new IPhone(relogio, sorteio);
        iphone.telefone.carregarExemplos();
        return iphone;
    }

    /** Executa várias operações de uma vez, sem outro pedido no meio (usado pela API). */
    public synchronized <T> T executar(Function<IPhone, T> acao) {
        sincronizar();
        return acao.apply(this);
    }

    // ----- Reprodutor musical -----

    @Override
    public synchronized void tocar() {
        exigirSemChamada();
        comoReprodutor.tocar();
        musicaInterrompida = false;
    }

    @Override
    public synchronized void pausar() {
        sincronizar();
        comoReprodutor.pausar();
        musicaInterrompida = false;
    }

    @Override
    public synchronized void selecionarMusica(String musica) {
        sincronizar();
        comoReprodutor.selecionarMusica(musica);
    }

    @Override
    public synchronized void proxima() {
        sincronizar();
        comoReprodutor.proxima();
    }

    @Override
    public synchronized void anterior() {
        sincronizar();
        comoReprodutor.anterior();
    }

    /** Tocar numa música da lista: seleciona e toca. */
    public synchronized void tocarMusica(String musica) {
        exigirSemChamada();
        comoReprodutor.selecionarMusica(musica);
        if (reprodutor.getEstado() != Reprodutor.Estado.TOCANDO) {
            comoReprodutor.tocar();
        }
        musicaInterrompida = false;
    }

    // ----- Aparelho telefônico -----

    @Override
    public synchronized void ligar(String numero) {
        sincronizar();
        comoTelefone.ligar(numero);
        interromperMusica("ligação iniciada");
    }

    @Override
    public synchronized void atender() {
        sincronizar();
        comoTelefone.atender();
    }

    @Override
    public synchronized void iniciarCorreioVoz() {
        sincronizar();
        comoTelefone.iniciarCorreioVoz();
        if (reprodutor.getEstado() == Reprodutor.Estado.TOCANDO) {
            rastro.comMotivo("ouvindo recado", comoReprodutor::pausar);
        }
    }

    @Override
    public synchronized void recusar() {
        sincronizar();
        comoTelefone.recusar();
        sincronizar();
    }

    @Override
    public synchronized void encerrar() {
        sincronizar();
        comoTelefone.encerrar();
        sincronizar();
    }

    /** Simula alguém ligando para este aparelho. */
    public synchronized void receberChamada(String numero) {
        sincronizar();
        telefone.receberChamada(numero);
        rastro.registrar("Rede", "chamadaRecebida", new Object[] {telefone.getChamadaAtual().orElseThrow().getNumero().formatado()}, null);
        interromperMusica("chamada recebida");
    }

    // ----- Navegador na internet -----

    @Override
    public synchronized void exibirPagina(String url) {
        comoNavegador.exibirPagina(url);
    }

    @Override
    public synchronized void adicionarNovaAba() {
        comoNavegador.adicionarNovaAba();
    }

    @Override
    public synchronized void atualizarPagina() {
        comoNavegador.atualizarPagina();
    }

    @Override
    public synchronized void voltar() {
        comoNavegador.voltar();
    }

    @Override
    public synchronized void avancar() {
        comoNavegador.avancar();
    }

    // ----- Coordenação entre os papéis -----

    /** Aplica o tempo que passou e, se a ligação acabou, devolve a música. */
    public synchronized void sincronizar() {
        reprodutor.sincronizar();
        telefone.sincronizar();
        if (musicaInterrompida && telefone.getChamadaAtual().isEmpty()) {
            musicaInterrompida = false;
            rastro.comMotivo("ligação encerrada", comoReprodutor::tocar);
        }
    }

    private void interromperMusica(String motivo) {
        if (reprodutor.getEstado() == Reprodutor.Estado.TOCANDO) {
            rastro.comMotivo(motivo, comoReprodutor::pausar);
            musicaInterrompida = true;
        }
    }

    private void exigirSemChamada() {
        sincronizar();
        if (telefone.getChamadaAtual().isPresent()) {
            throw new EstadoInvalidoException("Termine a ligação para ouvir música.");
        }
    }

    public boolean isMusicaInterrompida() {
        return musicaInterrompida;
    }

    public Reprodutor getReprodutor() {
        return reprodutor;
    }

    public Telefone getTelefone() {
        return telefone;
    }

    public Navegador getNavegador() {
        return navegador;
    }

    public Rastro getRastro() {
        return rastro;
    }
}
