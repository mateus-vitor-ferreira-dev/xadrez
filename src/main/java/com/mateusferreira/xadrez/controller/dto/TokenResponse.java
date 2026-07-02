package com.mateusferreira.xadrez.controller.dto;

/**
 * Resposta de autenticação: o token JWT + dados públicos do usuário. O campo
 * {@code admin} deixa o front liberar a vitrine inteira (todas as skins) para
 * contas administradoras, sem depender do rank/Elo.
 */
public record TokenResponse(String token, String usuario, String email, int elo, boolean admin) {
}
