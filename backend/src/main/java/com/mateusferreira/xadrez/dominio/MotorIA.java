package com.mateusferreira.xadrez.dominio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Inteligência artificial do xadrez, baseada em MINIMAX com poda ALPHA-BETA.
 *
 * Ideia: a IA simula o jogo 'profundidade' lances à frente, assumindo que os
 * dois lados jogam o melhor possível, e escolhe o lance que leva à melhor
 * posição para ela. A "nota" de uma posição vem de um {@link Avaliador}
 * intercambiável (material, posicional, ...), injetado no construtor.
 *
 * A poda alpha-beta corta muito mais quando os lances promissores são vistos
 * PRIMEIRO. Por isso ordenamos os lances antes de percorrê-los: capturas na
 * frente das jogadas quietas e, entre as capturas, por MVV-LVA (Most Valuable
 * Victim, Least Valuable Attacker — capturar dama com peão vem antes de capturar
 * peão com dama). O resultado escolhido é o mesmo; só chegamos nele mais rápido.
 *
 * A busca por tempo usa APROFUNDAMENTO ITERATIVO (iterative deepening): busca a
 * profundidade 1, 2, 3... dentro de um orçamento de tempo, guardando o melhor
 * lance de cada rodada completa. Isso é possível — e barato — graças à TABELA DE
 * TRANSPOSIÇÃO ({@link TabelaTransposicao}), que memoriza posições já avaliadas
 * (indexadas por hash de Zobrist) e ainda serve para ordenar melhor a rodada
 * seguinte (o melhor lance da profundidade anterior vai primeiro).
 *
 * É Java puro (não conhece Spring nem banco): opera só sobre a Partida.
 * As tabelas de Zobrist são {@code static final} imutáveis (thread-safe); todo
 * estado MUTÁVEL de busca (a tabela de transposição) é local a cada chamada,
 * porque o MotorIA é um singleton compartilhado entre requisições.
 */
public class MotorIA {

    // Pontuação de uma vitória/derrota por mate. Bem maior que qualquer material.
    private static final int MATE = 1_000_000;
    private static final int INFINITO = 9_999_999;
    // Toda captura pontua acima de qualquer lance quieto na ordenação.
    private static final int BASE_CAPTURA = 10_000;
    // Teto de profundidade do aprofundamento iterativo (o tempo é quem manda antes).
    private static final int PROFUNDIDADE_MAX = 64;

