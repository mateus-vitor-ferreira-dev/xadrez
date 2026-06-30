package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PosicaoInicialTest {

    @Test
    void cadaLadoComeçaCom16Pecas() {
        Tabuleiro tabuleiro = Tabuleiro.posicaoInicial();
        assertEquals(16, tabuleiro.posicoesDe(Cor.BRANCO).size());
        assertEquals(16, tabuleiro.posicoesDe(Cor.PRETO).size());
    }

    @Test
    void pecasNasCasasCorretas() {
        Tabuleiro tabuleiro = Tabuleiro.posicaoInicial();
        // Reis e damas nas casas certas.
        assertInstanceOf(Rei.class, tabuleiro.pecaEm(Posicao.de("e1")));
        assertInstanceOf(Rainha.class, tabuleiro.pecaEm(Posicao.de("d8")));
        // Cantos com torres.
        assertInstanceOf(Torre.class, tabuleiro.pecaEm(Posicao.de("a1")));
        assertInstanceOf(Torre.class, tabuleiro.pecaEm(Posicao.de("h8")));
        // Um peão branco na frente.
        assertInstanceOf(Peao.class, tabuleiro.pecaEm(Posicao.de("e2")));
        assertEquals(Cor.BRANCO, tabuleiro.pecaEm(Posicao.de("e2")).getCor());
    }

    @Test
    void notacaoDePosicaoEhInversaDoToString() {
        // Posicao.de("e4") deve recriar a mesma casa cujo toString() é "e4".
        Posicao e4 = Posicao.de("e4");
        assertEquals("e4", e4.toString());
        assertEquals(new Posicao(3, 4), e4);
    }
}
