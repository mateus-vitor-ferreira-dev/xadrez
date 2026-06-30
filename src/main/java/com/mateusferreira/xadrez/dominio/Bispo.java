package com.mateusferreira.xadrez.dominio;

/**
 * O Bispo desliza nas 4 DIAGONAIS.
 *
 * Compare com a Torre: a lógica é EXATAMENTE a mesma (herdada de
 * PecaDeslizante). A única diferença é o conjunto de direções. Esse é o
 * pagamento da refatoração: criar uma peça nova virou quase trivial.
 */
public class Bispo extends PecaDeslizante {

    /** As 4 diagonais. */
    private static final int[][] DIRECOES = {
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
    };

    public Bispo(Cor cor) {
        super(cor);
    }

    @Override
    protected int[][] direcoes() {
        return DIRECOES;
    }

    @Override
    public char simbolo() {
        return 'B';
    }
}
