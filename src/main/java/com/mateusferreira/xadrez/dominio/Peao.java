package com.mateusferreira.xadrez.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * O Peão — a peça com mais regras especiais, por isso estende Peca DIRETAMENTE
 * (não é deslizante nem de salto comum).
 *
 * Particularidades modeladas aqui:
 *   1. Direção depende da COR: branco sobe (linha +1), preto desce (linha -1).
 *   2. Avança 1 casa para frente — só se a casa estiver VAZIA (peão não captura
 *      andando para frente).
 *   3. No primeiro lance pode avançar 2 casas — desde que AS DUAS estejam
 *      livres (não pode pular peça).
 *   4. Captura somente na DIAGONAL para frente, e só se houver adversária lá.
 *
 * Ficam de FORA (regras de nível alto, tratadas na futura camada de regras):
 *   - Promoção (virar Rainha ao chegar na última fileira)
 *   - En passant
 */
public class Peao extends Peca {

    public Peao(Cor cor) {
        super(cor);
    }

    @Override
    public List<Posicao> movimentosPossiveis(Tabuleiro tabuleiro, Posicao origem) {
        List<Posicao> destinos = new ArrayList<>();

        // Branco anda para cima (+1); preto para baixo (-1). É o único lugar do
        // domínio onde o movimento depende da cor.
        int direcao = (getCor() == Cor.BRANCO) ? 1 : -1;

        // Fileira inicial de cada cor (linha 1 = "fileira 2"; linha 6 = "fileira 7").
        // Estar nela equivale a "ainda não se moveu" (peão nunca volta para trás).
        int fileiraInicial = (getCor() == Cor.BRANCO) ? 1 : 6;

        // --- 1) Avanço de uma casa (só se estiver vazia) ---
        Posicao umPasso = origem.deslocar(direcao, 0);
        boolean avancouUm = umPasso.dentroDoTabuleiro() && tabuleiro.estaVazia(umPasso);
        if (avancouUm) {
            destinos.add(umPasso);

            // --- 2) Avanço duplo: só no primeiro lance e se a 2ª casa também
            //        estiver vazia. Note que só chegamos aqui se a 1ª já estava
            //        livre (avancouUm == true), garantindo que ele não "pula". ---
            if (origem.linha() == fileiraInicial) {
                Posicao doisPassos = origem.deslocar(2 * direcao, 0);
                if (tabuleiro.estaVazia(doisPassos)) {
                    destinos.add(doisPassos);
                }
            }
        }

        // --- 3) Capturas nas duas diagonais da frente ---
        for (int dColuna : new int[]{-1, 1}) {
            Posicao diagonal = origem.deslocar(direcao, dColuna);
            // Só é movimento válido se houver uma peça ADVERSÁRIA na diagonal.
            if (tabuleiro.temPecaAdversaria(diagonal, getCor())) {
                destinos.add(diagonal);
            }
        }

        return destinos;
    }

    @Override
    public char simbolo() {
        return 'P';
    }
}
