package com.mateusferreira.xadrez.dominio;

/**
 * O Rei move-se 1 casa em qualquer das 8 direções.
 *
 * Repare: as direções do Rei são as MESMAS 8 da Rainha — a diferença é que o
 * Rei dá só UM passo (por isso é uma PecaDeSalto: um alvo fixo por direção),
 * enquanto a Rainha desliza. Mesma "forma", algoritmo diferente.
 *
 * Regras especiais do Rei (NÃO entrar em casa atacada, e o roque) são regras de
 * nível mais alto — assim como a promoção do peão, ficam fora da peça e serão
 * tratadas na camada de regras do jogo. Aqui mora só o movimento "geográfico".
 */
public class Rei extends PecaDeSalto {

    /** As 8 casas vizinhas (1 passo em cada direção). */
    private static final int[][] DESLOCAMENTOS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Rei(Cor cor) {
        super(cor);
    }

    @Override
    protected int[][] deslocamentos() {
        return DESLOCAMENTOS;
    }

    @Override
    public char simbolo() {
        return 'R';
    }
}
