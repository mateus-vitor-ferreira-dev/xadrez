package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalDeJogoTest {

    @Test
    void duasTorresDaoXequeMateNoReiNoCanto() {
        // Rei preto em a8 (7,0). Torres brancas controlam a coluna 'a' e a 'b'.
        //   - Torre a1 dá xeque pela coluna 'a'.
        //   - Torre b1 cobre b8 e b7; a torre a1 cobre a7.
        // O rei não tem para onde fugir nem como capturar -> mate.
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.PRETO), new Posicao(7, 0));   // a8
        tabuleiro.colocarPeca(new Torre(Cor.BRANCO), new Posicao(0, 0)); // a1 (dá o xeque)
        tabuleiro.colocarPeca(new Torre(Cor.BRANCO), new Posicao(0, 1)); // b1 (corta a fuga)
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(0, 7));   // h1 (longe)
        Partida partida = new Partida(tabuleiro, Cor.PRETO);

        assertTrue(partida.estaEmXeque(Cor.PRETO));
        assertTrue(partida.estaEmXequeMate(Cor.PRETO));
        assertFalse(partida.estaEmAfogamento(Cor.PRETO)); // mate não é afogamento
    }

    @Test
    void reiSemMovimentoMasSemXequeEhAfogamento() {
        // Rei preto em h8 (7,7), SEM estar em xeque, mas sem casa legal:
        //   - Dama branca em g6 controla g7, g8 e h7 (sem atacar h8).
        // Resultado: afogamento (empate), não mate.
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.PRETO), new Posicao(7, 7));    // h8
        tabuleiro.colocarPeca(new Rainha(Cor.BRANCO), new Posicao(5, 6)); // g6
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(5, 5));   // f6
        Partida partida = new Partida(tabuleiro, Cor.PRETO);

        assertFalse(partida.estaEmXeque(Cor.PRETO));
        assertTrue(partida.estaEmAfogamento(Cor.PRETO));
        assertFalse(partida.estaEmXequeMate(Cor.PRETO)); // sem xeque, não é mate
    }
}
