package dev.barboza.tresemum.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import dev.barboza.tresemum.aparelho.IPhone;
import dev.barboza.tresemum.aparelho.Registro;
import dev.barboza.tresemum.musica.Faixa;
import dev.barboza.tresemum.musica.Reprodutor;
import dev.barboza.tresemum.navegador.Aba;
import dev.barboza.tresemum.navegador.Bloco;
import dev.barboza.tresemum.navegador.Endereco;
import dev.barboza.tresemum.navegador.Navegador;
import dev.barboza.tresemum.navegador.Pagina;
import dev.barboza.tresemum.telefone.Chamada;
import dev.barboza.tresemum.telefone.Contato;
import dev.barboza.tresemum.telefone.Recado;
import dev.barboza.tresemum.telefone.Telefone;

/** O que a API devolve. Os objetos do domínio não saem direto: cada resposta mostra só o que a tela usa. */
public final class Respostas {

    private Respostas() {
    }

    private static double segundos(Duration duracao) {
        return duracao.toMillis() / 1000.0;
    }

    /** Fotografia do aparelho: toda ação devolve isto, e a tela se redesenha a partir dela. */
    public record Estado(Instant agora, Musica musica, Fone telefone, Web navegador, List<Chamado> rastro) {

        static Estado de(IPhone iphone, Clock relogio) {
            Instant agora = relogio.instant();
            return new Estado(agora, Musica.de(iphone), Fone.de(iphone.getTelefone(), agora),
                    Web.de(iphone.getNavegador()),
                    iphone.getRastro().recentes(40).stream().map(Chamado::de).toList());
        }
    }

    // ----- Música -----

    public record Musica(String estado, FaixaResposta faixa, double posicao, boolean aleatorio, String repeticao,
                         List<String> fila, boolean interrompida) {

        static Musica de(IPhone iphone) {
            Reprodutor r = iphone.getReprodutor();
            return new Musica(r.getEstado().name(), r.faixaAtual().map(FaixaResposta::de).orElse(null),
                    segundos(r.posicao()), r.isAleatorio(), r.getRepeticao().name(),
                    r.getFila().stream().map(Faixa::id).toList(), iphone.isMusicaInterrompida());
        }
    }

    public record FaixaResposta(String id, String titulo, String autor, String ano, int bpm, String partitura,
                                String baixo, String cor, String corSecundaria, double duracao) {

        static FaixaResposta de(Faixa f) {
            return new FaixaResposta(f.id(), f.titulo(), f.autor(), f.ano(), f.bpm(), f.partitura(), f.baixo(),
                    f.cor(), f.corSecundaria(), segundos(f.duracao()));
        }
    }

    // ----- Telefone -----

    public record Fone(ChamadaResposta chamada, List<ChamadaResposta> recentes, List<RecadoResposta> recados,
                       long recadosNaoOuvidos, long chamadasPerdidas, Long recadoEmReproducao) {

        static Fone de(Telefone t, Instant agora) {
            return new Fone(t.getChamadaAtual().map(c -> ChamadaResposta.de(c, agora)).orElse(null),
                    t.getRecentes().stream().map(c -> ChamadaResposta.de(c, agora)).toList(),
                    t.getRecados().stream().map(RecadoResposta::de).toList(),
                    t.recadosNaoOuvidos(), t.chamadasPerdidas(),
                    t.getRecadoEmReproducao().map(Recado::getId).orElse(null));
        }
    }

    public record ChamadaResposta(long id, String numero, String nome, String contato, String cor, String direcao,
                                  String estado, String desfecho, Instant inicio, double duracao,
                                  boolean mudo, boolean vivaVoz, boolean emergencia, String tons) {

        static ChamadaResposta de(Chamada c, Instant agora) {
            return new ChamadaResposta(c.getId(), c.getNumero().formatado(), c.nomeOuNumero(),
                    c.getContato().map(Contato::id).orElse(null), c.getContato().map(Contato::cor).orElse(null),
                    c.getDirecao().name(), c.getEstado().name(), c.getDesfecho().map(Chamada.Desfecho::nome).orElse(null),
                    c.getInicio(), segundos(c.duracao(agora)), c.isMudo(), c.isVivaVoz(), c.getNumero().emergencia(),
                    c.getTons());
        }
    }

