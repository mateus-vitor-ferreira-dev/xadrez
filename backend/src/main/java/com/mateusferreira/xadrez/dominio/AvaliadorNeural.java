package com.mateusferreira.xadrez.dominio;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

/**
 * Avaliador NEURAL: delega a nota da posição a uma rede neural treinada fora
 * do Java (Python/PyTorch) e exportada em ONNX, executada aqui pelo ONNX
 * Runtime. É a terceira implementação de {@link Avaliador} — a busca do
 * {@link MotorIA} não sabe (nem precisa saber) que a heurística virou uma rede.
 *
 * <p><b>Contrato do modelo</b> (o pipeline de treino em ml/ DEVE exportar assim):
 * <ul>
 *   <li><b>Entrada:</b> tensor float [1][780], NA ÓTICA DE QUEM AVALIA ('cor'):
 *       quando 'cor' é PRETO, o tabuleiro é espelhado na vertical
 *       (linha -&gt; 7-linha) e as cores das peças são trocadas.
 *       <ul>
 *         <li>0..767: 12 planos de 8x8 achatados. Índice =
 *             (tipo*2 + lado)*64 + (linha*8 + coluna), com tipo 0..5 = peão,
 *             cavalo, bispo, torre, rainha, rei e lado 0 = peça de quem avalia,
 *             1 = do oponente. Casa ocupada = 1.0.</li>
 *         <li>768..771: direitos de roque — meu na ala do rei, meu na ala da
 *             dama, dele na ala do rei, dele na ala da dama.</li>
 *         <li>772..779: coluna (a..h) do alvo de en passant, se houver.
 *             (Alas e colunas são invariantes ao espelhamento vertical.)</li>
 *       </ul></li>
 *   <li><b>Saída:</b> tensor float [1][1] = probabilidade de VITÓRIA DE QUEM
 *       AVALIA (0..1). Treinar sobre probabilidade (cp -&gt; sigmoid) satura
 *       melhor que centipeões crus em posições muito ganhas/perdidas.</li>
 * </ul>
 *
 * <p>Por que relativa e não absoluta? Duas razões. (1) A avaliação de xadrez
 * depende de QUEM está na vez (o "tempo" vale décimos de peão e decide posições
 * táticas); com a codificação relativa a rede enxerga isso sem precisar de uma
 * feature extra. (2) A nota já sai do ponto de vista certo para o negamax —
 * não há negação a esquecer, que é o bug clássico de engine neural que "joga
 * quase bem". De quebra, a simetria espelhada corta o espaço de entrada pela
 * metade (as brancas e as pretas compartilham o que a rede aprende).
 *
 * <p>A conversão probabilidade -&gt; centipeões usa a escala logística clássica
 * (cp = 173.7178 * ln(p/(1-p)), equivalente a 400*log10 das odds).
 *
 * <p>Thread-safety: {@link OrtSession#run} é seguro para chamadas concorrentes,
 * então uma única instância pode servir várias buscas ao mesmo tempo (o mesmo
 * padrão do MotorIA singleton). Implementa {@link AutoCloseable} para liberar
 * os recursos nativos da sessão.
 */
public class AvaliadorNeural implements Avaliador, AutoCloseable {

    /** Escala logística da conversão probabilidade -> centipeões (400/ln 10). */
    private static final double ESCALA_CP = 173.7178;
    /** Trava a probabilidade longe de 0/1 para o logit não explodir em infinito. */
    private static final double PROB_MIN = 1e-6;

    private static final int PLANOS = 12;
    private static final int CASAS = 64;
    // 12 planos + 4 direitos de roque + 8 colunas de en passant = 780
    private static final int FEATURES = PLANOS * CASAS + 4 + 8;
    private static final int BASE_ROQUE = PLANOS * CASAS;      // 768
    private static final int BASE_ENPASSANT = BASE_ROQUE + 4;  // 772

    private final OrtEnvironment ambiente;
    private final OrtSession sessao;
    private final String nomeEntrada;

    /** Carrega o modelo ONNX a partir dos bytes do arquivo .onnx. */
    public AvaliadorNeural(byte[] modelo) {
        try {
            this.ambiente = OrtEnvironment.getEnvironment();
            this.sessao = ambiente.createSession(modelo, new OrtSession.SessionOptions());
            // Nome do tensor de entrada descoberto do próprio modelo (não fixamos).
            this.nomeEntrada = sessao.getInputInfo().keySet().iterator().next();
        } catch (OrtException e) {
            throw new IllegalStateException("Falha ao carregar o modelo ONNX.", e);
        }
    }

