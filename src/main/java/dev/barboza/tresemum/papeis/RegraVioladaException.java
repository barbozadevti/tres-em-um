package dev.barboza.tresemum.papeis;

/** Pedido que fere uma regra do aparelho (número inválido, limite de abas...). Vira HTTP 422. */
public class RegraVioladaException extends RuntimeException {

    public RegraVioladaException(String mensagem) {
        super(mensagem);
    }
}
