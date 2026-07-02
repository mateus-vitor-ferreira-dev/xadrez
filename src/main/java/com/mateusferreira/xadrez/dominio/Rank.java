package com.mateusferreira.xadrez.dominio;

/**
 * Faixa de habilidade ("rank") derivada do Elo — no estilo Chess.com/Lichess.
 *
 * <p><b>Decisão de design:</b> o rank NÃO é um dado guardado no banco. Ele é
 * calculado a partir do Elo, que é a única fonte de verdade. Assim, quando o Elo
 * muda no fim de uma partida, o rank "muda junto" de graça — não há dois campos
 * para manter em sincronia (e, portanto, um bug a menos).
 *
 * <p>Cada faixa é definida só pelo seu <b>piso</b> ({@code eloMinimo}). O teto é o
 * piso da faixa seguinte menos 1; a última faixa não tem teto. Todo jogador nasce
 * com Elo 1200, ou seja, começa em {@link #INTERMEDIARIO}.
 */
public enum Rank {

    INICIANTE("Iniciante", 0),
    INTERMEDIARIO("Intermediário", 1000),
    AVANCADO("Avançado", 1400),
    ESPECIALISTA("Especialista", 1800),
    MESTRE("Mestre", 2100),
    GRAO_MESTRE("Grande Mestre", 2400);

    /** Nome amigável para exibir no front (com acento). */
    private final String rotulo;
    /** Menor Elo que ainda pertence a esta faixa. */
    private final int eloMinimo;

    Rank(String rotulo, int eloMinimo) {
        this.rotulo = rotulo;
        this.eloMinimo = eloMinimo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getEloMinimo() {
        return eloMinimo;
    }

    /**
     * Maior Elo desta faixa: o piso da faixa seguinte menos 1. A última faixa
     * (Grande Mestre) não tem teto, então devolvemos {@link Integer#MAX_VALUE}.
     */
    public int getEloMaximo() {
        Rank[] todos = values();
        boolean ehUltima = ordinal() == todos.length - 1;
        return ehUltima ? Integer.MAX_VALUE : todos[ordinal() + 1].eloMinimo - 1;
    }

    /**
     * A faixa em que um dado Elo cai. Percorremos de cima para baixo e devolvemos
     * a primeira cujo piso o Elo alcança (Elos negativos caem em INICIANTE).
     */
    public static Rank de(int elo) {
        Rank[] todos = values();
        for (int i = todos.length - 1; i >= 0; i--) {
            if (elo >= todos[i].eloMinimo) {
                return todos[i];
            }
        }
        return INICIANTE;
    }
}
