package com.mateusferreira.xadrez.controller.dto;

/** 1º login com Google: o credential + o apelido escolhido pelo usuário. */
public record GoogleFinalizarRequest(String credential, String apelido) {
}
