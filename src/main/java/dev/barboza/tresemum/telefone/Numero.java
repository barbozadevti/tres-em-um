package dev.barboza.tresemum.telefone;

import java.util.Set;

import dev.barboza.tresemum.papeis.RegraVioladaException;

/**
 * Número de telefone brasileiro, normalizado só com dígitos (DDD + número, sem o 55).
 * Aceita o que se digita no dia a dia: "(27) 99876-5432", "+55 27 3223-4567", "190", "0800 123 4567".
 * Sem DDD, usa o DDD do aparelho (27, Vitória).
 */
public record Numero(String digitos) {

    public static final String DDD_PADRAO = "27";

    private static final Set<String> DDDS = Set.of(
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "21", "22", "24", "27", "28",
            "31", "32", "33", "34", "35", "37", "38", "41", "42", "43", "44", "45", "46", "47", "48", "49",
            "51", "53", "54", "55", "61", "62", "63", "64", "65", "66", "67", "68", "69",
            "71", "73", "74", "75", "77", "79", "81", "82", "83", "84", "85", "86", "87", "88", "89",
            "91", "92", "93", "94", "95", "96", "97", "98", "99");

    /** Serviços públicos de 3 dígitos. */
    private static final Set<String> SERVICOS = Set.of("100", "180", "188", "190", "191", "192", "193", "197", "199");
    private static final Set<String> EMERGENCIAS = Set.of("190", "192", "193");

    public static Numero de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new RegraVioladaException("Digite um número.");
        }
        if (!texto.matches("[0-9+()\\s.-]+")) {
            throw new RegraVioladaException("O número só pode ter dígitos, espaços, parênteses, + e -.");
        }
        String d = texto.replaceAll("[^0-9]", "");
        if (d.length() == 3) {
            if (!SERVICOS.contains(d)) {
                throw new RegraVioladaException("Serviço desconhecido: " + d + ".");
            }
            return new Numero(d);
        }
        if (d.startsWith("0800")) {
            if (d.length() != 11) {
                throw new RegraVioladaException("Número 0800 deve ter 11 dígitos.");
            }
            return new Numero(d);
        }
        if (d.startsWith("55") && (d.length() == 12 || d.length() == 13)) {
            d = d.substring(2);
        } else if (d.startsWith("0") && (d.length() == 11 || d.length() == 12)) {
            d = d.substring(1);
        }
        if (d.length() == 8 || d.length() == 9) {
            d = DDD_PADRAO + d;
        }
        if (d.length() != 10 && d.length() != 11) {
            throw new RegraVioladaException("Número incompleto ou longo demais: " + texto.trim() + ".");
        }
        String ddd = d.substring(0, 2);
        if (!DDDS.contains(ddd)) {
            throw new RegraVioladaException("DDD " + ddd + " não existe.");
        }
        char primeiro = d.charAt(2);
        if (d.length() == 11 && primeiro != '9') {
            throw new RegraVioladaException("Celular começa com 9.");
        }
        if (d.length() == 10 && (primeiro < '2' || primeiro > '5')) {
            throw new RegraVioladaException("Telefone fixo começa com 2, 3, 4 ou 5.");
        }
        return new Numero(d);
    }

    public boolean emergencia() {
        return EMERGENCIAS.contains(digitos);
    }

    public String formatado() {
        if (digitos.length() == 3) {
            return digitos;
        }
        if (digitos.startsWith("0800")) {
            return "0800 " + digitos.substring(4, 7) + " " + digitos.substring(7);
        }
        String ddd = digitos.substring(0, 2);
        String resto = digitos.substring(2);
        int corte = resto.length() - 4;
        return "(" + ddd + ") " + resto.substring(0, corte) + "-" + resto.substring(corte);
    }

    @Override
    public String toString() {
        return formatado();
    }
}
