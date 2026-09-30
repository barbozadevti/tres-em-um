package dev.barboza.tresemum;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Relógio dos testes: parado até alguém avançar. */
public class RelogioAjustavel extends Clock {

    public static final Instant INICIO = Instant.parse("2026-09-30T13:00:00Z");

    private Instant agora = INICIO;

    public void avancar(Duration tempo) {
        agora = agora.plus(tempo);
    }

    public void avancarSegundos(double segundos) {
        avancar(Duration.ofMillis(Math.round(segundos * 1000)));
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("America/Sao_Paulo");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
