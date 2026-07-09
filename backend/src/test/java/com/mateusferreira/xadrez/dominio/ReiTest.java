package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReiTest {

    @Test
    void reiNoCentroTem8Movimentos() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Rei rei = new Rei(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(rei, origem);

        assertEquals(8, rei.movimentosPossiveis(tabuleiro, origem).size());
    }

    @Test
    void reiNoCantoTem3Movimentos() {
        // No canto (0,0) só existem 3 casas vizinhas dentro do tabuleiro.
        Tabuleiro tabuleiro = new Tabuleiro();
        Rei rei = new Rei(Cor.BRANCO);
        Posicao origem = new Posicao(0, 0);
        tabuleiro.colocarPeca(rei, origem);

        assertEquals(3, rei.movimentosPossiveis(tabuleiro, origem).size());
    }
}
