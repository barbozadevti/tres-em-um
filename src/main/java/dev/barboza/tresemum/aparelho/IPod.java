package dev.barboza.tresemum.aparelho;

import java.time.Clock;
import java.util.Random;

import dev.barboza.tresemum.musica.Biblioteca;
import dev.barboza.tresemum.musica.Reprodutor;
import dev.barboza.tresemum.papeis.ReprodutorMusical;

/**
 * Aparelho só de música. Existe para mostrar a segregação de interfaces: reaproveita o mesmo
 * {@link Reprodutor} do iPhone sem carregar nenhum método de telefone ou de navegador.
 */
public class IPod implements ReprodutorMusical {

    private final Reprodutor reprodutor;

    public IPod(Clock relogio) {
        this.reprodutor = new Reprodutor(new Biblioteca(), relogio, new Random());
    }

    @Override
    public void tocar() {
        reprodutor.tocar();
    }

    @Override
    public void pausar() {
        reprodutor.pausar();
    }

    @Override
    public void selecionarMusica(String musica) {
        reprodutor.selecionarMusica(musica);
    }

    @Override
    public void proxima() {
        reprodutor.proxima();
    }

    @Override
    public void anterior() {
        reprodutor.anterior();
    }

    public Reprodutor getReprodutor() {
        return reprodutor;
    }
}
