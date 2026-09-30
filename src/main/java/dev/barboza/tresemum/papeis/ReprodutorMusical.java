package dev.barboza.tresemum.papeis;

/** Papel de reprodutor musical: o "iPod" do iPhone. */
public interface ReprodutorMusical {

    void tocar();

    void pausar();

    /** @param musica identificador da faixa na biblioteca */
    void selecionarMusica(String musica);

    void proxima();

    void anterior();
}
