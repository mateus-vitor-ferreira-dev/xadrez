package com.mateusferreira.xadrez.controller.dto;

/**
 * DTO de ENTRADA: o corpo (JSON) de uma jogada, ex.:
 *   { "origem": "e2", "destino": "e4" }
 *   { "origem": "a7", "destino": "a8", "promocao": "RAINHA" }
 *
 * 'promocao' é opcional: só importa quando um peão chega à última fileira.
 * Se vier nulo, o backend assume RAINHA.
 */
public record JogadaRequest(String origem, String destino, String promocao) {
}
