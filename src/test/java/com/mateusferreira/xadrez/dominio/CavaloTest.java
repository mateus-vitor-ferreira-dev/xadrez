package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CavaloTest {

    @Test
    void cavaloNoCentroTem8Saltos() {
        Tabuleiro tabuleiro = new Tabuleiro();
        Cavalo cavalo = new Cavalo(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(cavalo, origem);

        assertEquals(8, cavalo.movimentosPossiveis(tabuleiro, origem).size());
    }

    @Test
    void cavaloNoCantoTemApenas2Saltos() {
        // No canto (0,0), 6 dos 8 saltos caem fora do tabuleiro.
        Tabuleiro tabuleiro = new Tabuleiro();
        Cavalo cavalo = new Cavalo(Cor.BRANCO);
        Posicao origem = new Posicao(0, 0);
        tabuleiro.colocarPeca(cavalo, origem);

        assertEquals(2, cavalo.movimentosPossiveis(tabuleiro, origem).size());
    }

    @Test
    void cavaloPulaSobrePecasVizinhas() {
        // Cercamos o cavalo com peças ALIADAS em todas as 8 casas vizinhas.
        // Como ele SALTA, os alvos (a 2 de distância) continuam acessíveis: 8.
        Tabuleiro tabuleiro = new Tabuleiro();
        Cavalo cavalo = new Cavalo(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(cavalo, origem);
        int[][] vizinhas = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] v : vizinhas) {
            tabuleiro.colocarPeca(new Torre(Cor.BRANCO), origem.deslocar(v[0], v[1]));
        }

        assertEquals(8, cavalo.movimentosPossiveis(tabuleiro, origem).size());
    }

    @Test
    void cavaloNaoPousaEmCasaDeAliado() {
        // Aliado exatamente num dos alvos do cavalo -> aquele alvo some (7).
        Tabuleiro tabuleiro = new Tabuleiro();
        Cavalo cavalo = new Cavalo(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(cavalo, origem);
        Posicao alvoBloqueado = new Posicao(5, 4); // = origem + (2,1)
        tabuleiro.colocarPeca(new Torre(Cor.BRANCO), alvoBloqueado);

        var movimentos = cavalo.movimentosPossiveis(tabuleiro, origem);
        assertFalse(movimentos.contains(alvoBloqueado));
        assertEquals(7, movimentos.size());
    }
}
