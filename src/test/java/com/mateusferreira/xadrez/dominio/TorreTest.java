package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes das regras de movimento da Torre.
 *
 * Conceitos de JUnit 5 que aparecem aqui:
 *   - @Test            -> marca um método como um teste executável.
 *   - assertEquals(a,b)-> falha se a != b ("o esperado é 'a'").
 *   - assertTrue/False -> falha se a condição não for o esperado.
 *
 * Padrão "AAA" (Arrange, Act, Assert) que organiza todo bom teste:
 *   Arrange = preparar o cenário | Act = executar | Assert = conferir.
 *
 * Repare: NÃO precisamos subir o Spring nem banco de dados. O domínio é Java
 * puro, então o teste é instantâneo. Esse é um dos frutos de ter isolado o
 * domínio da infraestrutura.
 */
class TorreTest {

    @Test
    void torreNoCentroDeTabuleiroVazioTem14Movimentos() {
        // Arrange: uma torre branca sozinha na casa (3,3) = "d4".
        Tabuleiro tabuleiro = new Tabuleiro();
        Torre torre = new Torre(Cor.BRANCO);
        Posicao origem = new Posicao(3, 3);
        tabuleiro.colocarPeca(torre, origem);

        // Act: pergunto à torre para onde ela pode ir.
        var movimentos = torre.movimentosPossiveis(tabuleiro, origem);

        // Assert: numa coluna inteira (7 casas) + uma linha inteira (7 casas) = 14.
        assertEquals(14, movimentos.size());
    }

    @Test
    void torreParaAntesDeUmaPecaAliada() {
        // Arrange: torre branca em "a1" (0,0) e OUTRA peça branca em (0,3).
        Tabuleiro tabuleiro = new Tabuleiro();
        Torre torre = new Torre(Cor.BRANCO);
        Posicao origem = new Posicao(0, 0);
        tabuleiro.colocarPeca(torre, origem);
        tabuleiro.colocarPeca(new Torre(Cor.BRANCO), new Posicao(0, 3)); // aliada

        // Act
        var movimentos = torre.movimentosPossiveis(tabuleiro, origem);

        // Assert:
        //  - andando p/ direita ela passa por (0,1) e (0,2)...
        assertTrue(movimentos.contains(new Posicao(0, 1)));
        assertTrue(movimentos.contains(new Posicao(0, 2)));
        //  - ...mas PARA antes da aliada: (0,3) NÃO é destino válido.
        assertFalse(movimentos.contains(new Posicao(0, 3)));
        //  - total: 2 (direita) + 7 (coluna p/ cima) = 9.
        assertEquals(9, movimentos.size());
    }

    @Test
    void torrePodeCapturarAdversariaMasNaoPularAlem() {
        // Arrange: torre branca em "a1" (0,0) e uma peça PRETA em (0,3).
        Tabuleiro tabuleiro = new Tabuleiro();
        Torre torre = new Torre(Cor.BRANCO);
        Posicao origem = new Posicao(0, 0);
        tabuleiro.colocarPeca(torre, origem);
        tabuleiro.colocarPeca(new Torre(Cor.PRETO), new Posicao(0, 3)); // adversária

        // Act
        var movimentos = torre.movimentosPossiveis(tabuleiro, origem);

        // Assert:
        //  - a casa da adversária (0,3) É um destino válido (captura)...
        assertTrue(movimentos.contains(new Posicao(0, 3)));
        //  - ...mas a torre NÃO pula por cima: (0,4) não pode.
        assertFalse(movimentos.contains(new Posicao(0, 4)));
        //  - total: 3 (direita, incluindo a captura) + 7 (coluna) = 10.
        assertEquals(10, movimentos.size());
    }
}
