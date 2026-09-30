package dev.barboza.tresemum.navegador;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.barboza.tresemum.navegador.Bloco.Cabecalho;
import dev.barboza.tresemum.navegador.Bloco.Cartao;
import dev.barboza.tresemum.navegador.Bloco.Cartoes;
import dev.barboza.tresemum.navegador.Bloco.Destaque;
import dev.barboza.tresemum.navegador.Bloco.Lista;
import dev.barboza.tresemum.navegador.Bloco.Paragrafo;

/**
 * A "internet" do simulador: poucos sites fictícios (domínio .exemplo), servidos pelo próprio backend.
 * Sites de verdade não abrem dentro de outra página (bloqueio de iframe), e assim a navegação
 * inteira fica testável.
 */
public class Web {

    private static final String AVISO = "Site fictício, criado para o simulador Três em Um.";

    private record Artigo(String titulo, String resumo, List<String> paragrafos) { }

    private static final Map<String, Artigo> NOTICIAS = Map.of(
            "/iphone-em-java", new Artigo("Desenvolvedor capixaba modela o iPhone em UML e o faz funcionar no navegador",
                    "Três interfaces Java, uma máquina de estados para as chamadas e um sintetizador para as músicas.",
                    List.of("O projeto parte de um exercício clássico de orientação a objetos: representar o iPhone como "
                                    + "reprodutor musical, aparelho telefônico e navegador de internet.",
                            "Cada papel virou uma interface. O aparelho implementa as três e coordena os papéis: "
                                    + "quando chega uma ligação, a música pausa e volta sozinha no fim da chamada.",
                            "O diagrama UML fica no repositório e um teste automatizado confere se ele continua igual ao código.")),
            "/festival-da-moqueca", new Artigo("Festival reúne 40 panelas de barro na orla de Camburi",
                    "Evento fictício celebra a moqueca capixaba e as paneleiras de Goiabeiras.",
                    List.of("As panelas de barro, feitas à mão pelas paneleiras de Goiabeiras, são o centro da festa.",
                            "A organização promete uma panela gigante no encerramento, com receita tradicional: "
                                    + "peixe, tomate, cebola, coentro, urucum e azeite, sem leite de coco.")),
            "/ciclovia-aos-domingos", new Artigo("Orla ganha faixa exclusiva para bicicletas aos domingos",
                    "Faixa funcionará das 7h às 13h, segundo a prefeitura fictícia do simulador.",
                    List.of("A faixa segue do Canal de Camburi até a Praia do Canto.",
                            "Nos outros dias, o trânsito volta ao normal.")));

    private static final List<Cartao> INDICE = List.of(
            new Cartao("Folha Capixaba", "Notícias de Vitória e do Espírito Santo.", "https://noticias.exemplo/"),
            new Cartao(NOTICIAS.get("/iphone-em-java").titulo(), "iPhone UML Java interfaces", "https://noticias.exemplo/iphone-em-java"),
            new Cartao(NOTICIAS.get("/festival-da-moqueca").titulo(), "moqueca festival panela de barro Camburi", "https://noticias.exemplo/festival-da-moqueca"),
            new Cartao(NOTICIAS.get("/ciclovia-aos-domingos").titulo(), "bicicleta ciclovia orla domingo", "https://noticias.exemplo/ciclovia-aos-domingos"),
            new Cartao("Clima em Vitória", "Previsão do tempo para a semana.", "https://clima.exemplo/"),
            new Cartao("Moqueca capixaba", "Receita tradicional, na panela de barro.", "https://receitas.exemplo/moqueca-capixaba"),
            new Cartao("9 de janeiro de 2007: três aparelhos em um", "A apresentação do iPhone na Macworld.", "https://keynote.exemplo/"),
            new Cartao("Rafael Barboza", "Desenvolvedor .NET e Java. Projetos e contato.", "https://barboza.dev/"));

