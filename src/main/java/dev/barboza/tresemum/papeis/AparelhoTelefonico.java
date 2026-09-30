package dev.barboza.tresemum.papeis;

/** Papel de aparelho telefônico. */
public interface AparelhoTelefonico {

    void ligar(String numero);

    void atender();

    /** Toca o próximo recado não ouvido da caixa postal. */
    void iniciarCorreioVoz();

    void recusar();

    void encerrar();
}
