package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static com.mateusferreira.xadrez.dominio.Posicao.de;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoqueTest {

    /** Base: rei branco em e1 e rei preto em a8 (longe da ação). */
    private Tabuleiro base() {
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rei(Cor.PRETO), de("a8"));
        return t;
    }

    @Test
    void roquePequenoMoveReiETorre() {
        Tabuleiro t = base();
        t.colocarPeca(new Torre(Cor.BRANCO), de("h1"));
        Partida partida = new Partida(t, Cor.BRANCO);

        assertTrue(partida.movimentosLegais(de("e1")).contains(de("g1")));

        partida.mover(de("e1"), de("g1"));
        assertInstanceOf(Rei.class, t.pecaEm(de("g1")));   // rei foi para g1
        assertInstanceOf(Torre.class, t.pecaEm(de("f1"))); // torre pulou para f1
        assertTrue(t.estaVazia(de("e1")));
        assertTrue(t.estaVazia(de("h1")));
    }

    @Test
    void roqueGrandeMoveReiETorre() {
        Tabuleiro t = base();
        t.colocarPeca(new Torre(Cor.BRANCO), de("a1"));
        Partida partida = new Partida(t, Cor.BRANCO);

        partida.mover(de("e1"), de("c1"));
        assertInstanceOf(Rei.class, t.pecaEm(de("c1")));
        assertInstanceOf(Torre.class, t.pecaEm(de("d1")));
    }

    @Test
    void naoRocaComPecaNoCaminho() {
        Tabuleiro t = base();
        t.colocarPeca(new Torre(Cor.BRANCO), de("h1"));
        t.colocarPeca(new Bispo(Cor.BRANCO), de("f1")); // bloqueia f1
        Partida partida = new Partida(t, Cor.BRANCO);

        assertFalse(partida.movimentosLegais(de("e1")).contains(de("g1")));
    }

    @Test
    void naoRocaEstandoEmXeque() {
        Tabuleiro t = base();
        t.colocarPeca(new Torre(Cor.BRANCO), de("h1"));
        t.colocarPeca(new Torre(Cor.PRETO), de("e8")); // dá xeque pela coluna e
        Partida partida = new Partida(t, Cor.BRANCO);

        assertFalse(partida.movimentosLegais(de("e1")).contains(de("g1")));
    }

    @Test
    void naoRocaPassandoPorCasaAtacada() {
        Tabuleiro t = base();
        t.colocarPeca(new Torre(Cor.BRANCO), de("h1"));
        t.colocarPeca(new Torre(Cor.PRETO), de("f8")); // ataca f1, por onde o rei passaria
        Partida partida = new Partida(t, Cor.BRANCO);

        assertFalse(partida.movimentosLegais(de("e1")).contains(de("g1")));
    }

    @Test
    void perdeODireitoDeRoqueDepoisQueOReiSeMove() {
        Tabuleiro t = base();
        t.colocarPeca(new Torre(Cor.BRANCO), de("h1"));
        Partida partida = new Partida(t, Cor.BRANCO);

        partida.mover(de("e1"), de("f1")); // rei se mexe (perde o direito)
        partida.mover(de("a8"), de("a7")); // lance qualquer das pretas
        partida.mover(de("f1"), de("e1")); // rei volta para e1

        // Rei está de volta a e1 com caminho livre, mas o direito foi perdido.
        assertFalse(partida.movimentosLegais(de("e1")).contains(de("g1")));
    }
}
