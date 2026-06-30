package com.mateusferreira.xadrez.controller.dto;

/**
 * DTO de ENTRADA: o corpo (JSON) de uma jogada, ex.:
 *   { "origem": "e2", "destino": "e4" }
 *
 * O Jackson (via @RequestBody) preenche este record casando os nomes dos campos.
 */
public record JogadaRequest(String origem, String destino) {
}
