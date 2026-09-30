package dev.barboza.tresemum.config;

import java.io.IOException;
import java.time.Clock;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.web.context.annotation.SessionScope;

import dev.barboza.tresemum.aparelho.IPhone;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class Configuracao {

    private static final Logger log = LoggerFactory.getLogger(Configuracao.class);

    /** Relógio injetável: os testes trocam por um relógio que só anda quando mandam. */
    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }

    /** Um aparelho por visitante: o Spring injeta um proxy que aponta para o iPhone da sessão atual. */
    @Bean
    @SessionScope
    IPhone iphone(Clock relogio) {
        return IPhone.comExemplos(relogio, new Random());
    }

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI().info(new Info()
                .title("Três em Um — API do iPhone")
                .version("1.0")
                .description("As rotas seguem os três papéis do aparelho: ReprodutorMusical, AparelhoTelefonico e "
                        + "NavegadorInternet. Cada visitante tem o próprio aparelho, guardado na sessão."));
    }

    /** Usado pelo atalho: quando o servidor fica pronto, abre o site no navegador padrão (Windows). */
    @EventListener
    public void aoFicarPronto(ApplicationReadyEvent evento) {
        Environment ambiente = evento.getApplicationContext().getEnvironment();
        if (!ambiente.getProperty("tresemum.abrir-navegador", Boolean.class, false)
                || !(evento.getApplicationContext() instanceof WebServerApplicationContext web)) {
            return;
        }
        String endereco = "http://localhost:" + web.getWebServer().getPort();
        try {
            new ProcessBuilder("cmd", "/c", "start", "", endereco).start();
        } catch (IOException e) {
            log.warn("Não foi possível abrir o navegador; acesse {}", endereco);
        }
    }
}
