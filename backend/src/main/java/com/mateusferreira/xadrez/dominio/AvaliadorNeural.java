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
 * <p><b>Contrato do modelo</b> (o pipeline de treino DEVE exportar assim):
 * <ul>
 *   <li><b>Entrada:</b> tensor float [1][768] — 12 planos de 8x8 achatados.
 *       Índice = (tipo*2 + corDaPeca)*64 + (linha*8 + coluna), com
 *       tipo 0..5 = peão, cavalo, bispo, torre, rainha, rei e
 *       corDaPeca 0 = branco, 1 = preto (a MESMA convenção do Zobrist em
 *       {@link MotorIA}). Casa ocupada = 1.0, vazia = 0.0. O tabuleiro é
 *       codificado em coordenadas ABSOLUTAS (ótica das brancas), sem espelhar.</li>
 *   <li><b>Saída:</b> tensor float [1][1] = probabilidade de VITÓRIA DAS BRANCAS
 *       (0..1). Treinar sobre probabilidade (cp -&gt; sigmoid) satura melhor que
 *       centipeões crus em posições muito ganhas/perdidas.</li>
 * </ul>
 *
 * <p>A conversão probabilidade -&gt; centipeões usa a escala logística clássica
 * (cp = 173.7178 * ln(p/(1-p)), equivalente a 400*log10 das odds). E como o
 * negamax espera a nota do ponto de vista de QUEM ESTÁ NA VEZ, o resultado é
 * NEGADO quando 'cor' é PRETO — esquecer essa negação é o bug clássico de
 * engine neural que "joga quase bem".
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
    private static final int FEATURES = PLANOS * CASAS; // 768

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
        double probBrancas = inferir(extrairFeatures(partida));
        int cpBrancas = paraCentipeoes(probBrancas);
        // Nota sempre do ponto de vista de 'cor' (contrato do Avaliador/negamax).
        return (cor == Cor.BRANCO) ? cpBrancas : -cpBrancas;
    }

    /** Codifica a posição nos 768 floats do contrato (12 planos de 8x8). */
    private float[] extrairFeatures(Partida partida) {
        Tabuleiro t = partida.getTabuleiro();
        float[] features = new float[FEATURES];
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = t.pecaEm(new Posicao(linha, coluna));
                if (peca != null) {
                    features[indicePlano(peca) * CASAS + linha * 8 + coluna] = 1.0f;
                }
            }
        }
        return features;
    }

    /** Plano 0..11 da peça: tipo (0..5) * 2 + cor (branco 0, preto 1) — como no Zobrist. */
    private int indicePlano(Peca peca) {
        int tipo;
        if (peca instanceof Peao) tipo = 0;
        else if (peca instanceof Cavalo) tipo = 1;
        else if (peca instanceof Bispo) tipo = 2;
        else if (peca instanceof Torre) tipo = 3;
        else if (peca instanceof Rainha) tipo = 4;
        else tipo = 5; // Rei
        return tipo * 2 + (peca.getCor() == Cor.BRANCO ? 0 : 1);
    }

    /** Roda a rede e devolve a probabilidade de vitória das brancas (0..1). */
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

    /** Probabilidade (ótica das brancas) -> centipeões, pela escala logística. */
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
