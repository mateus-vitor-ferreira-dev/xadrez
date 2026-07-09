package com.mateusferreira.xadrez.dominio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Inteligência artificial do xadrez, baseada em MINIMAX com poda ALPHA-BETA.
 *
 * Ideia: a IA simula o jogo 'profundidade' lances à frente, assumindo que os
 * dois lados jogam o melhor possível, e escolhe o lance que leva à melhor
 * posição para ela. A "nota" de uma posição é a vantagem de material.
 *
 * A poda alpha-beta corta muito mais quando os lances promissores são vistos
 * PRIMEIRO. Por isso ordenamos os lances antes de percorrê-los: capturas na
 * frente das jogadas quietas e, entre as capturas, por MVV-LVA (Most Valuable
 * Victim, Least Valuable Attacker — capturar dama com peão vem antes de capturar
 * peão com dama). O resultado escolhido é o mesmo; só chegamos nele mais rápido.
 *
 * É Java puro (não conhece Spring nem banco): opera só sobre a Partida.
 */
public class MotorIA {

    // Pontuação de uma vitória/derrota por mate. Bem maior que qualquer material.
    private static final int MATE = 1_000_000;
    private static final int INFINITO = 9_999_999;
    // Toda captura pontua acima de qualquer lance quieto na ordenação.
    private static final int BASE_CAPTURA = 10_000;
    // Soma dos pesos de fase na posição inicial (meio-jogo pleno). Ver faseDoJogo.
    private static final int FASE_TOTAL = 24;

