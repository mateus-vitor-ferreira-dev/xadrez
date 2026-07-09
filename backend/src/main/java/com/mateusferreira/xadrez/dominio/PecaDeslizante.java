package com.mateusferreira.xadrez.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstrata intermediária para as peças que se movem "deslizando" em
 * linha reta até esbarrar em alguém ou na borda: Torre, Bispo e Rainha.
 *
 * Esta classe implementa o PADRÃO DE PROJETO "Template Method":
 *   - o ALGORITMO geral (deslizar) é definido aqui, uma única vez;
 *   - a parte que VARIA (em quais direções deslizar) é deixada como um
 *     "buraco" abstrato -> direcoes() -> que cada subclasse preenche.
 *
 * Assim eliminamos a duplicação: o miolo do movimento existe em UM lugar só.
 */
public abstract class PecaDeslizante extends Peca {

    protected PecaDeslizante(Cor cor) {
        super(cor);
    }

    /**
     * O "buraco" do Template Method: cada peça deslizante devolve AQUI as suas
     * direções, como pares (deltaLinha, deltaColuna). É a ÚNICA coisa que
     * diferencia uma Torre de um Bispo de uma Rainha.
     */
    protected abstract int[][] direcoes();

    /**
     * O algoritmo deslizante, agora compartilhado por todas as subclasses.
     *
     * É 'final' de propósito: ninguém pode sobrescrevê-lo. Isso TRAVA o padrão
     * — uma subclasse só pode customizar via direcoes(), não reescrevendo o
     * movimento. Garante que a regra "não pula peças" valha para todas.
     */
    @Override
    public final List<Posicao> movimentosPossiveis(Tabuleiro tabuleiro, Posicao origem) {
        List<Posicao> destinos = new ArrayList<>();

        for (int[] direcao : direcoes()) {
            int dLinha = direcao[0];
            int dColuna = direcao[1];

            Posicao atual = origem.deslocar(dLinha, dColuna);

            // Desliza enquanto a casa existir e estiver vazia.
            while (atual.dentroDoTabuleiro() && tabuleiro.estaVazia(atual)) {
                destinos.add(atual);
                atual = atual.deslocar(dLinha, dColuna);
            }

            // Parou: se há peça adversária na casa de parada, pode capturar.
            if (tabuleiro.temPecaAdversaria(atual, getCor())) {
                destinos.add(atual);
            }
        }

        return destinos;
    }
}