    /** Carrega o modelo de um recurso do classpath (ex.: "/ia/modelo.onnx"). */
    public static AvaliadorNeural deRecurso(String recurso) {
        try (InputStream in = AvaliadorNeural.class.getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalStateException("Modelo ONNX não encontrado no classpath: " + recurso);
            }
            return new AvaliadorNeural(in.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o modelo ONNX: " + recurso, e);
        }
    }

    @Override
    public int avaliar(Partida partida, Cor cor) {
        // Codificação relativa: a probabilidade já sai do ponto de vista de
        // 'cor', que é exatamente o que o negamax espera. Sem negação.
        return paraCentipeoes(inferir(extrairFeatures(partida, cor)));
    }

    /**
     * Codifica a posição nos 780 floats do contrato, na ótica de 'cor': para as
     * pretas o tabuleiro é espelhado na vertical e as cores trocadas — a rede
     * sempre "olha o tabuleiro do seu lado", como um jogador que gira a mesa.
     * Roque e en passant entram como features extras (alas e colunas não mudam
     * com o espelhamento vertical).
     */
    private float[] extrairFeatures(Partida partida, Cor cor) {
        Tabuleiro t = partida.getTabuleiro();
        float[] features = new float[FEATURES];
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = t.pecaEm(new Posicao(linha, coluna));
                if (peca != null) {
                    int linhaRelativa = (cor == Cor.BRANCO) ? linha : 7 - linha;
                    int lado = (peca.getCor() == cor) ? 0 : 1; // 0 = minha, 1 = dele
                    int plano = tipo(peca) * 2 + lado;
                    features[plano * CASAS + linhaRelativa * 8 + coluna] = 1.0f;
                }
            }
        }
        // Direitos de roque relativos: maiúsculas = brancas no formato "KQkq".
        String roque = partida.direitosDeRoque();
        boolean brancoAvalia = (cor == Cor.BRANCO);
        if (roque.indexOf(brancoAvalia ? 'K' : 'k') >= 0) features[BASE_ROQUE] = 1.0f;
        if (roque.indexOf(brancoAvalia ? 'Q' : 'q') >= 0) features[BASE_ROQUE + 1] = 1.0f;
        if (roque.indexOf(brancoAvalia ? 'k' : 'K') >= 0) features[BASE_ROQUE + 2] = 1.0f;
        if (roque.indexOf(brancoAvalia ? 'q' : 'Q') >= 0) features[BASE_ROQUE + 3] = 1.0f;
        // Coluna do alvo de en passant (se houver).
        Posicao enPassant = partida.getAlvoEnPassant();
        if (enPassant != null) {
            features[BASE_ENPASSANT + enPassant.coluna()] = 1.0f;
        }
        return features;
    }

    /** Tipo 0..5 da peça (peão, cavalo, bispo, torre, rainha, rei) — como no Zobrist. */
    private int tipo(Peca peca) {
        if (peca instanceof Peao) return 0;
        if (peca instanceof Cavalo) return 1;
        if (peca instanceof Bispo) return 2;
        if (peca instanceof Torre) return 3;
        if (peca instanceof Rainha) return 4;
        return 5; // Rei
    }

    /** Roda a rede e devolve a probabilidade de vitória de quem avalia (0..1). */
    private double inferir(float[] features) {
        try (OnnxTensor entrada = OnnxTensor.createTensor(
                ambiente, FloatBuffer.wrap(features), new long[]{1, FEATURES});
             OrtSession.Result resultado = sessao.run(java.util.Map.of(nomeEntrada, entrada))) {
            float[][] saida = (float[][]) resultado.get(0).getValue();
            return saida[0][0];
        } catch (OrtException e) {
            throw new IllegalStateException("Falha na inferência do modelo ONNX.", e);
        }
    }

    /** Probabilidade (ótica de quem avalia) -> centipeões, pela escala logística. */
    private int paraCentipeoes(double prob) {
        double p = Math.min(1 - PROB_MIN, Math.max(PROB_MIN, prob));
        return (int) Math.round(ESCALA_CP * Math.log(p / (1 - p)));
    }

    @Override
    public void close() {
        try {
            sessao.close();
        } catch (Exception e) {
            // liberar recursos nativos é melhor-esforço; nada útil a fazer aqui
        }
    }
}
