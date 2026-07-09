package com.mateusferreira.xadrez.controller.dto;

/** Corpo do login: identificador (e-mail OU apelido) e senha. */
public record LoginRequest(String identificador, String senha) {
}
