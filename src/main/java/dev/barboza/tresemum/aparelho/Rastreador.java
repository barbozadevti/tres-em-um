package dev.barboza.tresemum.aparelho;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/**
 * Envolve um papel num proxy dinâmico (java.lang.reflect.Proxy) que registra cada chamada no {@link Rastro}.
 * É o mesmo mecanismo que o Spring usa para transações e segurança: quem chama só enxerga a interface.
 */
public final class Rastreador {

    private Rastreador() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T rastrear(Class<T> papel, T alvo, Rastro rastro) {
        if (!papel.isInterface()) {
            throw new IllegalArgumentException(papel.getSimpleName() + " não é uma interface.");
        }
        return (T) Proxy.newProxyInstance(papel.getClassLoader(), new Class<?>[] {papel}, (proxy, metodo, argumentos) -> {
            if (metodo.getDeclaringClass() == Object.class) {
                return metodo.invoke(alvo, argumentos);
            }
            try {
                Object resultado = metodo.invoke(alvo, argumentos);
                rastro.registrar(papel.getSimpleName(), metodo.getName(), argumentos, null);
                return resultado;
            } catch (InvocationTargetException e) {
                rastro.registrar(papel.getSimpleName(), metodo.getName(), argumentos, e.getCause().getMessage());
                throw e.getCause();
            }
        });
    }
}
