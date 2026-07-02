package com.mateusferreira.xadrez.elo;

import com.mateusferreira.xadrez.dominio.Resultado;

/**
 * Cálculo do rating Elo — a mesma matemática usada no xadrez de verdade (FIDE),
 * no futebol e em rankings de jogos online.
 *
 * <p>É uma classe PURA: só faz conta a partir de números, sem banco, sem Spring.
 * Isso a torna trivial de testar (veja CalculadoraEloTest) e fácil de entender.
 *
 * <h3>A ideia</h3>
 * Antes da partida, calculamos a <b>pontuação esperada</b> de cada jogador — a
 * probabilidade estimada de vitória, que depende SÓ da diferença de rating:
 * <pre>{@code  E = 1 / (1 + 10^((ratingOponente - ratingJogador) / 400)) }</pre>
 * Se os dois têm o mesmo rating, E = 0.5 (50%). A cada 400 pontos de vantagem,
 * a chance de vitória fica ~10x maior.
 *
 * <p>Depois comparamos o esperado com o <b>resultado real</b> S (1 vitória,
 * 0.5 empate, 0 derrota) e ajustamos:
 * <pre>{@code  R' = R + K * (S - E) }</pre>
 * <b>K</b> é o "tamanho do passo": quanto o rating pode mudar numa partida.
 * Usamos K = 32 (comum em plataformas online). Ganhar de um mais forte
 * (E baixo) rende muitos pontos; ganhar de um mais fraco (E alto) rende poucos.
 */
public final class CalculadoraElo {

    /** Fator K padrão: o ajuste máximo de rating por partida (jogador estabelecido). */
    public static final int K = 32;

    /**
     * K "provisório": maior, usado nas primeiras partidas de um jogador para ele
     * chegar rápido ao seu nível real (o Elo salta mais a cada resultado).
     */
    public static final int K_PROVISORIO = 64;

    /** Nº de partidas ranqueadas em que o jogador ainda é considerado provisório. */
    public static final int PARTIDAS_PROVISORIAS = 10;

    private CalculadoraElo() {
        // utilitária: só métodos estáticos.
    }

    /** K a usar conforme quantas partidas ranqueadas o jogador já disputou. */
    public static int kDe(int jogosRanqueados) {
        return jogosRanqueados < PARTIDAS_PROVISORIAS ? K_PROVISORIO : K;
    }

    /**
     * Bônus modesto de Elo por maré de vitórias, aplicado SÓ ao vencedor e sobre
     * o streak já atualizado (nº de vitórias consecutivas incluindo esta). Começa
     * na 3ª vitória seguida e é limitado, para engajar sem virar alvo de farm:
     * 3ª → +3, 4ª → +6, 5ª ou mais → +9 (teto).
     */
    public static int bonusStreak(int vitoriasSeguidas) {
        if (vitoriasSeguidas < 3) {
            return 0;
        }
        return Math.min((vitoriasSeguidas - 2) * 3, 9);
    }

    /**
     * Novos ratings dos dois jogadores após uma partida.
     *
     * @param eloBranco    rating atual das brancas
     * @param eloPreto     rating atual das pretas
     * @param resultado    desfecho (não pode ser EM_ANDAMENTO)
     * @return par (novo elo branco, novo elo preto)
     */
    public static Variacao novosRatings(int eloBranco, int eloPreto, Resultado resultado) {
        return novosRatings(eloBranco, eloPreto, resultado, K, K);
    }

    /**
     * Como {@link #novosRatings(int, int, Resultado)}, mas com um K por lado — é o
     * que permite o K provisório (cada jogador pode estar numa fase diferente).
     *
     * @param kBranco fator K das brancas (ver {@link #kDe(int)})
     * @param kPreto  fator K das pretas
     */
    public static Variacao novosRatings(int eloBranco, int eloPreto, Resultado resultado, int kBranco, int kPreto) {
        double scoreBranco = switch (resultado) {
            case VITORIA_BRANCO -> 1.0;
            case VITORIA_PRETO -> 0.0;
            case EMPATE -> 0.5;
            case EM_ANDAMENTO -> throw new IllegalArgumentException(
                    "Não há Elo a aplicar numa partida em andamento.");
        };
        // O score das pretas é o complemento (a soma é sempre 1).
        int novoBranco = novoRating(eloBranco, eloPreto, scoreBranco, kBranco);
        int novoPreto = novoRating(eloPreto, eloBranco, 1.0 - scoreBranco, kPreto);
        return new Variacao(novoBranco, novoPreto, novoBranco - eloBranco, novoPreto - eloPreto);
    }

    /** Novo rating de UM jogador dado seu score real (0, 0.5 ou 1) e seu fator K. */
    static int novoRating(int rating, int ratingOponente, double score, int k) {
        double esperado = 1.0 / (1.0 + Math.pow(10, (ratingOponente - rating) / 400.0));
        return (int) Math.round(rating + k * (score - esperado));
    }

    /** Resultado do cálculo: ratings novos e a variação (delta) de cada lado. */
    public record Variacao(int eloBranco, int eloPreto, int deltaBranco, int deltaPreto) {
    }
}
