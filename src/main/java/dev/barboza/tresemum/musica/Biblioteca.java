package dev.barboza.tresemum.musica;

import java.util.List;
import java.util.Optional;

/** As músicas do aparelho: melodias de domínio público, transcritas nota a nota. */
public final class Biblioteca {

    private static final String ODE_A = "E4:1 E4:1 F4:1 G4:1 G4:1 F4:1 E4:1 D4:1 C4:1 C4:1 D4:1 E4:1 ";
    private static final String MINUETO_A = "D5:1 G4:0.5 A4:0.5 B4:0.5 C5:0.5 D5:1 G4:1 G4:1 "
            + "E5:1 C5:0.5 D5:0.5 E5:0.5 F#5:0.5 G5:1 G4:1 G4:1 "
            + "C5:1 D5:0.5 C5:0.5 B4:0.5 A4:0.5 B4:1 C5:0.5 B4:0.5 A4:0.5 G4:0.5 ";
    private static final String ELISE = "E5:1 D#5:1 E5:1 D#5:1 E5:1 B4:1 D5:1 C5:1 A4:2 R:1 C4:1 E4:1 A4:1 "
            + "B4:2 R:1 E4:1 G#4:1 B4:1 C5:2 R:1 E4:1 E5:1 D#5:1 "
            + "E5:1 D#5:1 E5:1 B4:1 D5:1 C5:1 A4:2 R:1 C4:1 E4:1 A4:1 B4:2 R:1 E4:1 C5:1 B4:1 A4:3 R:1 ";
    private static final String GREEN_A = "A4:1 C5:2 D5:1 E5:1.5 F5:0.5 E5:1 D5:2 B4:1 G4:1.5 A4:0.5 B4:1 "
            + "C5:2 A4:1 A4:1.5 G#4:0.5 A4:1 B4:2 G#4:1 E4:2 ";
    private static final String GREEN_FIM = "C5:1.5 B4:0.5 A4:1 G#4:1.5 F#4:0.5 G#4:1 A4:3 ";
    private static final String GREEN_REFRAO = "G5:3 G5:1.5 F#5:0.5 E5:1 D5:2 B4:1 G4:1.5 A4:0.5 B4:1 ";

    private static final List<Faixa> FAIXAS = List.of(
            new Faixa("ode-a-alegria", "Ode à Alegria", "Ludwig van Beethoven", "1824", 132,
                    ODE_A + "E4:1.5 D4:0.5 D4:2 " + ODE_A + "D4:1.5 C4:0.5 C4:2 "
                            + "D4:1 D4:1 E4:1 C4:1 D4:1 E4:0.5 F4:0.5 E4:1 C4:1 "
                            + "D4:1 E4:0.5 F4:0.5 E4:1 D4:1 C4:1 D4:1 G3:2 "
                            + ODE_A + "D4:1.5 C4:0.5 C4:2",
                    "C3:4 G2:4 C3:4 G2:2 C3:2", "#f59e0b", "#ef4444"),
            new Faixa("fur-elise", "Für Elise", "Ludwig van Beethoven", "1810", 150,
                    ELISE + ELISE, "", "#ec4899", "#8b5cf6"),
            new Faixa("minueto-em-sol", "Minueto em Sol", "Christian Petzold", "1725", 120,
                    MINUETO_A + "F#4:1 G4:0.5 A4:0.5 B4:0.5 G4:0.5 A4:3 "
                            + MINUETO_A + "A4:1 B4:0.5 A4:0.5 G4:0.5 F#4:0.5 G4:3",
                    "G2:3 D3:3 C3:3 G2:3", "#10b981", "#0ea5e9"),
            new Faixa("brilha-estrelinha", "Brilha, Brilha, Estrelinha", "Tradicional", "1761", 110,
                    "C4:1 C4:1 G4:1 G4:1 A4:1 A4:1 G4:2 F4:1 F4:1 E4:1 E4:1 D4:1 D4:1 C4:2 "
                            + "G4:1 G4:1 F4:1 F4:1 E4:1 E4:1 D4:2 G4:1 G4:1 F4:1 F4:1 E4:1 E4:1 D4:2 "
                            + "C4:1 C4:1 G4:1 G4:1 A4:1 A4:1 G4:2 F4:1 F4:1 E4:1 E4:1 D4:1 D4:1 C4:2",
                    "C3:4 F2:2 C3:2 G2:4 C3:4", "#6366f1", "#22d3ee"),
            new Faixa("greensleeves", "Greensleeves", "Tradicional inglesa", "1580", 150,
                    GREEN_A + "A4:1 C5:2 D5:1 E5:1.5 F5:0.5 E5:1 D5:2 B4:1 G4:1.5 A4:0.5 B4:1 " + GREEN_FIM
                            + GREEN_REFRAO + "C5:2 A4:1 A4:1.5 G#4:0.5 A4:1 B4:2 G#4:1 E4:3 "
                            + GREEN_REFRAO + GREEN_FIM,
                    "A2:6 G2:6 A2:6 E2:6", "#14b8a6", "#166534"),
            new Faixa("canon-em-re", "Canon em Ré", "Johann Pachelbel", "1680", 90,
                    "F#5:2 E5:2 D5:2 C#5:2 B4:2 A4:2 B4:2 C#5:2 "
                            + "D5:2 C#5:2 B4:2 A4:2 G4:2 F#4:2 G4:2 E4:2 "
                            + "F#5:1 D5:0.5 E5:0.5 F#5:1 D5:0.5 E5:0.5 F#5:0.5 F#4:0.5 G4:0.5 A4:0.5 "
                            + "B4:0.5 C#5:0.5 D5:0.5 E5:0.5 D5:1 B4:0.5 C#5:0.5 D5:1 D4:0.5 E4:0.5 "
                            + "F#4:0.5 G4:0.5 F#4:0.5 E4:0.5 F#4:0.5 D4:0.5 E4:0.5 F#4:0.5 D4:4",
                    "D3:2 A2:2 B2:2 F#2:2 G2:2 D2:2 G2:2 A2:2", "#f43f5e", "#f97316"));

    public List<Faixa> todas() {
        return FAIXAS;
    }

    public Optional<Faixa> porId(String id) {
        return FAIXAS.stream().filter(f -> f.id().equals(id)).findFirst();
    }
}
