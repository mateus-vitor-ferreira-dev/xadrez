package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RainhaTest {

    @Test
    void rainhaNoCentroDeTabuleiroVazioTem27Movimentos() {
        // Arrange: rainha em (3,3) = "d4", tabuleiro vazio.
        Tabuleiro tabuleiro = new Tabuleiro();
        Rainha rainha = new Rainha(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(rainha, origem);

        // Act
        var movimentos = rainha.movimentosPossiveis(tabuleiro, origem);

        // Assert: rainha = torre (14) + bispo (13) = 27 movimentos.
        // Esta única conta confirma que ela combina os dois padrões corretamente.
        assertEquals(27, movimentos.size());
    }
}
