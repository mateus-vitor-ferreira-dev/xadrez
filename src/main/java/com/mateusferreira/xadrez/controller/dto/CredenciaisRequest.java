package com.mateusferreira.xadrez.controller.dto;

/** Corpo de cadastro/login: { "usuario": "...", "senha": "..." }. */
public record CredenciaisRequest(String usuario, String senha) {
}
