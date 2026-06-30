package com.mateusferreira.xadrez.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstrata intermediária para peças que se movem por SALTOS para um
 * conjunto FIXO de casas-alvo: Cavalo e Rei.
 *
 * É o "irmão" de PecaDeslizante — também usa Template Method, mas o algoritmo
 * é diferente: aqui NÃO há laço de "continuar na direção". Cada deslocamento
 * gera UMA casa-alvo, verificada de forma independente. Por isso o Cavalo
 * consegue PULAR sobre outras peças: não importa o que há no caminho, só
 * importa a casa onde ele aterrissa.
 */
public abstract class PecaDeSalto extends Peca {

    protected PecaDeSalto(Cor cor) {
        super(cor);
    }

    /**
     * O hook do Template Method: cada peça devolve seus deslocamentos possíveis
     * como pares (deltaLinha, deltaColuna) relativos à origem.
     */
    protected abstract int[][] deslocamentos();

    @Override
    public final List<Posicao> movimentosPossiveis(Tabuleiro tabuleiro, Posicao origem) {
        List<Posicao> destinos = new ArrayList<>();

        for (int[] passo : deslocamentos()) {
            Posicao destino = origem.deslocar(passo[0], passo[1]);

            // ATENÇÃO à ordem: verificamos dentroDoTabuleiro() PRIMEIRO.
            // Como '&&' faz curto-circuito, se a casa estiver fora nem chegamos
            // a perguntar se está vazia (o que, fora do tabuleiro, daria 'true'
            // por engano, já que pecaEm() devolve null para casas inexistentes).
            //
            // Uma casa-alvo é válida se: existe E (está vazia OU tem adversária).
            // "vazia ou adversária" = "não está ocupada por uma peça minha".
            boolean podeOcupar = destino.dentroDoTabuleiro()
                    && (tabuleiro.estaVazia(destino) || tabuleiro.temPecaAdversaria(destino, getCor()));

            if (podeOcupar) {
                destinos.add(destino);
            }
        }

        return destinos;
    }
}
