package com.mateusferreira.xadrez.controller.dto;

/** Corpo do cadastro: apelido único, e-mail único e senha. */
public record RegistroRequest(String usuario, String email, String senha) {
}