    /**
     * Escolhe o melhor lance para o jogador da vez, olhando 'profundidade'
     * lances à frente. Devolve vazio se não houver lances (fim de jogo).
     */
    public Optional<Jogada> melhorJogada(Partida partida, int profundidade) {
        Cor cor = partida.getJogadorAtual();
        List<Jogada> jogadas = jogadasOrdenadas(partida, cor);
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
        // Só vale ordenar quando vamos de fato percorrer os lances (não no leaf acima).
        ordenar(jogadas, partida);

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

    /**
     * Nota da posição do ponto de vista de 'cor': material (em centipeões) MAIS
     * bônus posicional (piece-square tables). Sem noção posicional a IA só
     * reagia a capturas; com as tabelas ela desenvolve peças ao centro, adianta
     * peões e mantém o rei seguro na abertura (e ativo no final).
     */
    private int avaliar(Partida partida, Cor cor) {
        Tabuleiro t = partida.getTabuleiro();
        int fase = faseDoJogo(t);
        int total = 0;
        for (Posicao p : t.posicoesDe(cor)) {
            total += valor(t.pecaEm(p)) + bonusPosicional(t.pecaEm(p), p, cor, fase);
        }
        Cor adversario = cor.oposta();
        for (Posicao p : t.posicoesDe(adversario)) {
            total -= valor(t.pecaEm(p)) + bonusPosicional(t.pecaEm(p), p, adversario, fase);
        }
        return total;
    }

    /** Valor clássico de cada peça em centipeões (o rei não conta: nunca é capturado). */
    private int valor(Peca peca) {
        if (peca instanceof Peao) return 100;
        if (peca instanceof Cavalo) return 320;
        if (peca instanceof Bispo) return 330;
        if (peca instanceof Torre) return 500;
        if (peca instanceof Rainha) return 900;
        return 0;
    }

    /**
     * Bônus posicional da peça na casa 'pos', do ponto de vista da cor DELA.
     * As tabelas são escritas na ótica das brancas (linha 0 = 1ª fileira); para
     * as pretas espelhamos a fileira (7 - linha). O rei usa avaliação "tapered":
     * interpola entre a tabela de meio-jogo e a de final conforme a 'fase'.
     */
    private int bonusPosicional(Peca peca, Posicao pos, Cor cor, int fase) {
        int linha = (cor == Cor.BRANCO) ? pos.linha() : 7 - pos.linha();
        int col = pos.coluna();
        if (peca instanceof Peao) return PST_PEAO[linha][col];
        if (peca instanceof Cavalo) return PST_CAVALO[linha][col];
        if (peca instanceof Bispo) return PST_BISPO[linha][col];
        if (peca instanceof Torre) return PST_TORRE[linha][col];
        if (peca instanceof Rainha) return PST_RAINHA[linha][col];
        if (peca instanceof Rei) {
            return (PST_REI_MEIO[linha][col] * fase
                    + PST_REI_FIM[linha][col] * (FASE_TOTAL - fase)) / FASE_TOTAL;
        }
        return 0;
    }

    /**
     * "Fase" do jogo pelo material não-peão em campo (cavalo/bispo=1, torre=2,
     * dama=4). {@link #FASE_TOTAL} = posição inicial (meio-jogo pleno); perto de
     * 0 = final. Usada para interpolar as tabelas do rei.
     */
    private int faseDoJogo(Tabuleiro t) {
        int fase = 0;
        for (Posicao p : t.posicoesDe(Cor.BRANCO)) fase += pesoFase(t.pecaEm(p));
        for (Posicao p : t.posicoesDe(Cor.PRETO)) fase += pesoFase(t.pecaEm(p));
        return Math.min(fase, FASE_TOTAL);
    }

    private int pesoFase(Peca peca) {
        if (peca instanceof Cavalo || peca instanceof Bispo) return 1;
        if (peca instanceof Torre) return 2;
        if (peca instanceof Rainha) return 4;
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

    /** Lances legais de 'cor' já ordenados para a poda (capturas primeiro, MVV-LVA). */
    List<Jogada> jogadasOrdenadas(Partida partida, Cor cor) {
        List<Jogada> jogadas = jogadasLegais(partida, cor);
        ordenar(jogadas, partida);
        return jogadas;
    }

    /** Ordena a lista in-place por {@link #scoreOrdenacao} decrescente. */
    private void ordenar(List<Jogada> jogadas, Partida partida) {
        jogadas.sort(Comparator.comparingInt((Jogada j) -> scoreOrdenacao(partida, j)).reversed());
    }

    /**
     * Nota de ordenação (não de avaliação): capturas ficam acima das jogadas
     * quietas e, entre elas, valem mais as que capturam peça cara com peça barata
     * (MVV-LVA). Jogadas quietas valem 0.
     */
    private int scoreOrdenacao(Partida partida, Jogada jogada) {
        if (!ehCaptura(partida, jogada)) {
            return 0;
        }
        int atacante = valor(partida.getTabuleiro().pecaEm(jogada.origem()));
        return BASE_CAPTURA + 10 * valorVitima(partida, jogada) - atacante;
    }

    /**
     * O lance é uma captura? Cobre a captura normal (peça no destino) e o
     * en passant (o peão vai para a casa-alvo VAZIA, capturando o peão ao lado).
     */
    private boolean ehCaptura(Partida partida, Jogada jogada) {
        Tabuleiro t = partida.getTabuleiro();
        if (t.pecaEm(jogada.destino()) != null) {
            return true;
        }
        return t.pecaEm(jogada.origem()) instanceof Peao
                && jogada.destino().equals(partida.getAlvoEnPassant());
    }

    /** Valor da peça capturada por 'jogada' (0 se não for captura). */
    private int valorVitima(Partida partida, Jogada jogada) {
        Tabuleiro t = partida.getTabuleiro();
        Peca alvo = t.pecaEm(jogada.destino());
        if (alvo != null) {
            return valor(alvo);
        }
        // En passant: a vítima é o peão que ficou na fileira da origem, coluna do destino.
        Peca peaoCapturado = t.pecaEm(new Posicao(jogada.origem().linha(), jogada.destino().coluna()));
        return peaoCapturado == null ? 0 : valor(peaoCapturado);
    }

    // ---------------------------------------------------------------------
    // Piece-square tables (valores em centipeões), na ótica das BRANCAS:
    // linha 0 = 1ª fileira (casa das brancas), coluna 0 = coluna 'a'. Para as
    // pretas, bonusPosicional espelha a fileira (7 - linha). Baseadas na
    // "Simplified Evaluation Function" (Tomasz Michniewski).
    // ---------------------------------------------------------------------

    private static final int[][] PST_PEAO = {
            {  0,   0,   0,   0,   0,   0,   0,   0},
            {  5,  10,  10, -20, -20,  10,  10,   5},
            {  5,  -5, -10,   0,   0, -10,  -5,   5},
            {  0,   0,   0,  20,  20,   0,   0,   0},
            {  5,   5,  10,  25,  25,  10,   5,   5},
            { 10,  10,  20,  30,  30,  20,  10,  10},
            { 50,  50,  50,  50,  50,  50,  50,  50},
            {  0,   0,   0,   0,   0,   0,   0,   0},
    };

    private static final int[][] PST_CAVALO = {
            {-50, -40, -30, -30, -30, -30, -40, -50},
            {-40, -20,   0,   5,   5,   0, -20, -40},
            {-30,   5,  10,  15,  15,  10,   5, -30},
            {-30,   0,  15,  20,  20,  15,   0, -30},
            {-30,   5,  15,  20,  20,  15,   5, -30},
            {-30,   0,  10,  15,  15,  10,   0, -30},
            {-40, -20,   0,   0,   0,   0, -20, -40},
            {-50, -40, -30, -30, -30, -30, -40, -50},
    };

    private static final int[][] PST_BISPO = {
            {-20, -10, -10, -10, -10, -10, -10, -20},
            {-10,   5,   0,   0,   0,   0,   5, -10},
            {-10,  10,  10,  10,  10,  10,  10, -10},
            {-10,   0,  10,  10,  10,  10,   0, -10},
            {-10,   5,   5,  10,  10,   5,   5, -10},
            {-10,   0,   5,  10,  10,   5,   0, -10},
            {-10,   0,   0,   0,   0,   0,   0, -10},
            {-20, -10, -10, -10, -10, -10, -10, -20},
    };

    private static final int[][] PST_TORRE = {
            {  0,   0,   0,   5,   5,   0,   0,   0},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            { -5,   0,   0,   0,   0,   0,   0,  -5},
            {  5,  10,  10,  10,  10,  10,  10,   5},
            {  0,   0,   0,   0,   0,   0,   0,   0},
    };

    private static final int[][] PST_RAINHA = {
            {-20, -10, -10,  -5,  -5, -10, -10, -20},
            {-10,   0,   5,   0,   0,   0,   0, -10},
            {-10,   5,   5,   5,   5,   5,   0, -10},
            {  0,   0,   5,   5,   5,   5,   0,  -5},
            { -5,   0,   5,   5,   5,   5,   0,  -5},
            {-10,   0,   5,   5,   5,   5,   0, -10},
            {-10,   0,   0,   0,   0,   0,   0, -10},
            {-20, -10, -10,  -5,  -5, -10, -10, -20},
    };

    // Rei no MEIO-JOGO: quer ficar recolhido atrás dos peões (fileiras 1-2).
    private static final int[][] PST_REI_MEIO = {
            { 20,  30,  10,   0,   0,  10,  30,  20},
            { 20,  20,   0,   0,   0,   0,  20,  20},
            {-10, -20, -20, -20, -20, -20, -20, -10},
            {-20, -30, -30, -40, -40, -30, -30, -20},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
    };

    // Rei no FINAL: quer ir para o centro ajudar (rei ativo).
    private static final int[][] PST_REI_FIM = {
            {-50, -30, -30, -30, -30, -30, -30, -50},
            {-30, -30,   0,   0,   0,   0, -30, -30},
            {-30, -10,  20,  30,  30,  20, -10, -30},
            {-30, -10,  30,  40,  40,  30, -10, -30},
            {-30, -10,  30,  40,  40,  30, -10, -30},
            {-30, -10,  20,  30,  30,  20, -10, -30},
            {-30, -20, -10,   0,   0, -10, -20, -30},
            {-50, -40, -30, -20, -20, -30, -40, -50},
    };
}
