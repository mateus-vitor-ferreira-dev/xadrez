package com.mateusferreira.xadrez.console;

import com.mateusferreira.xadrez.dominio.Cor;
import com.mateusferreira.xadrez.dominio.MovimentoInvalidoException;
import com.mateusferreira.xadrez.dominio.Partida;
import com.mateusferreira.xadrez.dominio.Posicao;

import java.util.Scanner;

/**
 * Jogo de xadrez INTERATIVO no terminal (2 jogadores, mesmo teclado).
 *
 * Você digita as jogadas no formato "origem destino" (ex.: e2 e4). O programa
 * aplica a jogada pelo domínio, redesenha o tabuleiro e avisa xeque/mate.
 * Jogadas inválidas são recusadas com uma mensagem, sem encerrar o jogo.
 *
 * É camada de console: o ÚNICO papel dela é ler do teclado e imprimir na tela.
 * Toda a regra continua no domínio (Partida).
 */
public class JogoConsole {

    public static void main(String[] args) {
        Partida partida = Partida.nova();

        // try-with-resources: o Scanner é fechado automaticamente ao final
        // (mesmo se ocorrer uma exceção), evitando vazamento de recurso.
        try (Scanner teclado = new Scanner(System.in)) {
            System.out.println("=== XADREZ ===");
            System.out.println("Digite a jogada como 'e2 e4'. Digite 'sair' para encerrar.\n");
            System.out.println(partida.getTabuleiro().desenhar());

            while (true) {
                Cor vez = partida.getJogadorAtual();
                System.out.print("Vez das " + vez + " > ");

                if (!teclado.hasNextLine()) {
                    break; // fim da entrada (Ctrl+D ou pipe acabou)
                }
                String entrada = teclado.nextLine().trim();

                if (entrada.equalsIgnoreCase("sair")) {
                    System.out.println("Até a próxima!");
                    break;
                }

                String[] partes = entrada.split("\\s+");
                if (partes.length != 2) {
                    System.out.println("✗ Formato inválido. Use: origem destino (ex.: e2 e4)\n");
                    continue;
                }

                // Tenta aplicar a jogada. Se for ilegal/mal formatada, avisa e segue.
                try {
                    partida.mover(Posicao.de(partes[0]), Posicao.de(partes[1]));
                } catch (MovimentoInvalidoException e) {
                    System.out.println("✗ " + e.getMessage() + "\n");
                    continue;
                } catch (RuntimeException e) {
                    System.out.println("✗ Entrada inválida (use casas de a1 a h8).\n");
                    continue;
                }

                System.out.println();
                System.out.println(partida.getTabuleiro().desenhar());

                // Após a jogada, é a vez do adversário: checamos a situação DELE.
                Cor proximo = partida.getJogadorAtual();
                if (partida.estaEmXequeMate(proximo)) {
                    System.out.println(">>> XEQUE-MATE! Vencem as " + proximo.oposta() + ". <<<");
                    break;
                } else if (partida.estaEmAfogamento(proximo)) {
                    System.out.println(">>> AFOGAMENTO — empate. <<<");
                    break;
                } else if (partida.estaEmXeque(proximo)) {
                    System.out.println("⚠ " + proximo + " está em XEQUE!\n");
                }
            }
        }
    }
}
