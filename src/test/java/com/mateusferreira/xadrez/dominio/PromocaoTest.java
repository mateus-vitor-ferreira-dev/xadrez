package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PromocaoTest {

    /** Tabuleiro mínimo com os dois reis (longe da ação), para mover() funcionar. */
    private Tabuleiro comReis() {
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), new Posicao(0, 7)); // h1
        t.colocarPeca(new Rei(Cor.PRETO), new Posicao(7, 7));  // h8
        return t;
    }

    @Test
    void peaoBrancoQueChegaNaUltimaFileiraViraRainha() {
        Tabuleiro t = comReis();
        t.colocarPeca(new Peao(Cor.BRANCO), new Posicao(6, 0)); // a7
        Partida partida = new Partida(t, Cor.BRANCO);

        partida.mover(Posicao.de("a7"), Posicao.de("a8"));

        assertInstanceOf(Rainha.class, t.pecaEm(Posicao.de("a8")));
        assertEquals(Cor.BRANCO, t.pecaEm(Posicao.de("a8")).getCor());
    }

    @Test
    void promocaoAoCapturarTambemFunciona() {
        Tabuleiro t = comReis();
        t.colocarPeca(new Peao(Cor.BRANCO), new Posicao(6, 1)); // b7
        t.colocarPeca(new Torre(Cor.PRETO), new Posicao(7, 0)); // a8 (capturável na diagonal)
        Partida partida = new Partida(t, Cor.BRANCO);

        partida.mover(Posicao.de("b7"), Posicao.de("a8")); // captura E promove

        assertInstanceOf(Rainha.class, t.pecaEm(Posicao.de("a8")));
    }

    @Test
    void peaoPretoPromoveNaLinhaZero() {
        Tabuleiro t = comReis();
        t.colocarPeca(new Peao(Cor.PRETO), new Posicao(1, 0)); // a2
        Partida partida = new Partida(t, Cor.PRETO);

        partida.mover(Posicao.de("a2"), Posicao.de("a1"));

        assertInstanceOf(Rainha.class, t.pecaEm(Posicao.de("a1")));
        assertEquals(Cor.PRETO, t.pecaEm(Posicao.de("a1")).getCor());
    }
}
