package dev.barboza.tresemum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.RegraVioladaException;
import dev.barboza.tresemum.telefone.Agenda;
import dev.barboza.tresemum.telefone.Chamada;
import dev.barboza.tresemum.telefone.Chamada.Desfecho;
import dev.barboza.tresemum.telefone.Chamada.Estado;
import dev.barboza.tresemum.telefone.Numero;
import dev.barboza.tresemum.telefone.Telefone;

class TelefoneTest {

    private RelogioAjustavel relogio;
    private Telefone telefone;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel();
        telefone = new Telefone(Agenda.exemplo(), relogio);
    }

    private Chamada atual() {
        return telefone.getChamadaAtual().orElseThrow();
    }

    // --- Números ---

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "(27) 99876-5432   | 27998765432 | (27) 99876-5432",
            "+55 11 98765-4321 | 11987654321 | (11) 98765-4321",
            "027 3223-4567     | 2732234567  | (27) 3223-4567",
            "99876-5432        | 27998765432 | (27) 99876-5432",
            "3223.4567         | 2732234567  | (27) 3223-4567",
            "190               | 190         | 190",
            "0800 555 0101     | 08005550101 | 0800 555 0101"})
    void numeroAceitaOQueSeDigitaNoDiaADia(String digitado, String digitos, String formatado) {
        Numero numero = Numero.de(digitado);
        assertThat(numero.digitos()).isEqualTo(digitos);
        assertThat(numero.formatado()).isEqualTo(formatado);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "''                | Digite um número",
            "abc               | só pode ter dígitos",
            "123               | Serviço desconhecido",
            "(20) 99876-5432   | DDD 20 não existe",
            "(27) 89876-5432   | Celular começa com 9",
            "(27) 9223-4567    | fixo começa com 2, 3, 4 ou 5",
            "12345             | incompleto",
            "0800 555          | 0800 deve ter 11 dígitos",
            "(27) 99876-54321  | incompleto ou longo demais"})
    void numeroInvalidoExplicaOMotivo(String digitado, String motivo) {
        assertThatThrownBy(() -> Numero.de(digitado))
                .isInstanceOf(RegraVioladaException.class).hasMessageContaining(motivo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"190", "192", "193"})
    void emergenciasSaoReconhecidas(String numero) {
        assertThat(Numero.de(numero).emergencia()).isTrue();
        assertThat(Numero.de("100").emergencia()).isFalse();
    }

    // --- Máquina de estados da chamada: todas as combinações ---

    @Test
    void transicoesDaChamadaConferemComATabela() {
        Map<Estado, Set<Estado>> esperado = Map.of(
                Estado.CHAMANDO, EnumSet.of(Estado.EM_ANDAMENTO, Estado.ENCERRADA),
                Estado.TOCANDO, EnumSet.of(Estado.EM_ANDAMENTO, Estado.ENCERRADA),
                Estado.EM_ANDAMENTO, EnumSet.of(Estado.ENCERRADA),
                Estado.ENCERRADA, EnumSet.noneOf(Estado.class));
        for (Estado de : Estado.values()) {
            for (Estado para : Estado.values()) {
                assertThat(de.proximos().contains(para))
                        .as("%s -> %s", de, para)
                        .isEqualTo(esperado.get(de).contains(para));
            }
        }
    }

    // --- Ligações feitas ---

    @Test
    void ligacaoFeitaChamaEOContatoAtendeDepoisDe4Segundos() {
        telefone.ligar("(27) 99812-3401");
        assertThat(atual().getEstado()).isEqualTo(Estado.CHAMANDO);
        assertThat(atual().nomeOuNumero()).isEqualTo("Ana Souza");
        relogio.avancarSegundos(3.9);
        assertThat(atual().getEstado()).isEqualTo(Estado.CHAMANDO);
        relogio.avancarSegundos(0.1);
        assertThat(atual().getEstado()).isEqualTo(Estado.EM_ANDAMENTO);
        relogio.avancarSegundos(65);
        assertThat(atual().duracao(relogio.instant())).isEqualTo(Duration.ofSeconds(65));

        telefone.encerrar();
        assertThat(telefone.getChamadaAtual()).isEmpty();
        Chamada registro = telefone.getRecentes().getFirst();
        assertThat(registro.getDesfecho()).contains(Desfecho.CONCLUIDA);
        assertThat(registro.duracao(relogio.instant())).isEqualTo(Duration.ofSeconds(65));
    }

    @Test
    void numeroForaDaAgendaAparecePeloNumeroFormatado() {
        telefone.ligar("27999990000");
        assertThat(atual().nomeOuNumero()).isEqualTo("(27) 99999-0000");
        assertThat(atual().getContato()).isEmpty();
    }

    @Test
    void contatoQueNaoAtendeViraNaoAtendidaDepoisDe25Segundos() {
        telefone.ligar("(21) 99654-7788"); // Diego nunca atende
        relogio.avancarSegundos(24);
        assertThat(atual().getEstado()).isEqualTo(Estado.CHAMANDO);
        relogio.avancarSegundos(1);
        assertThat(telefone.getChamadaAtual()).isEmpty();
        assertThat(telefone.getRecentes().getFirst().getDesfecho()).contains(Desfecho.NAO_ATENDIDA);
    }

    @Test
    void desligarAntesDeAtenderCancela() {
        telefone.ligar("190");
        telefone.encerrar();
        Chamada registro = telefone.getRecentes().getFirst();
        assertThat(registro.getDesfecho()).contains(Desfecho.CANCELADA);
        assertThat(registro.duracao(relogio.instant())).isZero();
    }

    @Test
    void naoLigaComChamadaEmCursoNemEncerraSemChamada() {
        assertThatThrownBy(telefone::encerrar).isInstanceOf(EstadoInvalidoException.class);
        telefone.ligar("190");
        assertThatThrownBy(() -> telefone.ligar("192"))
                .isInstanceOf(EstadoInvalidoException.class).hasMessageContaining("em curso");
    }

    @Test
    void numeroInvalidoNaoCriaChamada() {
        assertThatThrownBy(() -> telefone.ligar("123")).isInstanceOf(RegraVioladaException.class);
        assertThat(telefone.getChamadaAtual()).isEmpty();
    }

    // --- Ligações recebidas ---

    @Test
    void ligacaoRecebidaAtendidaEEncerrada() {
        telefone.receberChamada("(27) 99901-2233");
        assertThat(atual().getEstado()).isEqualTo(Estado.TOCANDO);
        assertThat(atual().nomeOuNumero()).isEqualTo("Mãe");
        relogio.avancarSegundos(3);
        telefone.atender();
        relogio.avancarSegundos(30);
        telefone.encerrar();
        Chamada registro = telefone.getRecentes().getFirst();
        assertThat(registro.getDirecao()).isEqualTo(Chamada.Direcao.RECEBIDA);
        assertThat(registro.duracao(relogio.instant())).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void naoAtendidaEm20SegundosViraPerdidaEDeixaRecado() {
        telefone.receberChamada("(27) 99745-1122");
        relogio.avancarSegundos(20);
        assertThat(telefone.getChamadaAtual()).isEmpty();
        assertThat(telefone.getRecentes().getFirst().getDesfecho()).contains(Desfecho.PERDIDA);
        assertThat(telefone.chamadasPerdidas()).isEqualTo(1);
        assertThat(telefone.getRecados()).hasSize(1);
        assertThat(telefone.getRecados().getFirst().getTranscricao()).startsWith("Oi, aqui é Bruno.");
    }

    @Test
    void recusarEncerraEDeixaRecado() {
        telefone.receberChamada("27999990000");
        telefone.recusar();
        assertThat(telefone.getRecentes().getFirst().getDesfecho()).contains(Desfecho.RECUSADA);
        assertThat(telefone.getRecados().getFirst().getTranscricao()).startsWith("Oi, tudo bem?");
    }

    @Test
    void servicoComo190NaoDeixaRecado() {
        telefone.receberChamada("190");
        telefone.recusar();
        assertThat(telefone.getRecados()).isEmpty();
    }

    @Test
    void naoAtendeNemRecusaSemChamadaTocando() {
        assertThatThrownBy(telefone::atender).isInstanceOf(EstadoInvalidoException.class);
        telefone.ligar("190");
        assertThatThrownBy(telefone::atender).isInstanceOf(EstadoInvalidoException.class);
        assertThatThrownBy(telefone::recusar).isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void linhaOcupadaNaoRecebeOutraChamada() {
        telefone.ligar("190");
        assertThatThrownBy(() -> telefone.receberChamada("192"))
                .isInstanceOf(EstadoInvalidoException.class).hasMessageContaining("ocupada");
    }

    // --- Recursos durante a chamada ---

    @Test
    void mudoEVivaVozETonsDoTeclado() {
        telefone.ligar("0800 555 0101");
        assertThatThrownBy(telefone::alternarMudo).isInstanceOf(EstadoInvalidoException.class);
        telefone.alternarVivaVoz();
        relogio.avancarSegundos(4);
        telefone.alternarMudo();
        telefone.enviarTom("1");
        telefone.enviarTom("#");
        assertThat(atual().isMudo()).isTrue();
        assertThat(atual().isVivaVoz()).isTrue();
        assertThat(atual().getTons()).isEqualTo("1#");
        assertThatThrownBy(() -> telefone.enviarTom("A")).isInstanceOf(RegraVioladaException.class);
    }

    // --- Correio de voz ---

    @Test
    void correioDeVozTocaOsRecadosNovosDoMaisAntigoParaOMaisNovo() {
        telefone.carregarExemplos();
        assertThat(telefone.recadosNaoOuvidos()).isEqualTo(2);
        telefone.iniciarCorreioVoz();
        assertThat(telefone.getRecadoEmReproducao().orElseThrow().getNome()).isEqualTo("Carla Mendes");
        telefone.iniciarCorreioVoz();
        assertThat(telefone.getRecadoEmReproducao().orElseThrow().getNome()).isEqualTo("Ana Souza");
        assertThat(telefone.recadosNaoOuvidos()).isZero();
        assertThatThrownBy(telefone::iniciarCorreioVoz)
                .isInstanceOf(EstadoInvalidoException.class).hasMessage("Nenhum recado novo.");
    }

    @Test
    void correioVisualOuveEmQualquerOrdemEApaga() {
        telefone.carregarExemplos();
        long daAna = telefone.getRecados().getFirst().getId();
        telefone.ouvirRecado(daAna);
        assertThat(telefone.recadosNaoOuvidos()).isEqualTo(1);
        telefone.apagarRecado(daAna);
        assertThat(telefone.getRecados()).hasSize(1);
        assertThat(telefone.getRecadoEmReproducao()).isEmpty();
        assertThatThrownBy(() -> telefone.ouvirRecado(999)).isInstanceOf(RegraVioladaException.class);
    }

    @Test
    void recadoTemDuracaoEstimadaPelaFala() {
        telefone.carregarExemplos();
        assertThat(telefone.getRecados()).allSatisfy(r -> assertThat(r.duracao()).isBetween(Duration.ofSeconds(3), Duration.ofSeconds(15)));
    }

    @Test
    void exemplosTrazemRecentesEONumeroNoIconeZeraAoAbrirRecentes() {
        telefone.carregarExemplos();
        assertThat(telefone.getRecentes()).hasSize(5);
        assertThat(telefone.chamadasPerdidas()).isEqualTo(2);
        telefone.marcarRecentesComoVistos();
        assertThat(telefone.chamadasPerdidas()).isZero();
        relogio.avancarSegundos(1);
        telefone.receberChamada("190");
        relogio.avancarSegundos(20);
        assertThat(telefone.chamadasPerdidas()).isEqualTo(1);
    }

    @Test
    void recentesGuardaNoMaximo30() {
        for (int i = 0; i < 35; i++) {
            telefone.ligar("190");
            telefone.encerrar();
        }
        assertThat(telefone.getRecentes()).hasSize(Telefone.MAXIMO_DE_RECENTES);
    }
}