    public Pagina abrir(String url) {
        if (Endereco.PAGINA_INICIAL.equals(url)) {
            return Pagina.inicio();
        }
        URI uri = URI.create(url);
        String caminho = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
        Pagina pagina = switch (uri.getHost()) {
            case "noticias.exemplo" -> noticias(url, caminho);
            case "clima.exemplo" -> caminho.equals("/") ? clima(url) : null;
            case "receitas.exemplo" -> caminho.equals("/") || caminho.equals("/moqueca-capixaba") ? moqueca(url) : null;
            case "keynote.exemplo" -> caminho.equals("/") ? keynote(url) : null;
            case "barboza.dev" -> caminho.equals("/") ? autor(url) : null;
            case "busca.exemplo" -> busca(url, uri.getRawQuery());
            default -> Pagina.erro(url, "Não foi possível abrir a página",
                    "O servidor \"" + uri.getHost() + "\" não foi encontrado. Este simulador conhece só alguns sites: "
                            + "experimente noticias.exemplo, clima.exemplo, receitas.exemplo ou keynote.exemplo.");
        };
        return pagina != null ? pagina
                : Pagina.erro(url, "Página não encontrada", "O endereço " + caminho + " não existe neste site (erro 404).");
    }

    private Pagina noticias(String url, String caminho) {
        if (caminho.equals("/")) {
            List<Cartao> manchetes = NOTICIAS.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new Cartao(e.getValue().titulo(), e.getValue().resumo(), "https://noticias.exemplo" + e.getKey()))
                    .toList();
            return new Pagina(url, "Folha Capixaba", Pagina.Tipo.SITE, "#b91c1c", List.of(
                    new Cabecalho("Folha Capixaba", "Notícias de Vitória e região"),
                    new Cartoes("Manchetes", manchetes),
                    new Paragrafo(AVISO)));
        }
        Artigo artigo = NOTICIAS.get(caminho);
        if (artigo == null) {
            return null;
        }
        List<Bloco> blocos = new java.util.ArrayList<>();
        blocos.add(new Cabecalho(artigo.titulo(), artigo.resumo()));
        artigo.paragrafos().forEach(p -> blocos.add(new Paragrafo(p)));
        blocos.add(new Cartoes("Mais notícias", List.of(new Cartao("Voltar para a capa", "Todas as manchetes", "https://noticias.exemplo/"))));
        return new Pagina(url, artigo.titulo(), Pagina.Tipo.SITE, "#b91c1c", blocos);
    }

    private Pagina clima(String url) {
        return new Pagina(url, "Clima em Vitória", Pagina.Tipo.SITE, "#0284c7", List.of(
                new Cabecalho("Vitória, ES", "Parcialmente nublado, brisa do mar"),
                new Destaque("27°", "Máxima 29° · Mínima 22° · Umidade 74%"),
                new Lista("Próximos dias", List.of(
                        "Quinta: 28° / 22°, sol entre nuvens",
                        "Sexta: 26° / 21°, pancadas à tarde",
                        "Sábado: 29° / 22°, ensolarado",
                        "Domingo: 30° / 23°, ensolarado"), false),
                new Paragrafo(AVISO)));
    }

    private Pagina moqueca(String url) {
        return new Pagina(url, "Moqueca capixaba", Pagina.Tipo.SITE, "#c2410c", List.of(
                new Cabecalho("Moqueca capixaba", "Serve 4 pessoas · 40 minutos · na panela de barro"),
                new Lista("Ingredientes", List.of(
                        "1 kg de peixe em postas (badejo, robalo ou dourado)",
                        "4 tomates maduros em rodelas",
                        "2 cebolas em rodelas",
                        "1 maço de coentro e cebolinha",
                        "Suco de 2 limões, sal e alho",
                        "Urucum e azeite a gosto"), false),
                new Lista("Modo de preparo", List.of(
                        "Tempere o peixe com limão, sal e alho e deixe descansar por 20 minutos.",
                        "Na panela de barro, faça camadas de cebola, tomate e peixe, com coentro entre elas.",
                        "Regue com azeite e urucum, tampe e cozinhe em fogo baixo por cerca de 20 minutos, sem mexer.",
                        "Sirva na própria panela, com arroz branco e pirão."), true),
                new Paragrafo("Na moqueca capixaba não vai leite de coco nem dendê. " + AVISO)));
    }

    private Pagina keynote(String url) {
        return new Pagina(url, "Três aparelhos em um", Pagina.Tipo.SITE, "#111827", List.of(
                new Cabecalho("9 de janeiro de 2007: três aparelhos em um", "Macworld, São Francisco"),
                new Paragrafo("A Apple anunciou que lançaria três produtos: um iPod com tela sensível ao toque, "
                        + "um celular e um aparelho para acessar a internet. Depois de repetir a lista algumas vezes, "
                        + "revelou que não eram três aparelhos, e sim um só: o iPhone."),
                new Lista("O que este simulador reproduz", List.of(
                        "Deslizar para desbloquear",
                        "Correio de voz visual: os recados em lista, ouvidos em qualquer ordem",
                        "A música pausa sozinha quando chega uma ligação e volta quando ela termina",
                        "Navegador com várias abas, voltar, avançar e atualizar"), false),
                new Paragrafo("Cada papel do aparelho é uma interface Java: ReprodutorMusical, AparelhoTelefonico "
                        + "e NavegadorInternet. O iPhone implementa as três."),
                new Paragrafo(AVISO)));
    }

    private Pagina autor(String url) {
        return new Pagina(url, "Rafael Barboza", Pagina.Tipo.SITE, "#4f46e5", List.of(
                new Cabecalho("Rafael Barboza", "Desenvolvedor .NET e Java"),
                new Paragrafo("Este simulador faz parte do meu portfólio. Os links abaixo abrem o GitHub de verdade, em outra aba."),
                new Cartoes("Projetos", List.of(
                        new Cartao("Três em Um", "Este simulador: UML, Java 21 e Spring Boot.", "https://github.com/barbozadevti/tres-em-um"),
                        new Cartao("Cofre", "Banco digital com Pix, cheque especial e perfis.", "https://github.com/barbozadevti/cofre"),
                        new Cartao("Crivo", "Sistema de recrutamento com Kanban e máquina de estados.", "https://github.com/barbozadevti/crivo"),
                        new Cartao("Todos os projetos", "Perfil no GitHub.", "https://github.com/barbozadevti")))));
    }

    private Pagina busca(String url, String consulta) {
        String termo = "";
        if (consulta != null) {
            for (String par : consulta.split("&")) {
                if (par.startsWith("q=")) {
                    termo = URLDecoder.decode(par.substring(2), StandardCharsets.UTF_8).strip();
                }
            }
        }
        List<String> palavras = Arrays.stream(semAcento(termo).split("\\s+")).filter(p -> p.length() >= 3).toList();
        List<Cartao> resultados = INDICE.stream()
                .filter(c -> palavras.stream().anyMatch(p -> semAcento(c.titulo() + " " + c.texto()).contains(p)))
                .map(c -> new Cartao(c.titulo(), Endereco.dominio(c.url()), c.url()))
                .toList();
        List<Bloco> blocos = resultados.isEmpty()
                ? List.of(new Cabecalho("Busca", "\"" + termo + "\""),
                        new Paragrafo("Nenhum resultado. Experimente: moqueca, clima, iPhone, notícias."))
                : List.of(new Cabecalho("Busca", resultados.size() + (resultados.size() == 1 ? " resultado" : " resultados")
                        + " para \"" + termo + "\""), new Cartoes(null, resultados));
        return new Pagina(url, termo.isEmpty() ? "Busca" : termo + " - Busca", Pagina.Tipo.BUSCA, "#16a34a", blocos);
    }

    static String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
