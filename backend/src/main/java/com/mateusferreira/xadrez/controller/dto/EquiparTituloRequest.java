package com.mateusferreira.xadrez.controller.dto;

/**
 * Corpo do {@code PUT /usuario/titulo}. {@code titulo} é o nome do enum
 * {@link com.mateusferreira.xadrez.dominio.Titulo} (ex.: "CAVALEIRO"); {@code null}
 * ou vazio remove o título exibido.
 */
public record EquiparTituloRequest(String titulo) {
}
