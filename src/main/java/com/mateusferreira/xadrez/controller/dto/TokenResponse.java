package com.mateusferreira.xadrez.controller.dto;

/** Resposta de autenticação: o token JWT + dados públicos do usuário. */
public record TokenResponse(String token, String usuario, String email, int elo) {
}
