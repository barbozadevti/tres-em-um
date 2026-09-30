package dev.barboza.tresemum.api;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.barboza.tresemum.api.Respostas.ContatoResposta;
import dev.barboza.tresemum.api.Respostas.Estado;
import dev.barboza.tresemum.api.Respostas.FaixaResposta;
import dev.barboza.tresemum.aparelho.IPhone;
import dev.barboza.tresemum.musica.Biblioteca;
import dev.barboza.tresemum.telefone.Agenda;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Cada visitante tem o próprio iPhone, guardado na sessão. Toda ação devolve o {@link Estado} completo.
 * As rotas seguem os três papéis do aparelho.
 */
@RestController
@RequestMapping("/api")
public class AparelhoController {

    public record Numero(@NotBlank String numero) { }

    public record Endereco(@NotBlank String url) { }

    public record Posicao(@NotNull @PositiveOrZero Double segundos) { }

    public record Tecla(@NotBlank String tecla) { }

    public record NovaAba(boolean privada) { }

    public record Selecao(@NotBlank String musica) { }

    private final IPhone iphone;
    private final Clock relogio;

    public AparelhoController(IPhone iphone, Clock relogio) {
        this.iphone = iphone;
        this.relogio = relogio;
    }

    private Estado agir(Consumer<IPhone> acao) {
        return iphone.executar(i -> {
            acao.accept(i);
            return Estado.de(i, relogio);
        });
    }

    // ----- Aparelho -----

    @Tag(name = "Aparelho")
    @Operation(summary = "Estado completo do aparelho", description = "Música, chamada, navegador e as últimas chamadas de método (rastro).")
    @GetMapping("/aparelho")
    public Estado estado() {
        return agir(i -> { });
    }

    @Tag(name = "Aparelho")
    @Operation(summary = "Começar de novo", description = "Descarta o aparelho desta sessão; o próximo pedido recebe um novo.")
    @PostMapping("/aparelho/reinicio")
    public ResponseEntity<Void> reiniciar(HttpSession sessao) {
        sessao.invalidate();
        return ResponseEntity.noContent().build();
    }

    // ----- ReprodutorMusical -----

    @Tag(name = "ReprodutorMusical")
    @GetMapping("/musica/biblioteca")
    public List<FaixaResposta> biblioteca() {
        return new Biblioteca().todas().stream().map(FaixaResposta::de).toList();
    }

    @Tag(name = "ReprodutorMusical")
    @Operation(summary = "selecionarMusica(musica)")
    @PostMapping("/musica/selecao")
    public Estado selecionar(@Valid @RequestBody Selecao selecao) {
        return agir(i -> i.selecionarMusica(selecao.musica()));
    }

    @Tag(name = "ReprodutorMusical")
    @Operation(summary = "Tocar uma música da lista", description = "selecionarMusica(id) seguido de tocar().")
    @PostMapping("/musica/faixas/{id}/tocar")
    public Estado tocarFaixa(@PathVariable String id) {
        return agir(i -> i.tocarMusica(id));
    }

    @Tag(name = "ReprodutorMusical")
    @Operation(summary = "tocar()")
    @PostMapping("/musica/tocar")
    public Estado tocar() {
        return agir(IPhone::tocar);
    }

    @Tag(name = "ReprodutorMusical")
    @Operation(summary = "pausar()")
    @PostMapping("/musica/pausar")
    public Estado pausar() {
        return agir(IPhone::pausar);
    }

    @Tag(name = "ReprodutorMusical")
    @Operation(summary = "proxima()")
    @PostMapping("/musica/proxima")
    public Estado proxima() {
        return agir(IPhone::proxima);
    }

    @Tag(name = "ReprodutorMusical")
    @Operation(summary = "anterior()", description = "Depois de 3 segundos, recomeça a música em vez de voltar.")
    @PostMapping("/musica/anterior")
    public Estado anterior() {
        return agir(IPhone::anterior);
    }

    @Tag(name = "ReprodutorMusical")
    @PostMapping("/musica/aleatorio")
    public Estado aleatorio() {
        return agir(i -> i.getReprodutor().alternarAleatorio());
    }

    @Tag(name = "ReprodutorMusical")
    @Operation(summary = "Repetição: desligada → todas → uma")
    @PostMapping("/musica/repeticao")
    public Estado repeticao() {
        return agir(i -> i.getReprodutor().alternarRepeticao());
    }

    @Tag(name = "ReprodutorMusical")
    @PostMapping("/musica/posicao")
    public Estado posicao(@Valid @RequestBody Posicao posicao) {
        return agir(i -> i.getReprodutor().buscar(Duration.ofMillis(Math.round(posicao.segundos() * 1000))));
    }

    // ----- AparelhoTelefonico -----

    @Tag(name = "AparelhoTelefonico")
    @GetMapping("/telefone/contatos")
    public List<ContatoResposta> contatos() {
        return Agenda.exemplo().todos().stream().map(ContatoResposta::de).toList();
    }

    @Tag(name = "AparelhoTelefonico")
    @Operation(summary = "ligar(numero)", description = "Simulado: o outro lado atende em 4 segundos (alguns contatos nunca atendem).")
    @PostMapping("/telefone/ligacoes")
    public Estado ligar(@Valid @RequestBody Numero numero) {
        return agir(i -> i.ligar(numero.numero()));
    }

