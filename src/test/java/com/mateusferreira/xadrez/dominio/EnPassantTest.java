package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static com.mateusferreira.xadrez.dominio.Posicao.de;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnPassantTest {

    private Tabuleiro comReis() {
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rei(Cor.PRETO), de("e8"));
        return t;
    }

    @Test
    void capturaEnPassantAvancaEPeaoCapturadoSome() {
        Tabuleiro t = comReis();
        t.colocarPeca(new Peao(Cor.BRANCO), de("e5"));
        t.colocarPeca(new Peao(Cor.PRETO), de("d7"));
        Partida partida = new Partida(t, Cor.PRETO);

        partida.mover(de("d7"), de("d5")); // lance duplo, parando ao lado do peão branco

        // As brancas podem capturar en passant na casa "pulada" (d6).
        assertTrue(partida.movimentosLegais(de("e5")).contains(de("d6")));

        partida.mover(de("e5"), de("d6"));
        assertInstanceOf(Peao.class, t.pecaEm(de("d6")));        // peão branco avançou para d6
        assertEquals(Cor.BRANCO, t.pecaEm(de("d6")).getCor());
        assertTrue(t.estaVazia(de("d5")));                       // o peão preto capturado sumiu
    }

    @Test
    void enPassantNaoExisteSemLanceDuploRecente() {
        // Peão preto já está em d5, mas NÃO via lance duplo agora -> sem alvo de en passant.
        Tabuleiro t = comReis();
        t.colocarPeca(new Peao(Cor.BRANCO), de("e5"));
        t.colocarPeca(new Peao(Cor.PRETO), de("d5"));
        Partida partida = new Partida(t, Cor.BRANCO);

        assertFalse(partida.movimentosLegais(de("e5")).contains(de("d6")));
    }
}
