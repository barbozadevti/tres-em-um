package dev.barboza.tresemum.musica;

import java.util.regex.Pattern;

/**
 * Melodia em texto: notas separadas por espaço no formato {@code NOTA:BATIDAS}.
 * Ex.: {@code "E4:1 D#4:0.5 R:0.5"} (R é pausa). O frontend sintetiza o som a partir do mesmo texto,
 * então a duração calculada aqui é a mesma que se ouve no navegador.
 */
public final class Partitura {

    private static final Pattern NOTA = Pattern.compile("^(R|[A-G]#?[0-8]):(\\d+(?:\\.\\d+)?)$");

    private Partitura() {
    }

    /** Soma das batidas; lança exceção se alguma nota estiver mal escrita. */
    public static double batidas(String partitura) {
        if (partitura == null || partitura.isBlank()) {
            throw new IllegalArgumentException("Partitura vazia.");
        }
        double total = 0;
        for (String token : partitura.trim().split("\\s+")) {
            var m = NOTA.matcher(token);
            if (!m.matches()) {
                throw new IllegalArgumentException("Nota inválida na partitura: " + token);
            }
            double batidas = Double.parseDouble(m.group(2));
            if (batidas <= 0) {
                throw new IllegalArgumentException("Duração deve ser positiva: " + token);
            }
            total += batidas;
        }
        return total;
    }
}
