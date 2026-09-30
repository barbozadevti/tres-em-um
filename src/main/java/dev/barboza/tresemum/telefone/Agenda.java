package dev.barboza.tresemum.telefone;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Agenda de contatos (fictícios). */
public class Agenda {

    private final List<Contato> contatos;

    public Agenda(List<Contato> contatos) {
        this.contatos = contatos.stream().sorted(Comparator.comparing(Contato::nome)).toList();
    }

    public static Agenda exemplo() {
        return new Agenda(List.of(
                new Contato("ana", "Ana Souza", Numero.de("(27) 99812-3401"), "celular", true, true, "#ec4899"),
                new Contato("bruno", "Bruno Lima", Numero.de("(27) 99745-1122"), "celular", true, true, "#0ea5e9"),
                new Contato("carla", "Carla Mendes", Numero.de("(11) 98876-5500"), "trabalho", false, true, "#f59e0b"),
                new Contato("diego", "Diego Rocha", Numero.de("(21) 99654-7788"), "celular", false, false, "#10b981"),
                new Contato("mae", "Mãe", Numero.de("(27) 99901-2233"), "casa", true, true, "#8b5cf6"),
                new Contato("pizzaria", "Pizzaria Bella Praia", Numero.de("(27) 3225-4410"), "trabalho", false, true, "#ef4444"),
                new Contato("suporte", "Suporte da Operadora", Numero.de("0800 555 0101"), "trabalho", false, true, "#64748b")));
    }

    public List<Contato> todos() {
        return contatos;
    }

    public List<Contato> favoritos() {
        return contatos.stream().filter(Contato::favorito).toList();
    }

    public Optional<Contato> porNumero(Numero numero) {
        return contatos.stream().filter(c -> c.numero().equals(numero)).findFirst();
    }

    public Optional<Contato> porId(String id) {
        return contatos.stream().filter(c -> c.id().equals(id)).findFirst();
    }
}
