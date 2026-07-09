package com.mateusferreira.xadrez.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Inteligência artificial do xadrez, baseada em MINIMAX com poda ALPHA-BETA.
 *
 * Ideia: a IA simula o jogo 'profundidade' lances à frente, assumindo que os
 * dois lados jogam o melhor possível, e escolhe o lance que leva à melhor
 * posição para ela. A "nota" de uma posição é a vantagem de material.
 *
 * É Java puro (não conhece Spring nem banco): opera só sobre a Partida.
 */
public class MotorIA {

    // Pontuação de uma vitória/derrota por mate. Bem maior que qualquer material.
    private static final int MATE = 1_000_000;
    private static final int INFINITO = 9_999_999;

    /**
     * Escolhe o melhor lance para o jogador da vez, olhando 'profundidade'
     * lances à frente. Devolve vazio se não houver lances (fim de jogo).
     */
    public Optional<Jogada> melhorJogada(Partida partida, int profundidade) {
        Cor cor = partida.getJogadorAtual();
        List<Jogada> jogadas = jogadasLegais(partida, cor);
        if (jogadas.isEmpty()) {
            return Optional.empty();
        }

        Jogada melhor = null;
        int melhorValor = -INFINITO;
        for (Jogada jogada : jogadas) {
            Partida copia = partida.copia();
            copia.mover(jogada.origem(), jogada.destino());
            // valor do ponto de vista do oponente, negado -> do nosso ponto de vista
            int valor = -negamax(copia, profundidade - 1, -INFINITO, INFINITO);
            if (valor > melhorValor) {
                melhorValor = valor;
                melhor = jogada;
            }
        }
        return Optional.of(melhor);
    }

    /**
     * Negamax: variação enxuta do minimax onde a nota é SEMPRE do ponto de vista
     * de quem está na vez, e a recursão nega o resultado do oponente.
     * 'alfa' e 'beta' são os limites da poda: se um ramo já é pior do que uma
     * alternativa garantida, paramos de explorá-lo.
     */
    private int negamax(Partida partida, int profundidade, int alfa, int beta) {
        Cor cor = partida.getJogadorAtual();
        List<Jogada> jogadas = jogadasLegais(partida, cor);

        // Sem lances legais: xeque-mate (péssimo) ou afogamento (empate = 0).
        if (jogadas.isEmpty()) {
            return partida.estaEmXeque(cor) ? -MATE : 0;
        }
        // Chegou ao limite da busca: avalia a posição pela vantagem de material.
        if (profundidade == 0) {
            return avaliar(partida, cor);
        }

        int melhor = -INFINITO;
        for (Jogada jogada : jogadas) {
            Partida copia = partida.copia();
            copia.mover(jogada.origem(), jogada.destino());
            int valor = -negamax(copia, profundidade - 1, -beta, -alfa);
            melhor = Math.max(melhor, valor);
            alfa = Math.max(alfa, melhor);
            if (alfa >= beta) {
                break; // poda: o oponente nunca deixaria chegar aqui
            }
        }
        return melhor;
    }

    /** Vantagem de material do ponto de vista de 'cor' (minhas peças - dele). */
    private int avaliar(Partida partida, Cor cor) {
        Tabuleiro t = partida.getTabuleiro();
        int total = 0;
        for (Posicao p : t.posicoesDe(cor)) {
            total += valor(t.pecaEm(p));
        }
        for (Posicao p : t.posicoesDe(cor.oposta())) {
            total -= valor(t.pecaEm(p));
        }
        return total;
    }

    /** Valor clássico de cada peça (o rei não conta: nunca é capturado). */
    private int valor(Peca peca) {
        if (peca instanceof Peao) return 1;
        if (peca instanceof Cavalo || peca instanceof Bispo) return 3;
        if (peca instanceof Torre) return 5;
        if (peca instanceof Rainha) return 9;
        return 0;
    }

    /** Todos os lances legais do jogador 'cor' (origem -> cada destino legal). */
    private List<Jogada> jogadasLegais(Partida partida, Cor cor) {
        List<Jogada> jogadas = new ArrayList<>();
        for (Posicao origem : partida.getTabuleiro().posicoesDe(cor)) {
            for (Posicao destino : partida.movimentosLegais(origem)) {
                jogadas.add(new Jogada(origem, destino));
            }
        }
        return jogadas;
    }
}
