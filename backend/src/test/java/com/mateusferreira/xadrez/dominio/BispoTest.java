package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BispoTest {

    @Test
    void bispoNoCentroDeTabuleiroVazioTem13Movimentos() {
        // Arrange: bispo em (3,3) = "d4", tabuleiro vazio.
        Tabuleiro tabuleiro = new Tabuleiro();
        Bispo bispo = new Bispo(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(bispo, origem);

        // Act
        var movimentos = bispo.movimentosPossiveis(tabuleiro, origem);

        // Assert: de d4 as 4 diagonais somam 13 casas (4 + 3 + 3 + 3).
        assertEquals(13, movimentos.size());
    }

    @Test
    void bispoCapturaNaDiagonalMasNaoPulaAlem() {
        // Arrange: bispo branco em (0,0) e peça preta na diagonal, em (2,2).
        Tabuleiro tabuleiro = new Tabuleiro();
        Bispo bispo = new Bispo(Cor.BRANCO);
        Posicao origem = new Posicao(0, 0);
        tabuleiro.colocarPeca(bispo, origem);
        tabuleiro.colocarPeca(new Torre(Cor.PRETO), new Posicao(2, 2)); // adversária

        // Act
        var movimentos = bispo.movimentosPossiveis(tabuleiro, origem);

        // Assert: passa por (1,1), captura em (2,2), mas não vai além (3,3).
        assertTrue(movimentos.contains(new Posicao(1, 1)));
        assertTrue(movimentos.contains(new Posicao(2, 2)));
        assertFalse(movimentos.contains(new Posicao(3, 3)));
    }
}
