package dev.barboza.tresemum.papeis;

/** Papel de navegador na internet. */
public interface NavegadorInternet {

    /** @param url endereço ou termo de busca, como na barra de endereços */
    void exibirPagina(String url);

    void adicionarNovaAba();

    void atualizarPagina();

    void voltar();

    void avancar();
}
