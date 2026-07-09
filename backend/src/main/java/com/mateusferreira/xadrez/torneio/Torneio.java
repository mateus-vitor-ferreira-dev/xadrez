package com.mateusferreira.xadrez.torneio;

import com.mateusferreira.xadrez.dominio.Avaliador;
import com.mateusferreira.xadrez.dominio.AvaliadorMaterial;
import com.mateusferreira.xadrez.dominio.AvaliadorNeural;
import com.mateusferreira.xadrez.dominio.AvaliadorPosicional;
import com.mateusferreira.xadrez.dominio.MotorIA;
import com.mateusferreira.xadrez.dominio.Resultado;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TORNEIO entre avaliadores — o experimento controlado da IA (ablation study).
 *
 * <p>Os três avaliadores jogam entre si com a MESMA busca ({@link MotorIA}) e o
 * MESMO tempo por lance: a única variável é a função de avaliação. Cada
 * pareamento joga a suíte de aberturas inteira com as CORES TROCADAS (anula a
 * vantagem das brancas), e o placar sai com intervalo de confiança e diferença
 * de Elo — "medi o ganho de cada componente isoladamente", com números.
 *
 * <p>Uso (da pasta backend/):
 * <pre>
 *   mvn -q compile exec:java -Dexec.args="--tempo 100 --aberturas 20 --modelo ../ml/saida/modelo.onnx"
 * </pre>
 */
public final class Torneio {

    /** Placar agregado de um pareamento, do ponto de vista do avaliador A. */
    private record Placar(String nomeA, String nomeB, int vitoriasA, int empates, int vitoriasB) {
        int jogos() {
            return vitoriasA + empates + vitoriasB;
        }

        /** Pontuação de A: vitória = 1, empate = 0.5 (fração 0..1). */
        double score() {
            return (vitoriasA + 0.5 * empates) / jogos();
        }

        /**
         * Erro-padrão do score: os jogos são um sorteio com três resultados
         * (1, 0.5, 0), então usamos a variância amostral dessa distribuição —
         * aproximação normal padrão em testes de engines.
         */
        double erroPadrao() {
            double s = score();
            double variancia = (vitoriasA * Math.pow(1 - s, 2)
                    + empates * Math.pow(0.5 - s, 2)
                    + vitoriasB * Math.pow(0 - s, 2)) / jogos();
            return Math.sqrt(variancia / jogos());
        }

        /** Diferença de Elo equivalente a um score (curva logística do Elo). */
        static double elo(double score) {
            double s = Math.min(0.999, Math.max(0.001, score));
            return -400.0 * Math.log10(1.0 / s - 1.0);
        }
    }

