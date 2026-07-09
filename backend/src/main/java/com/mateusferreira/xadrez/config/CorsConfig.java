package com.mateusferreira.xadrez.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de CORS.
 *
 * Em produção, o frontend (ex.: https://xadrez.vercel.app) e o backend
 * (ex.: https://xadrez.up.railway.app) ficam em ORIGENS diferentes. Por padrão,
 * o navegador BLOQUEIA chamadas entre origens diferentes — a menos que o
 * servidor diga "pode confiar nessa origem". Isso é o CORS.
 *
 * Aqui liberamos as origens listadas na propriedade 'app.cors.allowed-origins'
 * (no local, o default é o Vite; em produção, definimos a URL do Vercel via a
 * variável de ambiente APP_CORS_ALLOWED_ORIGINS — pode ser uma lista separada
 * por vírgulas).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String[] origensPermitidas;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // allowedOriginPatterns (em vez de allowedOrigins) aceita curingas, ex.:
        // "https://*.vercel.app" — útil porque cada preview da Vercel tem uma URL.
        registry.addMapping("/**")
                .allowedOriginPatterns(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
