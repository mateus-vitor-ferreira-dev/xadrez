package com.mateusferreira.xadrez.dominio;

/**
 * A Torre desliza nas 4 direções RETAS (horizontal e vertical).
 *
 * Repare como a classe encolheu: todo o algoritmo de movimento agora vive em
 * PecaDeslizante. A Torre só precisa dizer "estas são as minhas direções" e
 * qual é o seu símbolo. Isso é a herança trabalhando a nosso favor.
 */
public class Torre extends PecaDeslizante {

    /** Cima, baixo, direita, esquerda. */
    private static final int[][] DIRECOES = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    public Torre(Cor cor) {
        super(cor);
    }

    @Override
    protected int[][] direcoes() {
        return DIRECOES;
    }

    @Override
    public char simbolo() {
        return 'T';
    }
}