    @Tag(name = "AparelhoTelefonico")
    @Operation(summary = "atender()")
    @PostMapping("/telefone/atender")
    public Estado atender() {
        return agir(IPhone::atender);
    }

    @Tag(name = "AparelhoTelefonico")
    @Operation(summary = "recusar()", description = "Quem ligou deixa um recado na caixa postal.")
    @PostMapping("/telefone/recusar")
    public Estado recusar() {
        return agir(IPhone::recusar);
    }

    @Tag(name = "AparelhoTelefonico")
    @Operation(summary = "encerrar()")
    @PostMapping("/telefone/encerrar")
    public Estado encerrar() {
        return agir(IPhone::encerrar);
    }

    @Tag(name = "AparelhoTelefonico")
    @PostMapping("/telefone/mudo")
    public Estado mudo() {
        return agir(i -> i.getTelefone().alternarMudo());
    }

    @Tag(name = "AparelhoTelefonico")
    @PostMapping("/telefone/viva-voz")
    public Estado vivaVoz() {
        return agir(i -> i.getTelefone().alternarVivaVoz());
    }

    @Tag(name = "AparelhoTelefonico")
    @Operation(summary = "Tecla durante a chamada (0-9, * ou #)")
    @PostMapping("/telefone/tons")
    public Estado tom(@Valid @RequestBody Tecla tecla) {
        return agir(i -> i.getTelefone().enviarTom(tecla.tecla()));
    }

    @Tag(name = "AparelhoTelefonico")
    @Operation(summary = "iniciarCorreioVoz()", description = "Toca o recado não ouvido mais antigo.")
    @PostMapping("/telefone/correio-de-voz")
    public Estado correioDeVoz() {
        return agir(IPhone::iniciarCorreioVoz);
    }

    @Tag(name = "AparelhoTelefonico")
    @Operation(summary = "Correio de voz visual: ouvir um recado qualquer")
    @PostMapping("/telefone/recados/{id}/ouvir")
    public Estado ouvirRecado(@PathVariable long id) {
        return agir(i -> i.getTelefone().ouvirRecado(id));
    }

    @Tag(name = "AparelhoTelefonico")
    @DeleteMapping("/telefone/recados/{id}")
    public Estado apagarRecado(@PathVariable long id) {
        return agir(i -> i.getTelefone().apagarRecado(id));
    }

    @Tag(name = "AparelhoTelefonico")
    @Operation(summary = "Zera o número de chamadas perdidas do ícone")
    @PostMapping("/telefone/recentes/visto")
    public Estado recentesVistos() {
        return agir(i -> i.getTelefone().marcarRecentesComoVistos());
    }

    @Tag(name = "Simulador")
    @Operation(summary = "Alguém liga para o aparelho", description = "Se houver música tocando, ela pausa e volta quando a chamada termina.")
    @PostMapping("/simulador/chamada-recebida")
    public Estado chamadaRecebida(@Valid @RequestBody Numero numero) {
        return agir(i -> i.receberChamada(numero.numero()));
    }

    // ----- NavegadorInternet -----

    @Tag(name = "NavegadorInternet")
    @Operation(summary = "exibirPagina(url)", description = "Aceita endereço ou termo de busca, como a barra de endereços.")
    @PostMapping("/navegador/pagina")
    public Estado exibir(@Valid @RequestBody Endereco endereco) {
        return agir(i -> i.exibirPagina(endereco.url()));
    }

    @Tag(name = "NavegadorInternet")
    @Operation(summary = "adicionarNovaAba()", description = "Com privada=true, abre uma aba que não entra no histórico.")
    @PostMapping("/navegador/abas")
    public Estado novaAba(@RequestBody(required = false) NovaAba aba) {
        return agir(i -> {
            if (aba != null && aba.privada()) {
                i.getNavegador().adicionarAbaPrivada();
            } else {
                i.adicionarNovaAba();
            }
        });
    }

    @Tag(name = "NavegadorInternet")
    @PostMapping("/navegador/abas/{id}/selecao")
    public Estado selecionarAba(@PathVariable int id) {
        return agir(i -> i.getNavegador().selecionarAba(id));
    }

    @Tag(name = "NavegadorInternet")
    @DeleteMapping("/navegador/abas/{id}")
    public Estado fecharAba(@PathVariable int id) {
        return agir(i -> i.getNavegador().fecharAba(id));
    }

    @Tag(name = "NavegadorInternet")
    @Operation(summary = "atualizarPagina()")
    @PostMapping("/navegador/atualizar")
    public Estado atualizar() {
        return agir(IPhone::atualizarPagina);
    }

    @Tag(name = "NavegadorInternet")
    @Operation(summary = "voltar()")
    @PostMapping("/navegador/voltar")
    public Estado voltar() {
        return agir(IPhone::voltar);
    }

    @Tag(name = "NavegadorInternet")
    @Operation(summary = "avancar()")
    @PostMapping("/navegador/avancar")
    public Estado avancar() {
        return agir(IPhone::avancar);
    }

    @Tag(name = "NavegadorInternet")
    @PostMapping("/navegador/favorito")
    public Estado favorito() {
        return agir(i -> i.getNavegador().alternarFavorito());
    }
}
