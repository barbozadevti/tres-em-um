package dev.barboza.tresemum;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = "spring.main.banner-mode=off")
@AutoConfigureMockMvc
@Import(ApiTest.Relogio.class)
class ApiTest {

    @TestConfiguration
    static class Relogio {
        @Bean
        @Primary
        RelogioAjustavel relogioAjustavel() {
            return new RelogioAjustavel();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    RelogioAjustavel relogio;

    private MockHttpSession sessao;

    @BeforeEach
    void novoVisitante() {
        sessao = new MockHttpSession();
    }

    private ResultActions postar(String rota) throws Exception {
        return mvc.perform(post(rota).session(sessao));
    }

    private ResultActions postar(String rota, String json) throws Exception {
        return mvc.perform(post(rota).session(sessao).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void estadoInicialTemHistoricoDeExemploEPaginaInicial() throws Exception {
        mvc.perform(get("/api/aparelho").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.musica.estado").value("PARADO"))
                .andExpect(jsonPath("$.telefone.chamada").doesNotExist())
                .andExpect(jsonPath("$.telefone.recentes", hasSize(5)))
                .andExpect(jsonPath("$.telefone.recadosNaoOuvidos").value(2))
                .andExpect(jsonPath("$.telefone.chamadasPerdidas").value(2))
                .andExpect(jsonPath("$.navegador.pagina.tipo").value("INICIO"))
                .andExpect(jsonPath("$.navegador.favoritos", hasSize(4)))
                .andExpect(jsonPath("$.rastro", hasSize(0)));
    }

    @Test
    void bibliotecaEContatos() throws Exception {
        mvc.perform(get("/api/musica/biblioteca"))
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].partitura", startsWith("E4:1")))
                .andExpect(jsonPath("$[0].duracao").isNumber());
        mvc.perform(get("/api/telefone/contatos"))
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].nome").value("Ana Souza"))
                .andExpect(jsonPath("$[0].iniciais").value("AS"));
    }

    @Test
    void cadaVisitanteTemOProprioAparelho() throws Exception {
        postar("/api/musica/faixas/fur-elise/tocar").andExpect(jsonPath("$.musica.estado").value("TOCANDO"));
        mvc.perform(get("/api/aparelho").session(new MockHttpSession()))
                .andExpect(jsonPath("$.musica.estado").value("PARADO"));
    }

    @Test
    void cenaDe2007PelaApi() throws Exception {
        postar("/api/musica/faixas/ode-a-alegria/tocar")
                .andExpect(jsonPath("$.musica.faixa.titulo").value("Ode à Alegria"))
                .andExpect(jsonPath("$.musica.estado").value("TOCANDO"));
        relogio.avancarSegundos(5);
        postar("/api/simulador/chamada-recebida", "{\"numero\":\"(27) 99901-2233\"}")
                .andExpect(jsonPath("$.telefone.chamada.nome").value("Mãe"))
                .andExpect(jsonPath("$.telefone.chamada.estado").value("TOCANDO"))
                .andExpect(jsonPath("$.musica.estado").value("PAUSADO"))
                .andExpect(jsonPath("$.musica.interrompida").value(true))
                .andExpect(jsonPath("$.rastro[0].assinatura").value("ReprodutorMusical.pausar()"))
                .andExpect(jsonPath("$.rastro[0].motivo").value("chamada recebida"));
        postar("/api/telefone/atender").andExpect(jsonPath("$.telefone.chamada.estado").value("EM_ANDAMENTO"));
        relogio.avancarSegundos(12);
        postar("/api/telefone/encerrar")
                .andExpect(jsonPath("$.telefone.chamada").doesNotExist())
                .andExpect(jsonPath("$.telefone.recentes[0].desfecho").value("Concluída"))
                .andExpect(jsonPath("$.telefone.recentes[0].duracao").value(12.0))
                .andExpect(jsonPath("$.musica.estado").value("TOCANDO"))
                .andExpect(jsonPath("$.musica.posicao").value(5.0))
                .andExpect(jsonPath("$.rastro[0].motivo").value("ligação encerrada"));
    }

    @Test
    void controlesDoReprodutor() throws Exception {
        postar("/api/musica/selecao", "{\"musica\":\"greensleeves\"}").andExpect(jsonPath("$.musica.estado").value("PARADO"));
        postar("/api/musica/tocar").andExpect(jsonPath("$.musica.estado").value("TOCANDO"));
        postar("/api/musica/proxima").andExpect(jsonPath("$.musica.faixa.id").value("canon-em-re"));
        postar("/api/musica/anterior").andExpect(jsonPath("$.musica.faixa.id").value("greensleeves"));
        postar("/api/musica/posicao", "{\"segundos\":7.5}").andExpect(jsonPath("$.musica.posicao").value(7.5));
        postar("/api/musica/aleatorio").andExpect(jsonPath("$.musica.aleatorio").value(true))
                .andExpect(jsonPath("$.musica.fila[0]").value("greensleeves"));
        postar("/api/musica/repeticao").andExpect(jsonPath("$.musica.repeticao").value("TODAS"));
        postar("/api/musica/pausar").andExpect(jsonPath("$.musica.estado").value("PAUSADO"));
    }

    @Test
    void ligacaoFeitaComTeclasMudoEVivaVoz() throws Exception {
        postar("/api/telefone/ligacoes", "{\"numero\":\"0800 555 0101\"}")
                .andExpect(jsonPath("$.telefone.chamada.estado").value("CHAMANDO"))
                .andExpect(jsonPath("$.telefone.chamada.nome").value("Suporte da Operadora"));
        relogio.avancarSegundos(4);
        postar("/api/telefone/tons", "{\"tecla\":\"2\"}").andExpect(jsonPath("$.telefone.chamada.estado").value("EM_ANDAMENTO"));
        postar("/api/telefone/mudo").andExpect(jsonPath("$.telefone.chamada.mudo").value(true));
        postar("/api/telefone/viva-voz").andExpect(jsonPath("$.telefone.chamada.vivaVoz").value(true));
        postar("/api/telefone/encerrar").andExpect(jsonPath("$.telefone.recentes[0].nome").value("Suporte da Operadora"));
    }

    @Test
    void correioDeVozVisualEChamadasPerdidas() throws Exception {
        postar("/api/telefone/correio-de-voz")
                .andExpect(jsonPath("$.telefone.recadoEmReproducao").isNumber())
                .andExpect(jsonPath("$.telefone.recadosNaoOuvidos").value(1));
        String estado = mvc.perform(get("/api/aparelho").session(sessao)).andReturn().getResponse().getContentAsString();
        long id = com.jayway.jsonpath.JsonPath.parse(estado).read("$.telefone.recados[0].id", Long.class);
        postar("/api/telefone/recados/" + id + "/ouvir").andExpect(jsonPath("$.telefone.recadosNaoOuvidos").value(0));
        mvc.perform(delete("/api/telefone/recados/" + id).session(sessao)).andExpect(jsonPath("$.telefone.recados", hasSize(1)));
        postar("/api/telefone/recentes/visto").andExpect(jsonPath("$.telefone.chamadasPerdidas").value(0));
        postar("/api/telefone/correio-de-voz").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Nenhum recado novo."));
    }

    @Test
    void navegacaoComAbasBuscaEFavoritos() throws Exception {
        postar("/api/navegador/pagina", "{\"url\":\"clima.exemplo\"}")
                .andExpect(jsonPath("$.navegador.pagina.titulo").value("Clima em Vitória"))
                .andExpect(jsonPath("$.navegador.pagina.dominio").value("clima.exemplo"))
                .andExpect(jsonPath("$.navegador.pagina.blocos[1].tipo").value("destaque"))
                .andExpect(jsonPath("$.navegador.pagina.blocos[1].valor").value("27°"))
                .andExpect(jsonPath("$.navegador.pagina.blocos[0].itens").doesNotExist())
                .andExpect(jsonPath("$.navegador.podeVoltar").value(true));
        postar("/api/navegador/atualizar").andExpect(jsonPath("$.navegador.atualizacoes").value(1));
        postar("/api/navegador/favorito").andExpect(jsonPath("$.navegador.favorita").value(false));
        postar("/api/navegador/voltar").andExpect(jsonPath("$.navegador.pagina.tipo").value("INICIO"));
        postar("/api/navegador/avancar").andExpect(jsonPath("$.navegador.pagina.tipo").value("SITE"));
        postar("/api/navegador/abas", "{\"privada\":true}")
                .andExpect(jsonPath("$.navegador.abas", hasSize(2)))
                .andExpect(jsonPath("$.navegador.abas[1].privada").value(true));
        postar("/api/navegador/pagina", "{\"url\":\"moqueca\"}")
                .andExpect(jsonPath("$.navegador.pagina.tipo").value("BUSCA"))
                .andExpect(jsonPath("$.navegador.pagina.blocos[1].cartoes[*].url", hasItem("https://receitas.exemplo/moqueca-capixaba")));
        postar("/api/navegador/abas/1/selecao").andExpect(jsonPath("$.navegador.abaAtual").value(1));
        mvc.perform(delete("/api/navegador/abas/1").session(sessao)).andExpect(jsonPath("$.navegador.abas", hasSize(1)));
    }

    @Test
    void errosEmProblemJson() throws Exception {
        postar("/api/telefone/ligacoes", "{\"numero\":\"123\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Serviço desconhecido: 123."));
        postar("/api/musica/pausar").andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Não é possível agora"));
        postar("/api/navegador/pagina", "{\"url\":\"javascript:alert(1)\"}").andExpect(status().isUnprocessableContent());
        postar("/api/telefone/ligacoes", "{}").andExpect(status().isBadRequest());
        postar("/api/musica/posicao", "{\"segundos\":-1}").andExpect(status().isBadRequest());
        postar("/api/navegador/abas/abc/selecao").andExpect(status().isBadRequest());
    }

    @Test
    void reiniciarDescartaOAparelhoDaSessao() throws Exception {
        postar("/api/musica/faixas/fur-elise/tocar");
        postar("/api/aparelho/reinicio").andExpect(status().isNoContent());
        sessao = new MockHttpSession();
        mvc.perform(get("/api/aparelho").session(sessao)).andExpect(jsonPath("$.musica.estado").value("PARADO"));
    }

    @Test
    void saudeEDocumentacao() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Três em Um — API do iPhone"));
    }
}
