package dev.barboza.tresemum.aparelho;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Uma chamada de método em um dos papéis do aparelho, como aparece no painel "Por dentro".
 *
 * @param motivo por que o próprio aparelho fez a chamada (ex.: "chamada recebida"); nulo quando veio do usuário
 * @param erro mensagem da exceção, se a chamada foi recusada
 */
public record Registro(long numero, Instant quando, String papel, String metodo, List<String> argumentos,
                       String motivo, String erro) {

    /** Ex.: {@code ReprodutorMusical.selecionarMusica("fur-elise")}. */
    public String assinatura() {
        return papel + "." + metodo + "(" + argumentos.stream().collect(Collectors.joining(", ")) + ")";
    }
}
