package com.mateusferreira.xadrez.console;

import com.mateusferreira.xadrez.dominio.Cor;
import com.mateusferreira.xadrez.dominio.Partida;
import com.mateusferreira.xadrez.dominio.Posicao;

/**
 * Pequeno programa de demonstração: joga o "Mate do Pastor" (xeque-mate em 4
 * lances) e imprime o tabuleiro a cada jogada, mostrando o motor em ação.
 *
 * Esta classe é a CAMADA DE CONSOLE: é a única que faz I/O (System.out). O
 * domínio só fornece os dados (desenhar(), estaEmXequeMate()).
 */
public class DemoConsole {

    public static void main(String[] args) {
        Partida partida = Partida.nova();

        System.out.println("=== Mate do Pastor ===\n");
        System.out.println("Posição inicial:");
        System.out.println(partida.getTabuleiro().desenhar());

        // Cada par é uma jogada {origem, destino} em notação de xadrez.
        String[][] jogadas = {
                {"e2", "e4"}, {"e7", "e5"},   // 1.
                {"f1", "c4"}, {"b8", "c6"},   // 2.
                {"d1", "h5"}, {"g8", "f6"},   // 3. (f6 é o erro fatal das pretas)
                {"h5", "f7"}                  // 4. dama captura em f7 -> mate
        };

        for (String[] jogada : jogadas) {
            Posicao origem = Posicao.de(jogada[0]);
            Posicao destino = Posicao.de(jogada[1]);
            partida.mover(origem, destino);

            System.out.println("Jogada: " + jogada[0] + " -> " + jogada[1]
                    + "   (agora é a vez das " + partida.getJogadorAtual() + ")");
            System.out.println(partida.getTabuleiro().desenhar());
        }

        if (partida.estaEmXequeMate(Cor.PRETO)) {
            System.out.println(">>> XEQUE-MATE! As brancas venceram. <<<");
        } else if (partida.estaEmXeque(Cor.PRETO)) {
            System.out.println(">>> Xeque (mas não mate). <<<");
        }
    }
}
