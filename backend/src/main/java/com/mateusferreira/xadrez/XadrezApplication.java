package com.mateusferreira.xadrez;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação Spring Boot.
 *
 * A anotação @SpringBootApplication é, na verdade, 3 anotações em 1:
 *
 *  - @Configuration       -> esta classe pode definir "beans" (objetos que
 *                            o Spring vai gerenciar para nós).
 *  - @EnableAutoConfiguration -> o Spring olha as dependências do classpath e
 *                            configura sozinho o que for possível (ex.: se
 *                            achar o starter-web, sobe um servidor; se achar
 *                            o JPA, prepara o banco). É a "mágica" do Boot.
 *  - @ComponentScan       -> o Spring varre ESTE pacote e os subpacotes
 *                            procurando classes anotadas (@Service, @Controller...)
 *                            para gerenciar. Por isso a classe principal fica
 *                            na "raiz" dos pacotes.
 */
@SpringBootApplication
public class XadrezApplication {

    public static void main(String[] args) {
        // Esta única linha inicializa todo o "contexto" do Spring:
        // ele cria e conecta os objetos da aplicação automaticamente.
        SpringApplication.run(XadrezApplication.class, args);
    }
}
