package dev.barboza.tresemum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.barboza.tresemum.musica.Biblioteca;
import dev.barboza.tresemum.musica.Faixa;
import dev.barboza.tresemum.musica.Partitura;
import dev.barboza.tresemum.musica.Reprodutor;
import dev.barboza.tresemum.musica.Reprodutor.Estado;
import dev.barboza.tresemum.musica.Reprodutor.Repeticao;
import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.RegraVioladaException;

class ReprodutorTest {

    private final Biblioteca biblioteca = new Biblioteca();
    private RelogioAjustavel relogio;
    private Reprodutor reprodutor;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel();
        reprodutor = new Reprodutor(biblioteca, relogio, new Random(7));
    }

    private Faixa faixa(int i) {
        return biblioteca.todas().get(i);
    }

    private double segundos(Duration d) {
        return d.toMillis() / 1000.0;
    }

    @Test
    void partituraSomaAsBatidasEDuracaoVemDoAndamento() {
        assertThat(Partitura.batidas("C4:1 D4:0.5 R:0.5 E4:2")).isEqualTo(4.0);
        // 4 batidas a 120 bpm = 2 segundos.
        var faixa = new Faixa("t", "Teste", "Autor", "2026", 120, "C4:1 D4:1 E4:1 F4:1", "", "#000", "#fff");
        assertThat(faixa.duracao()).isEqualTo(Duration.ofSeconds(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"H4:1", "C4", "C4:0", "C9:1", "C4:-1", "Db4:1"})
    void partituraRecusaNotaMalEscrita(String nota) {
        assertThatThrownBy(() -> Partitura.batidas("C4:1 " + nota)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void todasAsMusicasDaBibliotecaSaoValidasETemEntre15e60Segundos() {
        assertThat(biblioteca.todas()).hasSize(6);
        assertThat(biblioteca.todas()).allSatisfy(f ->
                assertThat(segundos(f.duracao())).isBetween(15.0, 60.0));
        assertThat(biblioteca.todas()).extracting(Faixa::id).doesNotHaveDuplicates();
    }

    @Test
    void selecionarETocarContaOTempoPeloRelogio() {
        reprodutor.selecionarMusica("fur-elise");
        assertThat(reprodutor.getEstado()).isEqualTo(Estado.PARADO);
        reprodutor.tocar();
        relogio.avancarSegundos(5);
        assertThat(reprodutor.posicao()).isEqualTo(Duration.ofSeconds(5));
        reprodutor.pausar();
        relogio.avancarSegundos(30);
        assertThat(reprodutor.posicao()).isEqualTo(Duration.ofSeconds(5));
        reprodutor.tocar();
        relogio.avancarSegundos(2);
        assertThat(reprodutor.posicao()).isEqualTo(Duration.ofSeconds(7));
    }

    @Test
    void tocarSemEscolherComecaPelaPrimeiraMusica() {
        reprodutor.tocar();
        assertThat(reprodutor.faixaAtual()).contains(faixa(0));
    }

    @Test
    void naoPausaSemEstarTocandoNemTocaDuasVezes() {
        assertThatThrownBy(reprodutor::pausar).isInstanceOf(EstadoInvalidoException.class);
        reprodutor.tocar();
        assertThatThrownBy(reprodutor::tocar).isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void musicaInexistenteEhRecusada() {
        assertThatThrownBy(() -> reprodutor.selecionarMusica("nao-existe"))
                .isInstanceOf(RegraVioladaException.class).hasMessageContaining("nao-existe");
    }

    @Test
    void trocarDeMusicaTocandoContinuaTocandoDoComeco() {
        reprodutor.tocar();
        relogio.avancarSegundos(4);
        reprodutor.selecionarMusica("greensleeves");
        assertThat(reprodutor.getEstado()).isEqualTo(Estado.TOCANDO);
        assertThat(reprodutor.posicao()).isZero();
    }

    @Test
    void aoTerminarPassaParaAProximaComASobraDeTempo() {
        reprodutor.selecionarMusica(faixa(0).id());
        reprodutor.tocar();
        relogio.avancar(faixa(0).duracao().plusSeconds(3));
        reprodutor.sincronizar();
        assertThat(reprodutor.faixaAtual()).contains(faixa(1));
        assertThat(reprodutor.posicao()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void semRepeticaoParaNoFimDaFilaEVoltaParaAPrimeira() {
        Faixa ultima = faixa(5);
        reprodutor.selecionarMusica(ultima.id());
        reprodutor.tocar();
        relogio.avancar(ultima.duracao().plusSeconds(1));
        reprodutor.sincronizar();
        assertThat(reprodutor.getEstado()).isEqualTo(Estado.PARADO);
        assertThat(reprodutor.faixaAtual()).contains(faixa(0));
        assertThat(reprodutor.posicao()).isZero();
    }

    @Test
    void repetirTodasVoltaParaAPrimeiraTocando() {
        reprodutor.alternarRepeticao();
        assertThat(reprodutor.getRepeticao()).isEqualTo(Repeticao.TODAS);
        reprodutor.selecionarMusica(faixa(5).id());
        reprodutor.tocar();
        relogio.avancar(faixa(5).duracao().plusSeconds(2));
        reprodutor.sincronizar();
        assertThat(reprodutor.getEstado()).isEqualTo(Estado.TOCANDO);
        assertThat(reprodutor.faixaAtual()).contains(faixa(0));
    }

    @Test
    void repetirUmaRecomecaAMesmaMusica() {
        reprodutor.alternarRepeticao();
        reprodutor.alternarRepeticao();
        assertThat(reprodutor.getRepeticao()).isEqualTo(Repeticao.UMA);
        reprodutor.selecionarMusica(faixa(2).id());
        reprodutor.tocar();
        relogio.avancar(faixa(2).duracao().multipliedBy(2).plusSeconds(1));
        assertThat(reprodutor.faixaAtual()).contains(faixa(2));
        reprodutor.sincronizar();
        assertThat(reprodutor.posicao()).isEqualTo(Duration.ofSeconds(1));
        reprodutor.alternarRepeticao();
        assertThat(reprodutor.getRepeticao()).isEqualTo(Repeticao.DESLIGADA);
    }

    @Test
    void variasMusicasPassamDeUmaVezSeOTempoForLongo() {
        reprodutor.alternarRepeticao();
        reprodutor.tocar();
        Duration tresMusicas = faixa(0).duracao().plus(faixa(1).duracao()).plus(faixa(2).duracao());
        relogio.avancar(tresMusicas.plusSeconds(1));
        reprodutor.sincronizar();
        assertThat(reprodutor.faixaAtual()).contains(faixa(3));
        assertThat(reprodutor.posicao()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void anteriorRecomecaSeJaPassouDe3SegundosSenaoVoltaUma() {
        reprodutor.selecionarMusica(faixa(2).id());
        reprodutor.tocar();
        relogio.avancarSegundos(10);
        reprodutor.anterior();
        assertThat(reprodutor.faixaAtual()).contains(faixa(2));
        assertThat(reprodutor.posicao()).isZero();
        relogio.avancarSegundos(1);
        reprodutor.anterior();
        assertThat(reprodutor.faixaAtual()).contains(faixa(1));
    }

    @Test
    void anteriorNaPrimeiraVaiParaAUltimaEProximaNaUltimaVoltaParaAPrimeira() {
        reprodutor.selecionarMusica(faixa(0).id());
        reprodutor.anterior();
        assertThat(reprodutor.faixaAtual()).contains(faixa(5));
        reprodutor.proxima();
        assertThat(reprodutor.faixaAtual()).contains(faixa(0));
    }

    @Test
    void proximaNaUltimaSemRepeticaoParaDeTocar() {
        reprodutor.selecionarMusica(faixa(5).id());
        reprodutor.tocar();
        reprodutor.proxima();
        assertThat(reprodutor.getEstado()).isEqualTo(Estado.PARADO);
        assertThat(reprodutor.faixaAtual()).contains(faixa(0));
    }

    @Test
    void proximaEAnteriorExigemMusica() {
        assertThatThrownBy(reprodutor::proxima).isInstanceOf(EstadoInvalidoException.class);
        assertThatThrownBy(reprodutor::anterior).isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void buscarMudaAPosicaoDentroDaMusica() {
        reprodutor.selecionarMusica(faixa(1).id());
        reprodutor.buscar(Duration.ofSeconds(8));
        assertThat(reprodutor.posicao()).isEqualTo(Duration.ofSeconds(8));
        assertThat(reprodutor.getEstado()).isEqualTo(Estado.PAUSADO);
        assertThatThrownBy(() -> reprodutor.buscar(faixa(1).duracao().plusSeconds(1)))
                .isInstanceOf(RegraVioladaException.class);
        assertThatThrownBy(() -> reprodutor.buscar(Duration.ofSeconds(-1)))
                .isInstanceOf(RegraVioladaException.class);
    }

    @Test
    void aleatorioMantemAMusicaAtualEmPrimeiroERepeteComAMesmaSemente() {
        reprodutor.selecionarMusica("canon-em-re");
        reprodutor.tocar();
        reprodutor.alternarAleatorio();
        assertThat(reprodutor.getFila().getFirst().id()).isEqualTo("canon-em-re");
        assertThat(reprodutor.getFila()).containsExactlyInAnyOrderElementsOf(biblioteca.todas());
        assertThat(reprodutor.getEstado()).isEqualTo(Estado.TOCANDO);

        var outro = new Reprodutor(biblioteca, relogio, new Random(7));
        outro.selecionarMusica("canon-em-re");
        outro.alternarAleatorio();
        assertThat(outro.getFila()).isEqualTo(reprodutor.getFila());

        reprodutor.alternarAleatorio();
        assertThat(reprodutor.getFila()).isEqualTo(biblioteca.todas());
        assertThat(reprodutor.faixaAtual().orElseThrow().id()).isEqualTo("canon-em-re");
    }
}