    public static void main(String[] args) throws Exception {
        long tempoMs = 100;
        int aberturas = Aberturas.SUITE.size();
        int tetoDeLances = 300;
        Path modelo = Path.of("../ml/saida/modelo.onnx");
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--tempo" -> tempoMs = Long.parseLong(args[++i]);
                case "--aberturas" -> aberturas = Integer.parseInt(args[++i]);
                case "--teto" -> tetoDeLances = Integer.parseInt(args[++i]);
                case "--modelo" -> modelo = Path.of(args[++i]);
                default -> { }
            }
        }
        aberturas = Math.min(aberturas, Aberturas.SUITE.size());

        Map<String, Avaliador> avaliadores = new LinkedHashMap<>();
        avaliadores.put("Material", new AvaliadorMaterial());
        avaliadores.put("Posicional", new AvaliadorPosicional());
        AvaliadorNeural neural = null;
        if (Files.exists(modelo)) {
            neural = new AvaliadorNeural(Files.readAllBytes(modelo));
            avaliadores.put("Neural", neural);
        } else {
            System.out.println("(modelo " + modelo + " não encontrado — torneio sem o avaliador Neural;"
                    + " treine com ml/treina.py ou aponte --modelo)");
        }

        List<String> nomes = new ArrayList<>(avaliadores.keySet());
        System.out.printf("Torneio: %s | %d aberturas x 2 cores por pareamento | %d ms/lance%n%n",
                String.join(" x ", nomes), aberturas, tempoMs);

        int paralelismo = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ExecutorService executor = Executors.newFixedThreadPool(paralelismo);
        List<Placar> placares = new ArrayList<>();
        try {
            // Todos os pares (A, B) com A antes de B na lista: MxP, MxN, PxN.
            for (int a = 0; a < nomes.size(); a++) {
                for (int b = a + 1; b < nomes.size(); b++) {
                    placares.add(jogarPareamento(
                            nomes.get(a), avaliadores.get(nomes.get(a)),
                            nomes.get(b), avaliadores.get(nomes.get(b)),
                            aberturas, tempoMs, tetoDeLances, executor));
                }
            }
        } finally {
            executor.shutdown();
            if (neural != null) {
                neural.close();
            }
        }

        System.out.println("\n=== RESULTADO (score do 1º avaliador; IC 95%; +Elo = 1º melhor) ===");
        for (Placar p : placares) {
            double s = p.score();
            double ic = 1.96 * p.erroPadrao();
            System.out.printf("%-10s x %-10s  +%d =%d -%d   score %.1f%% ± %.1f   Elo %+.0f [%+.0f, %+.0f]%n",
                    p.nomeA(), p.nomeB(), p.vitoriasA(), p.empates(), p.vitoriasB(),
                    100 * s, 100 * ic,
                    Placar.elo(s), Placar.elo(s - ic), Placar.elo(s + ic));
        }
    }

    /** Joga um pareamento completo: cada abertura 2x, trocando as cores. */
    private static Placar jogarPareamento(String nomeA, Avaliador a, String nomeB, Avaliador b,
                                          int aberturas, long tempoMs, int teto,
                                          ExecutorService executor) throws Exception {
        MotorIA motorA = new MotorIA(a);
        MotorIA motorB = new MotorIA(b);

        // Submete os 2*aberturas jogos; cada Future devolve o resultado na
        // perspectiva de A (1 = A venceu, 0.5 = empate, 0 = B venceu).
        List<Future<Double>> futuros = new ArrayList<>();
        for (int i = 0; i < aberturas; i++) {
            Aberturas.Abertura abertura = Aberturas.SUITE.get(i);
            futuros.add(executor.submit(jogo(motorA, motorB, abertura, tempoMs, teto, true)));
            futuros.add(executor.submit(jogo(motorB, motorA, abertura, tempoMs, teto, false)));
        }

        int vitoriasA = 0, empates = 0, vitoriasB = 0;
        for (Future<Double> f : futuros) {
            double pontos = f.get();
            if (pontos == 1.0) vitoriasA++;
            else if (pontos == 0.0) vitoriasB++;
            else empates++;
        }
        Placar placar = new Placar(nomeA, nomeB, vitoriasA, empates, vitoriasB);
        System.out.printf("%-10s x %-10s  +%d =%d -%d (%d jogos)%n",
                nomeA, nomeB, vitoriasA, empates, vitoriasB, placar.jogos());
        return placar;
    }

    /** Um jogo como tarefa: 'aDeBrancas' diz se o avaliador A está de brancas. */
    private static Callable<Double> jogo(MotorIA brancas, MotorIA pretas,
                                         Aberturas.Abertura abertura,
                                         long tempoMs, int teto, boolean aDeBrancas) {
        return () -> {
            Resultado r = Confronto.jogar(brancas, pretas, abertura, tempoMs, teto);
            if (r == Resultado.EMPATE) {
                return 0.5;
            }
            boolean brancasVenceram = r == Resultado.VITORIA_BRANCO;
            return (brancasVenceram == aDeBrancas) ? 1.0 : 0.0;
        };
    }
}
