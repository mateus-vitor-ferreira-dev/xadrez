package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void ordenaCapturasPorMvvLva() {
        // Brancas na vez, SEM xeque, com duas capturas possíveis:
        //  - peão d4 x dama e5  (vítima cara, atacante barato -> melhor MVV-LVA)
        //  - dama a1 x peão a7  (vítima barata, atacante caro)
        // A ordenação deve colocar o peão-captura-dama à frente.
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("h1"));
        t.colocarPeca(new Rei(Cor.PRETO), de("h8"));
        t.colocarPeca(new Peao(Cor.BRANCO), de("d4"));
        t.colocarPeca(new Rainha(Cor.BRANCO), de("a1"));
        t.colocarPeca(new Rainha(Cor.PRETO), de("e5"));
        t.colocarPeca(new Peao(Cor.PRETO), de("a7"));
        Partida partida = new Partida(t, Cor.BRANCO);

        List<Jogada> ordenadas = motor.jogadasOrdenadas(partida, Cor.BRANCO);

        assertEquals(new Jogada(de("d4"), de("e5")), ordenadas.get(0));
    }

    @Test
    void prefereCentralizarOCavaloComMaterialIgual() {
        // Sem capturas e material igual: a decisão é puramente posicional. Um
        // cavalo na borda (a3) vale menos que no centro; a IA deve levá-lo a c4.
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Cavalo(Cor.BRANCO), de("a3"));
        t.colocarPeca(new Rei(Cor.PRETO), de("h8"));
        Partida partida = new Partida(t, Cor.BRANCO);

        Jogada jogada = motor.melhorJogada(partida, 1).orElseThrow();

        assertEquals(de("a3"), jogada.origem());
        assertEquals(de("c4"), jogada.destino());
    }
}
