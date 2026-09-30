package dev.barboza.tresemum.papeis;

/** Ação que não faz sentido no estado atual (pausar sem nada tocando, atender sem chamada...). Vira HTTP 409. */
public class EstadoInvalidoException extends RuntimeException {

    public EstadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