    public record RecadoResposta(long id, String numero, String nome, Instant recebidoEm, double duracao,
                                 String transcricao, boolean ouvido) {

        static RecadoResposta de(Recado r) {
            return new RecadoResposta(r.getId(), r.getDe().formatado(), r.getNome(), r.getRecebidoEm(),
                    segundos(r.duracao()), r.getTranscricao(), r.isOuvido());
        }
    }

    public record ContatoResposta(String id, String nome, String iniciais, String numero, String rotulo,
                                  boolean favorito, String cor) {

        static ContatoResposta de(Contato c) {
            return new ContatoResposta(c.id(), c.nome(), c.iniciais(), c.numero().formatado(), c.rotulo(), c.favorito(), c.cor());
        }
    }

    // ----- Navegador -----

    public record Web(List<AbaResposta> abas, int abaAtual, PaginaResposta pagina, boolean podeVoltar,
                      boolean podeAvancar, boolean favorita, List<Link> favoritos, List<Link> visitadas,
                      int atualizacoes, Instant carregadaEm) {

        static Web de(Navegador n) {
            Aba atual = n.getAbaAtual();
            return new Web(n.getAbas().stream().map(a -> AbaResposta.de(a, n)).toList(), atual.getId(),
                    PaginaResposta.de(n.paginaAtual()), atual.podeVoltar(), atual.podeAvancar(), n.isFavorita(),
                    n.getFavoritos().stream().map(u -> Link.de(u, n)).toList(),
                    n.getVisitadas().stream().limit(6).map(u -> Link.de(u, n)).toList(),
                    atual.getAtualizacoes(), atual.getCarregadaEm());
        }
    }

    public record AbaResposta(int id, String url, String dominio, String titulo, boolean privada) {

        static AbaResposta de(Aba a, Navegador n) {
            return new AbaResposta(a.getId(), a.getUrl(), Endereco.dominio(a.getUrl()), n.pagina(a.getUrl()).titulo(), a.isPrivada());
        }
    }

    public record Link(String url, String dominio, String titulo) {

        static Link de(String url, Navegador n) {
            return new Link(url, Endereco.dominio(url), n.pagina(url).titulo());
        }
    }

    public record PaginaResposta(String url, String dominio, String titulo, String tipo, String cor, List<BlocoResposta> blocos) {

        static PaginaResposta de(Pagina p) {
            return new PaginaResposta(p.url(), Endereco.dominio(p.url()), p.titulo(), p.tipo().name(), p.cor(),
                    p.blocos().stream().map(BlocoResposta::de).toList());
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BlocoResposta(String tipo, String titulo, String subtitulo, String texto, List<String> itens,
                                Boolean numerada, String valor, String legenda, List<Bloco.Cartao> cartoes) {

        /** Switch exaustivo sobre a interface selada: um tipo novo de bloco sem conversão não compila. */
        static BlocoResposta de(Bloco bloco) {
            return switch (bloco) {
                case Bloco.Cabecalho c -> new BlocoResposta("cabecalho", c.titulo(), c.subtitulo(), null, null, null, null, null, null);
                case Bloco.Paragrafo p -> new BlocoResposta("paragrafo", null, null, p.texto(), null, null, null, null, null);
                case Bloco.Lista l -> new BlocoResposta("lista", l.titulo(), null, null, l.itens(), l.numerada(), null, null, null);
                case Bloco.Destaque d -> new BlocoResposta("destaque", null, null, null, null, null, d.valor(), d.legenda(), null);
                case Bloco.Cartoes c -> new BlocoResposta("cartoes", c.titulo(), null, null, null, null, null, null, c.itens());
            };
        }
    }

    // ----- Rastro -----

    public record Chamado(long numero, Instant quando, String papel, String metodo, String assinatura, String motivo, String erro) {

        static Chamado de(Registro r) {
            return new Chamado(r.numero(), r.quando(), r.papel(), r.metodo(), r.assinatura(), r.motivo(), r.erro());
        }
    }
}
