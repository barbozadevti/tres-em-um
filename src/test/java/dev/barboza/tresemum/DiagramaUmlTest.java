package dev.barboza.tresemum;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import dev.barboza.tresemum.papeis.AparelhoTelefonico;
import dev.barboza.tresemum.papeis.NavegadorInternet;
import dev.barboza.tresemum.papeis.ReprodutorMusical;

/**
 * "O diagrama não mente": lê docs/uml/tres-em-um.mmd e confere, por reflexão, que cada classe,
 * atributo, método e relação desenhados existem no código. Se alguém mudar o código e esquecer
 * o diagrama (ou o contrário), o build quebra.
 */
class DiagramaUmlTest {

    private static final Path DIAGRAMA = Path.of("docs/uml/tres-em-um.mmd");
    private static final List<String> PACOTES = List.of("papeis", "aparelho", "musica", "telefone", "navegador");

    private static final Pattern CLASSE = Pattern.compile("^\\s*class (\\w+)\\s*(\\{)?\\s*$");
    private static final Pattern METODO = Pattern.compile("^\\s*[+\\-#~](\\w+)\\((.*?)\\)(\\$)?\\s*(\\S+)?\\s*$");
    private static final Pattern ATRIBUTO = Pattern.compile("^\\s*[+\\-#~](\\w+):\\s*(\\S+)\\s*$");
    private static final Pattern RELACAO = Pattern.compile("^\\s*(\\w+)\\s*(<\\|\\.\\.|\\*--|o--|\\.\\.>)\\s*(\\w+)");

    record Membro(String nome, int parametros, boolean estatico) { }

    record Relacao(String de, String tipo, String para) { }

    private static final Map<String, List<Membro>> metodos = new LinkedHashMap<>();
    private static final Map<String, List<String>> atributos = new LinkedHashMap<>();
    private static final List<Relacao> relacoes = new ArrayList<>();

    @BeforeAll
    static void lerDiagrama() throws IOException {
        String classeAberta = null;
        for (String linha : Files.readAllLines(DIAGRAMA)) {
            Matcher m;
            if ((m = CLASSE.matcher(linha)).matches()) {
                metodos.putIfAbsent(m.group(1), new ArrayList<>());
                atributos.putIfAbsent(m.group(1), new ArrayList<>());
                classeAberta = m.group(2) != null ? m.group(1) : null;
            } else if (linha.trim().equals("}")) {
                classeAberta = null;
            } else if (classeAberta != null && (m = METODO.matcher(linha)).matches()) {
                int parametros = m.group(2).isBlank() ? 0 : m.group(2).split(",").length;
                metodos.get(classeAberta).add(new Membro(m.group(1), parametros, m.group(3) != null));
            } else if (classeAberta != null && (m = ATRIBUTO.matcher(linha)).matches()) {
                atributos.get(classeAberta).add(m.group(1));
            } else if ((m = RELACAO.matcher(linha)).find()) {
                relacoes.add(new Relacao(m.group(1), m.group(2), m.group(3)));
            }
        }
    }

    private static Optional<Class<?>> classe(String nome) {
        for (String pacote : PACOTES) {
            try {
                return Optional.of(Class.forName("dev.barboza.tresemum." + pacote + "." + nome));
            } catch (ClassNotFoundException e) {
                // tenta o próximo pacote
            }
        }
        return Optional.empty();
    }

    private static Class<?> exigir(String nome) {
        return classe(nome).orElseThrow(() -> new AssertionError("Classe do diagrama não existe no código: " + nome));
    }

    @Test
    void diagramaFoiLidoPorInteiro() {
        assertThat(metodos).hasSize(12);
        assertThat(relacoes).hasSize(15);
    }

    @TestFactory
    List<DynamicTest> cadaClasseDoDiagramaExisteComSeusMembros() {
        return metodos.keySet().stream().map(nome -> DynamicTest.dynamicTest(nome, () -> {
            Class<?> tipo = exigir(nome);
            for (Membro membro : metodos.get(nome)) {
                boolean existe = Arrays.stream(tipo.getMethods()).anyMatch(metodo ->
                        metodo.getName().equals(membro.nome())
                                && metodo.getParameterCount() == membro.parametros()
                                && Modifier.isStatic(metodo.getModifiers()) == membro.estatico());
                assertThat(existe).as("%s.%s com %d parâmetro(s)", nome, membro.nome(), membro.parametros()).isTrue();
            }
            for (String atributo : atributos.get(nome)) {
                assertThat(Arrays.stream(tipo.getDeclaredFields()).map(Field::getName))
                        .as("atributo %s.%s", nome, atributo).contains(atributo);
            }
        })).toList();
    }

    @TestFactory
    List<DynamicTest> cadaRelacaoDoDiagramaExisteNoCodigo() {
        return relacoes.stream().map(r -> DynamicTest.dynamicTest(r.de() + " " + r.tipo() + " " + r.para(), () -> {
            Class<?> de = exigir(r.de());
            Class<?> para = exigir(r.para());
            switch (r.tipo()) {
                case "<|.." -> {
                    assertThat(de.isInterface()).as("%s é interface", r.de()).isTrue();
                    assertThat(de.isAssignableFrom(para)).as("%s implementa %s", r.para(), r.de()).isTrue();
                }
                case "*--", "o--" -> assertThat(Arrays.stream(de.getDeclaredFields())
                        .anyMatch(campo -> campo.getType() == para || campo.getGenericType().getTypeName().contains(para.getName())))
                        .as("%s tem um atributo do tipo %s", r.de(), r.para()).isTrue();
                case "..>" -> assertThat(para).isNotNull();
                default -> throw new AssertionError("Relação desconhecida: " + r.tipo());
            }
        })).toList();
    }

    @Test
    void todoMetodoDosTresPapeisEstaNoDiagrama() {
        for (Class<?> papel : List.of(ReprodutorMusical.class, AparelhoTelefonico.class, NavegadorInternet.class)) {
            List<String> desenhados = metodos.get(papel.getSimpleName()).stream().map(Membro::nome).toList();
            assertThat(desenhados).as(papel.getSimpleName())
                    .containsExactlyInAnyOrderElementsOf(Arrays.stream(papel.getDeclaredMethods()).map(Method::getName).toList());
        }
    }
}
