package dev.barboza.tresemum.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Política de segurança de conteúdo (CSP): o site só carrega scripts, estilos e dados da própria origem,
 * sem nada inline. O Swagger UI fica de fora porque depende de estilos inline.
 */
@Component
public class CabecalhosDeSeguranca extends OncePerRequestFilter {

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "connect-src 'self'; media-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        String caminho = pedido.getRequestURI();
        if (!caminho.startsWith("/swagger-ui") && !caminho.startsWith("/v3/api-docs") && !caminho.startsWith("/img/")) {
            resposta.setHeader("Content-Security-Policy", CSP);
        }
        resposta.setHeader("X-Content-Type-Options", "nosniff");
        resposta.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        cadeia.doFilter(pedido, resposta);
    }
}
