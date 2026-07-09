package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static com.mateusferreira.xadrez.dominio.Posicao.de;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotorIATest {

    private final MotorIA motor = new MotorIA();

    @Test
    void capturaUmaPecaPenduradaComProfundidade1() {
        // Pretas (IA) na vez. A dama branca em a4 está atacada pela torre a8 e
        // indefesa -> a IA deve capturá-la (ganho de material) já na profundidade 1.
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rainha(Cor.BRANCO), de("a4"));
        t.colocarPeca(new Rei(Cor.PRETO), de("e8"));
        t.colocarPeca(new Torre(Cor.PRETO), de("a8"));
        Partida partida = new Partida(t, Cor.PRETO);

        Jogada jogada = motor.melhorJogada(partida, 1).orElseThrow();

        assertEquals(de("a8"), jogada.origem());
        assertEquals(de("a4"), jogada.destino()); // captura a dama
    }

    @Test
    void encontraMateEmUmComProfundidade2() {
        // Mate de corredor: rei preto preso em g8 pelos próprios peões; a torre
        // branca dá mate em e8. A IA (brancas) deve achar com profundidade 2.
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.PRETO), de("g8"));
        t.colocarPeca(new Peao(Cor.PRETO), de("f7"));
        t.colocarPeca(new Peao(Cor.PRETO), de("g7"));
        t.colocarPeca(new Peao(Cor.PRETO), de("h7"));
        t.colocarPeca(new Torre(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rei(Cor.BRANCO), de("a1"));
        Partida partida = new Partida(t, Cor.BRANCO);

        Jogada jogada = motor.melhorJogada(partida, 2).orElseThrow();
        partida.mover(jogada.origem(), jogada.destino());

        assertTrue(partida.estaEmXequeMate(Cor.PRETO));
    }
}
