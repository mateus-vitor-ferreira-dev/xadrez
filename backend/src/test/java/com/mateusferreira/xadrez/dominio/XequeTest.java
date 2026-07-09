package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XequeTest {

    @Test
    void posicaoDoReiEncontraORei() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Posicao casa = new Posicao(0, 4);
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), casa);

        assertEquals(casa, tabuleiro.posicaoDoRei(Cor.BRANCO));
    }

    @Test
    void reiEmXequePorTorreNaMesmaColuna() {
        // Rei branco em e1 (0,4); torre preta em e8 (7,4), coluna livre entre eles.
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(0, 4));
        tabuleiro.colocarPeca(new Torre(Cor.PRETO), new Posicao(7, 4));

        assertTrue(tabuleiro.estaEmXeque(Cor.BRANCO));
    }

    @Test
    void naoHaXequeQuandoUmaPecaBloqueiaOCaminho() {
        // Mesma torre, mas um peão (de qualquer cor) em e4 (3,4) corta a linha.
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(0, 4));
        tabuleiro.colocarPeca(new Torre(Cor.PRETO), new Posicao(7, 4));
        tabuleiro.colocarPeca(new Peao(Cor.BRANCO), new Posicao(3, 4)); // bloqueio

        assertFalse(tabuleiro.estaEmXeque(Cor.BRANCO));
    }

    @Test
    void cavaloDaXequeSaltandoSobrePecas() {
        // Cavalo preto em (2,3) ataca o rei branco em (0,4): salto (-2,+1).
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(0, 4));
        tabuleiro.colocarPeca(new Cavalo(Cor.PRETO), new Posicao(2, 3));

        assertTrue(tabuleiro.estaEmXeque(Cor.BRANCO));
    }

    @Test
    void peaoAtacaReiNaDiagonal() {
        // Rei branco em (3,4); peão preto em (4,3) ataca a diagonal (3,4)?
        // Peão preto anda para baixo (-1) e captura em (3,2) e (3,4). Logo, xeque.
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(3, 4));
        tabuleiro.colocarPeca(new Peao(Cor.PRETO), new Posicao(4, 3));

        assertTrue(tabuleiro.estaEmXeque(Cor.BRANCO));
    }

    @Test
    void peaoNaoAtacaReiDeFrente() {
        // Peão preto exatamente à frente do rei (4,4 sobre 3,4) NÃO dá xeque:
        // peão não captura andando reto.
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(3, 4));
        tabuleiro.colocarPeca(new Peao(Cor.PRETO), new Posicao(4, 4));

        assertFalse(tabuleiro.estaEmXeque(Cor.BRANCO));
    }
}
