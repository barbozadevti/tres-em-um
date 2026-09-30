package dev.barboza.tresemum.musica;

import java.time.Duration;

/**
 * Uma música da biblioteca. Todas são de domínio público e tocadas por um sintetizador
 * no navegador, a partir da {@link Partitura}.
 *
 * @param baixo acompanhamento opcional que se repete durante a melodia (pode ser vazio)
 */
public record Faixa(String id, String titulo, String autor, String ano, int bpm,
                    String partitura, String baixo, String cor, String corSecundaria) {

    public Faixa {
        if (bpm < 30 || bpm > 240) {
            throw new IllegalArgumentException("Andamento fora da faixa: " + bpm);
        }
        Partitura.batidas(partitura);
        if (baixo != null && !baixo.isBlank()) {
            Partitura.batidas(baixo);
        }
    }

    public Duration duracao() {
        return Duration.ofMillis(Math.round(Partitura.batidas(partitura) * 60_000.0 / bpm));
    }
}
