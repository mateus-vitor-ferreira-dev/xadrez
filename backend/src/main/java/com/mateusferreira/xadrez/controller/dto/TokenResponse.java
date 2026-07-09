package com.mateusferreira.xadrez.controller.dto;

/**
 * Resposta de autenticação: o token JWT + dados públicos do usuário. {@code admin}
 * deixa o front liberar a vitrine inteira (todas as skins) sem depender do rank.
 * {@code titulo} é o nome do enum {@link com.mateusferreira.xadrez.dominio.Titulo}
 * equipado (ou {@code null}) — o front resolve o rótulo para exibir.
 */
public record TokenResponse(String token, String usuario, String email, int elo, boolean admin, String titulo) {
}
