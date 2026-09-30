package dev.barboza.tresemum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.barboza.tresemum.navegador.Bloco;
import dev.barboza.tresemum.navegador.Endereco;
import dev.barboza.tresemum.navegador.Navegador;
import dev.barboza.tresemum.navegador.Pagina;
import dev.barboza.tresemum.navegador.Web;
import dev.barboza.tresemum.papeis.EstadoInvalidoException;
import dev.barboza.tresemum.papeis.RegraVioladaException;

class NavegadorTest {

    private RelogioAjustavel relogio;
    private Navegador navegador;

    @BeforeEach
    void preparar() {
        relogio = new RelogioAjustavel();
        navegador = new Navegador(new Web(), relogio);
    }

    private String url() {
        return navegador.getAbaAtual().getUrl();
    }

    // --- Barra de endereços ---

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "clima.exemplo                  | https://clima.exemplo/",
            "  CLIMA.Exemplo/               | https://clima.exemplo/",
            "http://noticias.exemplo/x?a=1  | http://noticias.exemplo/x?a=1",
            "localhost:5260                 | https://localhost:5260/",
            "moqueca capixaba               | https://busca.exemplo/?q=moqueca+capixaba",
            "iphone                         | https://busca.exemplo/?q=iphone",
            "about:blank                    | about:blank"})
    void barraDeEnderecosEntendeEnderecosEBuscas(String digitado, String esperado) {
        assertThat(Endereco.interpretar(digitado)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "file:///C:/Windows", "data:text/html,oi", "ftp://site.exemplo"})
    void esquemasPerigososSaoRecusados(String digitado) {
        assertThatThrownBy(() -> Endereco.interpretar(digitado))
                .isInstanceOf(RegraVioladaException.class).hasMessageContaining("não permitido");
    }

    @Test
    void enderecoVazioOuLongoDemaisERecusado() {
        assertThatThrownBy(() -> Endereco.interpretar("  ")).isInstanceOf(RegraVioladaException.class);
        assertThatThrownBy(() -> Endereco.interpretar("a".repeat(301) + ".exemplo")).isInstanceOf(RegraVioladaException.class);
    }

    // --- Histórico da aba ---

    @Test
    void comecaNaPaginaInicialComQuatroFavoritos() {
        assertThat(url()).isEqualTo(Endereco.PAGINA_INICIAL);
        assertThat(navegador.paginaAtual().tipo()).isEqualTo(Pagina.Tipo.INICIO);
        assertThat(navegador.getFavoritos()).hasSize(4);
        assertThat(navegador.getAbaAtual().podeVoltar()).isFalse();
    }

    @Test
    void voltarEAvancarPercorremOHistoricoDaAba() {
        navegador.exibirPagina("noticias.exemplo");
        navegador.exibirPagina("noticias.exemplo/iphone-em-java");
        navegador.voltar();
        assertThat(url()).isEqualTo("https://noticias.exemplo/");
        navegador.voltar();
        assertThat(url()).isEqualTo(Endereco.PAGINA_INICIAL);
        assertThatThrownBy(navegador::voltar).isInstanceOf(EstadoInvalidoException.class);
        navegador.avancar();
        navegador.avancar();
        assertThat(url()).isEqualTo("https://noticias.exemplo/iphone-em-java");
        assertThatThrownBy(navegador::avancar).isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void abrirPaginaDepoisDeVoltarDescartaOQueEstavaParaFrente() {
        navegador.exibirPagina("clima.exemplo");
        navegador.exibirPagina("receitas.exemplo");
        navegador.voltar();
        navegador.exibirPagina("keynote.exemplo");
        assertThat(navegador.getAbaAtual().podeAvancar()).isFalse();
        assertThat(navegador.getAbaAtual().getHistorico())
                .containsExactly("about:blank", "https://clima.exemplo/", "https://keynote.exemplo/");
    }

    @Test
    void atualizarContaAsRecargasEAbrirAMesmaPaginaEquivaleAAtualizar() {
        navegador.exibirPagina("clima.exemplo");
        navegador.atualizarPagina();
        relogio.avancarSegundos(10);
        navegador.exibirPagina("https://clima.exemplo/");
        assertThat(navegador.getAbaAtual().getAtualizacoes()).isEqualTo(2);
        assertThat(navegador.getAbaAtual().getCarregadaEm()).isEqualTo(relogio.instant());
        assertThat(navegador.getAbaAtual().getHistorico()).hasSize(2);
    }

    // --- Abas ---

    @Test
    void cadaAbaTemOProprioHistorico() {
        navegador.exibirPagina("clima.exemplo");
        int primeira = navegador.getAbaAtual().getId();
        navegador.adicionarNovaAba();
        assertThat(url()).isEqualTo(Endereco.PAGINA_INICIAL);
        navegador.exibirPagina("receitas.exemplo");
        navegador.selecionarAba(primeira);
        assertThat(url()).isEqualTo("https://clima.exemplo/");
        assertThat(navegador.getAbas()).hasSize(2);
    }

    @Test
    void limiteDe8Abas() {
        for (int i = 1; i < Navegador.MAXIMO_DE_ABAS; i++) {
            navegador.adicionarNovaAba();
        }
        assertThatThrownBy(navegador::adicionarNovaAba)
                .isInstanceOf(RegraVioladaException.class).hasMessageContaining("Limite de 8 abas");
    }

    @Test
    void fecharAAbaAtualSelecionaAVizinhaEFecharAUltimaAbreUmaNova() {
        int primeira = navegador.getAbaAtual().getId();
        navegador.adicionarNovaAba();
        int segunda = navegador.getAbaAtual().getId();
        navegador.fecharAba(segunda);
        assertThat(navegador.getAbaAtual().getId()).isEqualTo(primeira);
        navegador.fecharAba(primeira);
        assertThat(navegador.getAbas()).hasSize(1);
        assertThat(navegador.getAbaAtual().getId()).isNotEqualTo(primeira);
        assertThatThrownBy(() -> navegador.fecharAba(999)).isInstanceOf(RegraVioladaException.class);
    }

    @Test
    void abaPrivadaNaoEntraNasPaginasVisitadas() {
        navegador.exibirPagina("clima.exemplo");
        navegador.adicionarAbaPrivada();
        assertThat(navegador.getAbaAtual().isPrivada()).isTrue();
        navegador.exibirPagina("receitas.exemplo");
        navegador.exibirPagina("moqueca");
        assertThat(navegador.getVisitadas()).containsExactly("https://clima.exemplo/");
    }

    @Test
    void favoritarETirarDosFavoritos() {
        assertThatThrownBy(navegador::alternarFavorito).isInstanceOf(EstadoInvalidoException.class);
        navegador.exibirPagina("receitas.exemplo");
        assertThat(navegador.isFavorita()).isFalse();
        navegador.alternarFavorito();
        assertThat(navegador.isFavorita()).isTrue();
        assertThat(navegador.getFavoritos()).last().isEqualTo("https://receitas.exemplo/");
        navegador.alternarFavorito();
        assertThat(navegador.isFavorita()).isFalse();
    }

    // --- A "internet" do simulador ---

    @Test
    void sitesConhecidosAbremComTituloEBlocos() {
        Web web = new Web();
        for (String site : new String[] {"noticias.exemplo", "clima.exemplo", "receitas.exemplo", "keynote.exemplo", "barboza.dev",
                "noticias.exemplo/iphone-em-java", "noticias.exemplo/festival-da-moqueca", "noticias.exemplo/ciclovia-aos-domingos"}) {
            Pagina pagina = web.abrir(Endereco.interpretar(site));
            assertThat(pagina.tipo()).as(site).isEqualTo(Pagina.Tipo.SITE);
            assertThat(pagina.blocos()).as(site).isNotEmpty();
        }
    }

    @Test
    void servidorDesconhecidoECaminhoInexistenteMostramErro() {
        Web web = new Web();
        Pagina semServidor = web.abrir("https://nao-existe.exemplo/");
        assertThat(semServidor.tipo()).isEqualTo(Pagina.Tipo.ERRO);
        assertThat(semServidor.titulo()).isEqualTo("Não foi possível abrir a página");
        Pagina semPagina = web.abrir("https://clima.exemplo/amanha");
        assertThat(semPagina.titulo()).isEqualTo("Página não encontrada");
    }

    @Test
    void buscaIgnoraAcentosEMostraOsResultados() {
        navegador.exibirPagina("noticias moqueca");
        Pagina pagina = navegador.paginaAtual();
        assertThat(pagina.tipo()).isEqualTo(Pagina.Tipo.BUSCA);
        var cartoes = pagina.blocos().stream().filter(b -> b instanceof Bloco.Cartoes).map(b -> (Bloco.Cartoes) b).findFirst().orElseThrow();
        assertThat(cartoes.itens()).extracting(Bloco.Cartao::url)
                .contains("https://noticias.exemplo/", "https://receitas.exemplo/moqueca-capixaba", "https://noticias.exemplo/festival-da-moqueca");

        navegador.exibirPagina("xyzxyz");
        assertThat(navegador.paginaAtual().blocos()).anySatisfy(b ->
                assertThat(b).isInstanceOfSatisfying(Bloco.Paragrafo.class, p -> assertThat(p.texto()).startsWith("Nenhum resultado")));
    }
}
