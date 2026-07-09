package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartidaTest {

    /** Monta um tabuleiro só com os dois reis (necessários para não faltar rei). */
    private Tabuleiro tabuleiroComReis() {
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(0, 4)); // e1
        tabuleiro.colocarPeca(new Rei(Cor.PRETO), new Posicao(7, 4));  // e8
        return tabuleiro;
    }

    @Test
    void moverAplicaAJogadaEPassaAVez() {
        Tabuleiro tabuleiro = tabuleiroComReis();
        tabuleiro.colocarPeca(new Peao(Cor.BRANCO), new Posicao(1, 0)); // a2
        Partida partida = new Partida(tabuleiro, Cor.BRANCO);

        partida.mover(new Posicao(1, 0), new Posicao(3, 0)); // a2 -> a4 (lance duplo)

        assertTrue(tabuleiro.estaVazia(new Posicao(1, 0)));               // origem esvaziou
        assertEquals('P', tabuleiro.pecaEm(new Posicao(3, 0)).simbolo()); // peão chegou no destino
        assertEquals(Cor.PRETO, partida.getJogadorAtual());              // virou a vez
    }

    @Test
    void naoPodeMoverPecaDoAdversarioNaSuaVez() {
        Tabuleiro tabuleiro = tabuleiroComReis();
        tabuleiro.colocarPeca(new Peao(Cor.PRETO), new Posicao(6, 0)); // a7 (peça preta)
        Partida partida = new Partida(tabuleiro, Cor.BRANCO);          // vez do branco

        assertThrows(MovimentoInvalidoException.class,
                () -> partida.mover(new Posicao(6, 0), new Posicao(5, 0)));
    }

    @Test
    void movimentoForaDasRegrasLancaExcecao() {
        Tabuleiro tabuleiro = tabuleiroComReis();
        tabuleiro.colocarPeca(new Peao(Cor.BRANCO), new Posicao(1, 0));
        Partida partida = new Partida(tabuleiro, Cor.BRANCO);

        // Peão não anda 3 casas: movimento ilegal.
        assertThrows(MovimentoInvalidoException.class,
                () -> partida.mover(new Posicao(1, 0), new Posicao(4, 0)));
    }

    @Test
    void pecaCravadaNaoPodeAbandonarADefesaDoRei() {
        // PIN: rei branco e1 (0,4); torre branca e2 (1,4); torre preta e8 (7,4).
        // A torre branca está "presa" na coluna 'e' defendendo o rei.
        Tabuleiro tabuleiro = new Tabuleiro();
        tabuleiro.colocarPeca(new Rei(Cor.BRANCO), new Posicao(0, 4));
        tabuleiro.colocarPeca(new Torre(Cor.BRANCO), new Posicao(1, 4));
        tabuleiro.colocarPeca(new Rei(Cor.PRETO), new Posicao(7, 0));   // rei preto longe (a8)
        tabuleiro.colocarPeca(new Torre(Cor.PRETO), new Posicao(7, 4)); // e8
        Partida partida = new Partida(tabuleiro, Cor.BRANCO);

        var legais = partida.movimentosLegais(new Posicao(1, 4));

        // Sair da coluna 'e' (ir para d2) expõe o rei -> ILEGAL.
        assertFalse(legais.contains(new Posicao(1, 3)));
        // Subir na PRÓPRIA coluna 'e' (e3) mantém o escudo -> LEGAL.
        assertTrue(legais.contains(new Posicao(2, 4)));
        // E capturar a torre atacante em e8 também é legal (some o xeque).
        assertTrue(legais.contains(new Posicao(7, 4)));

        // Tentar a jogada ilegal pela Partida deve estourar a exceção.
        assertThrows(MovimentoInvalidoException.class,
                () -> partida.mover(new Posicao(1, 4), new Posicao(1, 3)));
    }
}
