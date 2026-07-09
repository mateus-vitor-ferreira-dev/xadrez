package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.mateusferreira.xadrez.dominio.Posicao.de;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @Test
    void naoCapturaPeaoDefendidoQuePerdeDama() {
        // Efeito horizonte: a dama branca pode capturar o peão d5 (ganho aparente
        // de +1), mas o peão está defendido por c6 e a dama seria recapturada. Com
        // quiescência a IA enxerga a recaptura e NÃO joga Dxd5.
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rainha(Cor.BRANCO), de("d1"));
        t.colocarPeca(new Rei(Cor.PRETO), de("h8"));
        t.colocarPeca(new Peao(Cor.PRETO), de("d5"));
        t.colocarPeca(new Peao(Cor.PRETO), de("c6"));
        Partida partida = new Partida(t, Cor.BRANCO);

        Jogada jogada = motor.melhorJogada(partida, 1).orElseThrow();

        assertNotEquals(de("d5"), jogada.destino()); // não morde a isca
    }

    @Test
    void mesmaPosicaoPorTransposicaoTemMesmoHash() {
        // A mesma posição alcançada por ordens de lances diferentes precisa ter
        // o mesmo hash de Zobrist (é o que torna a tabela de transposição útil).
        Partida a = Partida.nova();
        a.mover(de("b1"), de("c3"));
        a.mover(de("b8"), de("c6"));
        a.mover(de("g1"), de("f3"));
        a.mover(de("g8"), de("f6"));

        Partida b = Partida.nova();
        b.mover(de("g1"), de("f3"));
        b.mover(de("g8"), de("f6"));
        b.mover(de("b1"), de("c3"));
        b.mover(de("b8"), de("c6"));

        assertEquals(motor.zobrist(a), motor.zobrist(b));

        Partida diferente = Partida.nova();
        diferente.mover(de("e2"), de("e4"));
        assertNotEquals(motor.zobrist(a), motor.zobrist(diferente));
    }

    @Test
    void buscaPorTempoDevolveLanceLegal() {
        // A sobrecarga por orçamento de tempo (long) deve devolver um lance legal.
        Partida partida = Partida.nova();

        Jogada jogada = motor.melhorJogada(partida, 200L).orElseThrow();

        assertFalse(partida.movimentosLegais(jogada.origem()).isEmpty());
        assertTrue(partida.movimentosLegais(jogada.origem()).contains(jogada.destino()));
    }

    @Test
    void aceitaAvaliadorInjetado() {
        // Com um avaliador só de material, o motor ainda ganha a dama pendurada:
        // prova que a função de avaliação é intercambiável via construtor.
        MotorIA motorMaterial = new MotorIA(new AvaliadorMaterial());
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rainha(Cor.BRANCO), de("a4"));
        t.colocarPeca(new Rei(Cor.PRETO), de("e8"));
        t.colocarPeca(new Torre(Cor.PRETO), de("a8"));
        Partida partida = new Partida(t, Cor.PRETO);

        Jogada jogada = motorMaterial.melhorJogada(partida, 1).orElseThrow();

        assertEquals(de("a8"), jogada.origem());
        assertEquals(de("a4"), jogada.destino());
    }
}
