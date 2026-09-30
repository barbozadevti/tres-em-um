package dev.barboza.tresemum.aparelho;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Guarda as últimas chamadas feitas aos papéis do aparelho. */
public class Rastro {

    public static final int MAXIMO = 80;

    private final Clock relogio;
    private final List<Registro> registros = new ArrayList<>();
    private long sequencia;
    private String motivo;

    public Rastro(Clock relogio) {
        this.relogio = relogio;
    }

    public void registrar(String papel, String metodo, Object[] argumentos, String erro) {
        List<String> textos = argumentos == null ? List.of()
                : Arrays.stream(argumentos).map(Rastro::literal).toList();
        registros.addFirst(new Registro(++sequencia, relogio.instant(), papel, metodo, textos, motivo, erro));
        if (registros.size() > MAXIMO) {
            registros.removeLast();
        }
    }

    /** Executa a ação marcando as chamadas feitas nela com o motivo (o aparelho agindo sozinho). */
    public void comMotivo(String motivo, Runnable acao) {
        String anterior = this.motivo;
        this.motivo = motivo;
        try {
            acao.run();
        } finally {
            this.motivo = anterior;
        }
    }

    /** Mais recentes primeiro. */
    public List<Registro> recentes(int quantidade) {
        return List.copyOf(registros.subList(0, Math.min(quantidade, registros.size())));
    }

    private static String literal(Object valor) {
        return valor instanceof String texto ? "\"" + texto + "\"" : String.valueOf(valor);
    }
}
