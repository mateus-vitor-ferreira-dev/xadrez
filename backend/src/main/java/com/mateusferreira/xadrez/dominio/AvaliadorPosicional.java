package com.mateusferreira.xadrez.dominio;

/**
 * Avaliador MATERIAL + POSICIONAL: ao material (via {@link AvaliadorMaterial})
 * soma um bônus por casa (piece-square tables). É o avaliador PADRÃO do motor.
 *
 * <p>Sem noção posicional a IA só reage a capturas; com as tabelas ela
 * desenvolve peças ao centro, adianta peões e mantém o rei seguro na abertura
 * (e ativo no final).
 */
public class AvaliadorPosicional implements Avaliador {

    // Soma dos pesos de fase na posição inicial (meio-jogo pleno). Ver faseDoJogo.
    private static final int FASE_TOTAL = 24;

    @Override
    public int avaliar(Partida partida, Cor cor) {
        Tabuleiro t = partida.getTabuleiro();
        int fase = faseDoJogo(t);
        int total = 0;
        for (Posicao p : t.posicoesDe(cor)) {
            total += AvaliadorMaterial.valor(t.pecaEm(p)) + bonusPosicional(t.pecaEm(p), p, cor, fase);
        }
        Cor adversario = cor.oposta();
        for (Posicao p : t.posicoesDe(adversario)) {
            total -= AvaliadorMaterial.valor(t.pecaEm(p)) + bonusPosicional(t.pecaEm(p), p, adversario, fase);
        }
        return total;
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
