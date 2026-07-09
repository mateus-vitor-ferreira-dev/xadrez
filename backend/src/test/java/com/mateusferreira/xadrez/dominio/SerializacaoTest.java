package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Testes da serialização do tabuleiro (domínio puro): garantir que dá para
 * gravar como texto e reconstruir sem perder informação (round-trip).
 */
class SerializacaoTest {

    @Test
    void serializarProduz64Caracteres() {
        assertEquals(64, Tabuleiro.posicaoInicial().serializar().length());
    }

    @Test
    void serializarEReconstruirPreservaOTabuleiro() {
        String original = Tabuleiro.posicaoInicial().serializar();
        Tabuleiro reconstruido = Tabuleiro.deTexto(original);
        // Se a reconstrução serializa para o mesmo texto, nada se perdeu.
        assertEquals(original, reconstruido.serializar());
    }

    @Test
    void pecasSaoReconstruidasComTipoECorCorretos() {
        Tabuleiro t = Tabuleiro.deTexto(Tabuleiro.posicaoInicial().serializar());

        assertInstanceOf(Rei.class, t.pecaEm(Posicao.de("e1")));
        assertEquals(Cor.BRANCO, t.pecaEm(Posicao.de("e1")).getCor());

        assertInstanceOf(Rei.class, t.pecaEm(Posicao.de("e8")));
        assertEquals(Cor.PRETO, t.pecaEm(Posicao.de("e8")).getCor());

        assertInstanceOf(Cavalo.class, t.pecaEm(Posicao.de("b1")));
    }
}
