package dev.barboza.tresemum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.barboza.tresemum.aparelho.IPhone;
import dev.barboza.tresemum.aparelho.IPod;
import dev.barboza.tresemum.aparelho.Rastreador;
import dev.barboza.tresemum.aparelho.Rastro;
import dev.barboza.tresemum.aparelho.Registro;
import dev.barboza.tresemum.musica.Reprodutor.Estado;
import dev.barboza.tresemum.papeis.AparelhoTelefonico;
import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.NavegadorInternet;
import dev.barboza.tresemum.papeis.RegraVioladaException;
import dev.barboza.tresemum.papeis.ReprodutorMusical;

class IPhoneTest {

    private RelogioAjustavel relogio;
    private IPhone iphone;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel();
        iphone = new IPhone(relogio, new Random(1));
    }

    private List<String> rastro() {
        return iphone.getRastro().recentes(Rastro.MAXIMO).reversed().stream().map(Registro::assinatura).toList();
    }

    // --- Três aparelhos em um ---

    @Test
    void oIPhoneAssumeOsTresPapeis() {
        ReprodutorMusical reprodutor = iphone;
        AparelhoTelefonico telefone = iphone;
        NavegadorInternet navegador = iphone;

        reprodutor.selecionarMusica("fur-elise");
        reprodutor.tocar();
        navegador.exibirPagina("clima.exemplo");
        telefone.ligar("190");

        assertThat(rastro()).containsExactly(
                "ReprodutorMusical.selecionarMusica(\"fur-elise\")",
                "ReprodutorMusical.tocar()",
                "NavegadorInternet.exibirPagina(\"clima.exemplo\")",
                "AparelhoTelefonico.ligar(\"190\")",
                "ReprodutorMusical.pausar()");
    }

    @Test
    void oIPodSoTemOPapelDeReprodutor() {
        IPod ipod = new IPod(relogio);
        assertThat(ipod).isInstanceOf(ReprodutorMusical.class);
        assertThat(ipod).isNotInstanceOf(AparelhoTelefonico.class).isNotInstanceOf(NavegadorInternet.class);
        ipod.selecionarMusica("canon-em-re");
        ipod.tocar();
        relogio.avancarSegundos(3);
        assertThat(ipod.getReprodutor().posicao()).isEqualTo(Duration.ofSeconds(3));
    }

    // --- A cena de 2007: música, ligação, música de novo ---

    @Test
    void chamadaRecebidaPausaAMusicaEElaVoltaQuandoALigacaoTermina() {
        iphone.tocarMusica("ode-a-alegria");
        relogio.avancarSegundos(10);
        iphone.receberChamada("(27) 99901-2233");
        assertThat(iphone.getReprodutor().getEstado()).isEqualTo(Estado.PAUSADO);
        assertThat(iphone.isMusicaInterrompida()).isTrue();

        iphone.atender();
        relogio.avancarSegundos(30);
        iphone.encerrar();

        assertThat(iphone.getReprodutor().getEstado()).isEqualTo(Estado.TOCANDO);
        assertThat(iphone.getReprodutor().posicao()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void musicaVoltaTambemQuandoAChamadaCaiSozinha() {
        iphone.tocarMusica("minueto-em-sol");
        iphone.receberChamada("27999990000");
        relogio.avancarSegundos(20); // ninguém atendeu: chamada perdida
        iphone.sincronizar();
        assertThat(iphone.getReprodutor().getEstado()).isEqualTo(Estado.TOCANDO);
        assertThat(iphone.getTelefone().getRecados()).hasSize(1);
    }

    @Test
    void ligarPausaAMusicaERecusarDevolve() {
        iphone.tocarMusica("greensleeves");
        iphone.ligar("190");
        assertThat(iphone.getReprodutor().getEstado()).isEqualTo(Estado.PAUSADO);
        iphone.encerrar();
        assertThat(iphone.getReprodutor().getEstado()).isEqualTo(Estado.TOCANDO);

        iphone.receberChamada("190");
        iphone.recusar();
        assertThat(iphone.getReprodutor().getEstado()).isEqualTo(Estado.TOCANDO);
    }

    @Test
    void musicaPausadaAntesDaLigacaoContinuaPausada() {
        iphone.tocarMusica("fur-elise");
        iphone.pausar();
        iphone.ligar("190");
        iphone.encerrar();
        assertThat(iphone.getReprodutor().getEstado()).isEqualTo(Estado.PAUSADO);
    }

    @Test
    void naoTocaMusicaDuranteALigacao() {
        iphone.ligar("190");
        assertThatThrownBy(iphone::tocar).isInstanceOf(EstadoInvalidoException.class).hasMessageContaining("Termine a ligação");
        assertThatThrownBy(() -> iphone.tocarMusica("fur-elise")).isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void ouvirRecadoPausaAMusica() {
        IPhone demo = IPhone.comExemplos(relogio, new Random(1));
        demo.tocarMusica("canon-em-re");
        demo.iniciarCorreioVoz();
        assertThat(demo.getReprodutor().getEstado()).isEqualTo(Estado.PAUSADO);
        assertThat(demo.getTelefone().getRecadoEmReproducao()).isPresent();
    }

    // --- Rastro: cada chamada aos papéis passa pelo proxy ---

    @Test
    void rastroMostraAsChamadasAosPapeisEOMotivoQuandoOAparelhoAgeSozinho() {
        iphone.tocarMusica("fur-elise");
        iphone.receberChamada("(27) 99901-2233");
        iphone.atender();
        iphone.encerrar();

        assertThat(rastro()).containsExactly(
                "ReprodutorMusical.selecionarMusica(\"fur-elise\")",
                "ReprodutorMusical.tocar()",
                "Rede.chamadaRecebida(\"(27) 99901-2233\")",
                "ReprodutorMusical.pausar()",
                "AparelhoTelefonico.atender()",
                "AparelhoTelefonico.encerrar()",
                "ReprodutorMusical.tocar()");

        List<Registro> registros = iphone.getRastro().recentes(10);
        assertThat(registros.getFirst().motivo()).isEqualTo("ligação encerrada");
        assertThat(registros.get(3).motivo()).isEqualTo("chamada recebida");
        assertThat(registros.get(4).motivo()).isNull();
    }

    @Test
    void rastroGuardaAsChamadasRecusadasComOErro() {
        assertThatThrownBy(() -> iphone.ligar("123")).isInstanceOf(RegraVioladaException.class);
        Registro registro = iphone.getRastro().recentes(1).getFirst();
        assertThat(registro.assinatura()).isEqualTo("AparelhoTelefonico.ligar(\"123\")");
        assertThat(registro.erro()).isEqualTo("Serviço desconhecido: 123.");
    }

    @Test
    void rastroGuardaNoMaximo80Chamadas() {
        for (int i = 0; i < 100; i++) {
            iphone.atualizarPagina();
        }
        assertThat(iphone.getRastro().recentes(1000)).hasSize(Rastro.MAXIMO);
        assertThat(iphone.getRastro().recentes(1).getFirst().numero()).isEqualTo(100);
    }

    @Test
    void rastreadorCriaUmProxyDaInterfaceESoAceitaInterfaces() {
        Rastro rastro = new Rastro(relogio);
        NavegadorInternet proxy = Rastreador.rastrear(NavegadorInternet.class, iphone.getNavegador(), rastro);
        assertThat(Proxy.isProxyClass(proxy.getClass())).isTrue();
        proxy.exibirPagina("keynote.exemplo");
        assertThat(proxy.toString()).isNotBlank();
        assertThat(rastro.recentes(5)).extracting(Registro::assinatura).containsExactly("NavegadorInternet.exibirPagina(\"keynote.exemplo\")");
        assertThatThrownBy(() -> Rastreador.rastrear(IPhone.class, iphone, rastro)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void executarRodaVariasOperacoesJuntasDepoisDeSincronizar() {
        iphone.tocarMusica("brilha-estrelinha");
        relogio.avancar(Duration.ofMinutes(5)); // passa do fim da fila: para
        String faixa = iphone.executar(i -> i.getReprodutor().getEstado() + " " + i.getReprodutor().faixaAtual().orElseThrow().id());
        assertThat(faixa).isEqualTo("PARADO ode-a-alegria");
    }
}
