package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeaoTest {

    @Test
    void peaoBrancoNaFileiraInicialPodeAvancarUmaOuDuas() {
        // Peão branco em "e2" (1,4), tabuleiro vazio.
        Tabuleiro tabuleiro = new Tabuleiro();
        Peao peao = new Peao(Cor.BRANCO);
        Posicao origem = new Posicao(1, 4);
        tabuleiro.colocarPeca(peao, origem);

        var movimentos = peao.movimentosPossiveis(tabuleiro, origem);

        assertEquals(2, movimentos.size());
        assertTrue(movimentos.contains(new Posicao(2, 4))); // um passo
        assertTrue(movimentos.contains(new Posicao(3, 4))); // lance duplo
    }

    @Test
    void peaoForaDaFileiraInicialAvancaApenasUma() {
        // Mesmo peão, agora em (2,4): já saiu da fileira inicial.
        Tabuleiro tabuleiro = new Tabuleiro();
        Peao peao = new Peao(Cor.BRANCO);
        Posicao origem = new Posicao(2, 4);
        tabuleiro.colocarPeca(peao, origem);

        assertEquals(1, peao.movimentosPossiveis(tabuleiro, origem).size());
    }

    @Test
    void peaoBloqueadoDeFrenteNaoAvanca() {
        // Peça logo à frente (3,4) bloqueia o avanço — e peão NÃO captura de frente.
        Tabuleiro tabuleiro = new Tabuleiro();
        Peao peao = new Peao(Cor.BRANCO);
        Posicao origem = new Posicao(2, 4);
        tabuleiro.colocarPeca(peao, origem);
        tabuleiro.colocarPeca(new Torre(Cor.PRETO), new Posicao(3, 4)); // adversária na frente

        assertEquals(0, peao.movimentosPossiveis(tabuleiro, origem).size());
    }

    @Test
    void lanceDuploBloqueadoQuandoSegundaCasaOcupada() {
        // 1ª casa livre, 2ª ocupada: só o avanço simples é permitido.
        Tabuleiro tabuleiro = new Tabuleiro();
        Peao peao = new Peao(Cor.BRANCO);
        Posicao origem = new Posicao(1, 4);
        tabuleiro.colocarPeca(peao, origem);
        tabuleiro.colocarPeca(new Torre(Cor.BRANCO), new Posicao(3, 4)); // 2 casas à frente

        var movimentos = peao.movimentosPossiveis(tabuleiro, origem);
        assertEquals(1, movimentos.size());
        assertTrue(movimentos.contains(new Posicao(2, 4)));
        assertFalse(movimentos.contains(new Posicao(3, 4)));
    }

    @Test
    void peaoCapturaNasDiagonaisMasNaoDeFrente() {
        // Peão branco em (3,3); adversárias nas duas diagonais da frente.
        Tabuleiro tabuleiro = new Tabuleiro();
        Peao peao = new Peao(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(peao, origem);
        tabuleiro.colocarPeca(new Torre(Cor.PRETO), new Posicao(4, 2)); // diagonal esquerda
        tabuleiro.colocarPeca(new Torre(Cor.PRETO), new Posicao(4, 4)); // diagonal direita

        var movimentos = peao.movimentosPossiveis(tabuleiro, origem);

        // 1 avanço (4,3 vazio) + 2 capturas = 3.
        assertEquals(3, movimentos.size());
        assertTrue(movimentos.contains(new Posicao(4, 2)));
        assertTrue(movimentos.contains(new Posicao(4, 4)));
        assertTrue(movimentos.contains(new Posicao(4, 3)));
    }

    @Test
    void peaoNaoCapturaPecaAliadaNaDiagonal() {
        // Aliada na diagonal não é destino válido.
        Tabuleiro tabuleiro = new Tabuleiro();
        Peao peao = new Peao(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(peao, origem);
        tabuleiro.colocarPeca(new Torre(Cor.BRANCO), new Posicao(4, 4)); // aliada

        var movimentos = peao.movimentosPossiveis(tabuleiro, origem);
        assertFalse(movimentos.contains(new Posicao(4, 4)));
    }

    @Test
    void peaoPretoMoveNaDirecaoOposta() {
        // Peão preto em "e7" (6,4) anda para BAIXO (linhas menores).
        Tabuleiro tabuleiro = new Tabuleiro();
        Peao peao = new Peao(Cor.PRETO);
        Posicao origem = new Posicao(6, 4);
        tabuleiro.colocarPeca(peao, origem);

        var movimentos = peao.movimentosPossiveis(tabuleiro, origem);

        assertEquals(2, movimentos.size());
        assertTrue(movimentos.contains(new Posicao(5, 4)));
        assertTrue(movimentos.contains(new Posicao(4, 4)));
    }
}