    // ---- Zobrist hashing: um long aleatório (seed fixa -> reprodutível) por
    // (peça, casa), pelos direitos de roque, pela coluna de en passant e pelo
    // lado a mover. O hash da posição é o XOR das parcelas presentes. ----
    private static final long[][] Z_PECA = new long[12][64];
    private static final long[] Z_ROQUE = new long[4];       // K, Q, k, q
    private static final long[] Z_ENPASSANT = new long[8];   // coluna a..h
    private static final long Z_LADO;                        // XOR quando é a vez das pretas
    static {
        Random r = new Random(20250709L);
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 64; j++) {
                Z_PECA[i][j] = r.nextLong();
            }
        }
        for (int i = 0; i < 4; i++) Z_ROQUE[i] = r.nextLong();
        for (int i = 0; i < 8; i++) Z_ENPASSANT[i] = r.nextLong();
        Z_LADO = r.nextLong();
    }

    /** Sinaliza que o orçamento de tempo estourou; sem stack trace (é fluxo de controle). */
    private static final class TempoEsgotado extends RuntimeException {
        TempoEsgotado() {
            super(null, null, false, false);
        }
    }
    private static final TempoEsgotado TEMPO_ESGOTADO = new TempoEsgotado();

    // Função de avaliação (ponto de extensão): material, posicional, neural...
    private final Avaliador avaliador;

    /** Motor com o avaliador padrão (material + posicional). */
    public MotorIA() {
        this(new AvaliadorPosicional());
    }

    /** Motor com um avaliador específico — usado em torneios/experimentos. */
    public MotorIA(Avaliador avaliador) {
        this.avaliador = avaliador;
    }

    /**
     * Escolhe o melhor lance a uma 'profundidade' FIXA. Devolve vazio se não
     * houver lances (fim de jogo). Sem limite de tempo — usado sobretudo em testes.
     */
    public Optional<Jogada> melhorJogada(Partida partida, int profundidade) {
        return Optional.ofNullable(
                buscaRaiz(partida, profundidade, new TabelaTransposicao(), Long.MAX_VALUE));
    }

    /**
     * Escolhe o melhor lance dentro de um ORÇAMENTO DE TEMPO (em ms), por
     * aprofundamento iterativo: busca 1, 2, 3... plies até o tempo acabar e
     * devolve o melhor lance da última profundidade COMPLETADA. Devolve vazio só
     * se não houver lances legais.
     */
    public Optional<Jogada> melhorJogada(Partida partida, long orcamentoMs) {
        Cor cor = partida.getJogadorAtual();
        List<Jogada> jogadas = jogadasOrdenadas(partida, cor);
        if (jogadas.isEmpty()) {
            return Optional.empty();
        }
        // Fallback garantido: se o tempo estourar antes de concluir até a
        // profundidade 1, ao menos devolvemos o melhor lance por ordenação.
        Jogada melhor = jogadas.get(0);

        long fim = System.nanoTime() + Math.max(1L, orcamentoMs) * 1_000_000L;
        TabelaTransposicao tt = new TabelaTransposicao();
        for (int profundidade = 1; profundidade <= PROFUNDIDADE_MAX; profundidade++) {
            try {
                Jogada m = buscaRaiz(partida, profundidade, tt, fim);
                if (m != null) {
                    melhor = m;
                }
            } catch (TempoEsgotado e) {
                break; // descarta a profundidade incompleta, mantém a anterior
            }
            if (System.nanoTime() >= fim) {
                break;
            }
        }
        return Optional.of(melhor);
    }

    /**
     * Busca na RAIZ: percorre os lances do jogador da vez com janela cheia
     * (valor exato de cada um) e devolve o melhor. Reordena pela melhor jogada já
     * conhecida na tabela (iteração anterior do aprofundamento) e, ao terminar,
     * registra a sua escolha para a próxima iteração. Devolve null se não há lances.
     */
    private Jogada buscaRaiz(Partida partida, int profundidade, TabelaTransposicao tt, long fim) {
        Cor cor = partida.getJogadorAtual();
        List<Jogada> jogadas = jogadasOrdenadas(partida, cor);
        if (jogadas.isEmpty()) {
            return null;
        }
        long chave = zobrist(partida);
        TabelaTransposicao.Entrada e = tt.buscar(chave);
        if (e != null && e.melhor != null) {
            moverParaFrente(jogadas, e.melhor);
        }

        Jogada melhor = jogadas.get(0);
        int melhorValor = -INFINITO;
        for (Jogada jogada : jogadas) {
            Partida copia = partida.copia();
            copia.mover(jogada.origem(), jogada.destino());
            // valor do ponto de vista do oponente, negado -> do nosso ponto de vista
            int valor = -negamax(copia, profundidade - 1, -INFINITO, INFINITO, tt, fim);
            if (valor > melhorValor) {
                melhorValor = valor;
                melhor = jogada;
            }
        }
        tt.guardar(chave, profundidade, melhorValor, TabelaTransposicao.Tipo.EXATO, melhor);
        return melhor;
    }

    /** Leva 'alvo' para o começo da lista (ordenação por melhor lance conhecido). */
    private void moverParaFrente(List<Jogada> jogadas, Jogada alvo) {
        int i = jogadas.indexOf(alvo);
        if (i > 0) {
            jogadas.remove(i);
            jogadas.add(0, alvo);
        }
    }

    /**
     * Negamax: variação enxuta do minimax onde a nota é SEMPRE do ponto de vista
     * de quem está na vez, e a recursão nega o resultado do oponente.
     * 'alfa' e 'beta' são os limites da poda: se um ramo já é pior do que uma
     * alternativa garantida, paramos de explorá-lo.
     */
    private int negamax(Partida partida, int profundidade, int alfa, int beta,
                        TabelaTransposicao tt, long fim) {
        if (System.nanoTime() >= fim) {
            throw TEMPO_ESGOTADO;
        }

        // Consulta a tabela: se já vimos ESTA posição numa busca ao menos tão
        // profunda, reaproveitamos o valor (respeitando se ele era exato ou limite).
        int alfaOriginal = alfa;
        long chave = zobrist(partida);
        TabelaTransposicao.Entrada e = tt.buscar(chave);
        if (e != null && e.profundidade >= profundidade) {
            switch (e.tipo) {
                case EXATO -> { return e.valor; }
                case INFERIOR -> alfa = Math.max(alfa, e.valor);
                case SUPERIOR -> beta = Math.min(beta, e.valor);
            }
            if (alfa >= beta) {
                return e.valor;
            }
        }

        Cor cor = partida.getJogadorAtual();
        List<Jogada> jogadas = jogadasLegais(partida, cor);

        // Sem lances legais: xeque-mate (péssimo) ou afogamento (empate = 0).
        if (jogadas.isEmpty()) {
            return partida.estaEmXeque(cor) ? -MATE : 0;
        }
        // Chegou ao limite da busca: em vez de avaliar já, estende a busca só nas
        // capturas (quiescência) para não parar no meio de uma troca (horizonte).
        if (profundidade == 0) {
            return quiescencia(partida, alfa, beta, fim);
        }
        ordenar(jogadas, partida);
        // Melhor lance conhecido desta posição vai primeiro (poda mais forte).
        if (e != null && e.melhor != null) {
            moverParaFrente(jogadas, e.melhor);
        }

        int melhor = -INFINITO;
        Jogada melhorJogada = null;
        for (Jogada jogada : jogadas) {
            Partida copia = partida.copia();
            copia.mover(jogada.origem(), jogada.destino());
            int valor = -negamax(copia, profundidade - 1, -beta, -alfa, tt, fim);
            if (valor > melhor) {
                melhor = valor;
                melhorJogada = jogada;
            }
            alfa = Math.max(alfa, melhor);
            if (alfa >= beta) {
                break; // poda: o oponente nunca deixaria chegar aqui
            }
        }

        // Classifica o valor em relação à janela original e guarda na tabela.
        TabelaTransposicao.Tipo tipo;
        if (melhor <= alfaOriginal) {
            tipo = TabelaTransposicao.Tipo.SUPERIOR; // fail-low: só um teto
        } else if (melhor >= beta) {
            tipo = TabelaTransposicao.Tipo.INFERIOR; // fail-high: só um piso
        } else {
            tipo = TabelaTransposicao.Tipo.EXATO;
        }
        tt.guardar(chave, profundidade, melhor, tipo, melhorJogada);
        return melhor;
    }

    /**
     * Busca de quiescência: no limite da profundidade, continua explorando APENAS
     * capturas até a posição ficar "quieta". Assim a IA não é enganada pelo efeito
     * horizonte — não conta como ganho uma captura que perde material na recaptura
     * do lance seguinte.
     *
     * 'standPat' é a nota de parar aqui (não somos obrigados a capturar): se já
     * supera beta, podamos; senão vira o piso de alfa. Termina sempre, porque cada
     * captura reduz o material em campo.
     */
    private int quiescencia(Partida partida, int alfa, int beta, long fim) {
        if (System.nanoTime() >= fim) {
            throw TEMPO_ESGOTADO;
        }
        Cor cor = partida.getJogadorAtual();
        int standPat = avaliador.avaliar(partida, cor);
        if (standPat >= beta) {
            return beta;
        }
        if (standPat > alfa) {
            alfa = standPat;
        }

        List<Jogada> capturas = capturasLegais(partida, cor);
        ordenar(capturas, partida);
        for (Jogada jogada : capturas) {
            Partida copia = partida.copia();
            copia.mover(jogada.origem(), jogada.destino());
            int valor = -quiescencia(copia, -beta, -alfa, fim);
            if (valor >= beta) {
                return beta;
            }
            if (valor > alfa) {
                alfa = valor;
            }
        }
        return alfa;
    }

    /** Só as capturas legais de 'cor' (subconjunto de jogadasLegais). */
    private List<Jogada> capturasLegais(Partida partida, Cor cor) {
        List<Jogada> capturas = new ArrayList<>();
        for (Jogada jogada : jogadasLegais(partida, cor)) {
            if (ehCaptura(partida, jogada)) {
                capturas.add(jogada);
            }
        }
        return capturas;
    }

    /**
     * Hash de Zobrist da posição: identifica a posição inteira (peças + de quem é
     * a vez + direitos de roque + alvo de en passant) num único long, para servir
     * de chave na tabela de transposição. Duas posições idênticas alcançadas por
     * ordens de lances diferentes têm o MESMO hash (é justamente o que queremos).
     * Package-private para permitir testes de transposição.
     */
    long zobrist(Partida partida) {
        Tabuleiro t = partida.getTabuleiro();
        long h = 0L;
        for (int linha = 0; linha < 8; linha++) {
            for (int col = 0; col < 8; col++) {
                Peca peca = t.pecaEm(new Posicao(linha, col));
                if (peca != null) {
                    h ^= Z_PECA[indicePeca(peca)][linha * 8 + col];
                }
            }
        }
        if (partida.getJogadorAtual() == Cor.PRETO) {
            h ^= Z_LADO;
        }
        String roque = partida.direitosDeRoque();
        if (roque.indexOf('K') >= 0) h ^= Z_ROQUE[0];
        if (roque.indexOf('Q') >= 0) h ^= Z_ROQUE[1];
        if (roque.indexOf('k') >= 0) h ^= Z_ROQUE[2];
        if (roque.indexOf('q') >= 0) h ^= Z_ROQUE[3];
        Posicao ep = partida.getAlvoEnPassant();
        if (ep != null) {
            h ^= Z_ENPASSANT[ep.coluna()];
        }
        return h;
    }

    /** Índice 0..11 da peça para o Zobrist: tipo (0..5) * 2 + cor (branco 0, preto 1). */
    private int indicePeca(Peca peca) {
        int tipo;
        if (peca instanceof Peao) tipo = 0;
        else if (peca instanceof Cavalo) tipo = 1;
        else if (peca instanceof Bispo) tipo = 2;
        else if (peca instanceof Torre) tipo = 3;
        else if (peca instanceof Rainha) tipo = 4;
        else tipo = 5; // Rei
        return tipo * 2 + (peca.getCor() == Cor.BRANCO ? 0 : 1);
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
        int atacante = AvaliadorMaterial.valor(partida.getTabuleiro().pecaEm(jogada.origem()));
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
            return AvaliadorMaterial.valor(alvo);
        }
        // En passant: a vítima é o peão que ficou na fileira da origem, coluna do destino.
        Peca peaoCapturado = t.pecaEm(new Posicao(jogada.origem().linha(), jogada.destino().coluna()));
        return peaoCapturado == null ? 0 : AvaliadorMaterial.valor(peaoCapturado);
    }

}
