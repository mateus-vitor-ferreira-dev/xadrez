package com.mateusferreira.xadrez.dominio;

/**
 * A Rainha (Dama) é a peça mais poderosa: combina o movimento da Torre com o
 * do Bispo. Por isso suas direções são simplesmente RETAS + DIAGONAIS = 8.
 *
 * Veja como a herança paga dividendos: não escrevemos NENHUMA lógica de
 * movimento aqui — só listamos as 8 direções e reusamos o algoritmo de
 * PecaDeslizante.
 */
public class Rainha extends PecaDeslizante {

    /** 4 retas (como a Torre) + 4 diagonais (como o Bispo). */
    private static final int[][] DIRECOES = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},     // retas
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}    // diagonais
    };

    public Rainha(Cor cor) {
        super(cor);
    }

    @Override
    protected int[][] direcoes() {
        return DIRECOES;
    }

    @Override
    public char simbolo() {
        // 'D' de Dama (na notação brasileira a rainha é a Dama; o Rei será 'R').
        return 'D';
    }
}
