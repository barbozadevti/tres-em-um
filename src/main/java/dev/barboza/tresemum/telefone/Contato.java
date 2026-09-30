package dev.barboza.tresemum.telefone;

/**
 * Contato da agenda.
 *
 * @param atende se o contato atende quando recebe uma ligação (na simulação, alguns nunca atendem)
 */
public record Contato(String id, String nome, Numero numero, String rotulo, boolean favorito, boolean atende, String cor) {

    public String iniciais() {
        String[] partes = nome.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        return partes.length == 1 ? primeira.toUpperCase() : (primeira + partes[partes.length - 1].charAt(0)).toUpperCase();
    }
}
