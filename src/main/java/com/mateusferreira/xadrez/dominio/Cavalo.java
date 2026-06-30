package com.mateusferreira.xadrez.dominio;

/**
 * O Cavalo move-se em "L": duas casas numa direção e uma na perpendicular,
 * totalizando 8 saltos possíveis. É a única peça que pula sobre outras —
 * comportamento que ganhamos de graça em PecaDeSalto (sem laço de caminho).
 */
public class Cavalo extends PecaDeSalto {

    /** Os 8 saltos em "L": combinações de (±1,±2) e (±2,±1). */
    private static final int[][] DESLOCAMENTOS = {
            {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
            {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
    };

    public Cavalo(Cor cor) {
        super(cor);
    }

    @Override
    protected int[][] deslocamentos() {
        return DESLOCAMENTOS;
    }

    @Override
    public char simbolo() {
        return 'C';
    }
}
