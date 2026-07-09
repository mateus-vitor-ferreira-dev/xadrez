package com.mateusferreira.xadrez.controller.dto;

/**
 * DTO de SAÍDA para erros: o corpo JSON devolvido quando algo dá errado, ex.:
 *   { "erro": "Movimento ilegal: e2 -> e5." }
 *
 * Ter um formato fixo de erro deixa a API previsível para quem a consome.
 */
public record ErroResponse(String erro) {
}
