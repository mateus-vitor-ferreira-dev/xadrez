package com.mateusferreira.xadrez.dominio;

/**
 * Uma jogada como um par (origem, destino). Usado pelo motor de IA para
 * representar e devolver lances candidatos.
 */
public record Jogada(Posicao origem, Posicao destino) {
}
