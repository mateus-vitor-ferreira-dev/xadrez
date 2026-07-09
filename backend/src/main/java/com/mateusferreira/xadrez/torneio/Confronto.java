package com.mateusferreira.xadrez.torneio;

import com.mateusferreira.xadrez.dominio.Cor;
import com.mateusferreira.xadrez.dominio.Jogada;
import com.mateusferreira.xadrez.dominio.MotorIA;
import com.mateusferreira.xadrez.dominio.Partida;
import com.mateusferreira.xadrez.dominio.Posicao;
import com.mateusferreira.xadrez.dominio.Resultado;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Joga UMA partida completa entre dois motores, a partir de uma abertura da
 * suíte, e devolve o {@link Resultado}.
 *
 * <p>O domínio ({@link Partida}) só conhece mate e afogamento; aqui adicionamos
 * as regras de empate que impedem partidas infinitas entre motores
 * determinísticos:
 * <ul>
 *   <li><b>Tripla repetição:</b> a mesma posição (tabuleiro + vez) pela 3ª vez
 *       empata — sem isso dois motores podem ficar repetindo lances para sempre;</li>
 *   <li><b>Teto de lances:</b> passou do limite, empate — cobre finais que os
 *       motores não sabem converter (equivalente pragmático da regra dos 50 lances).</li>
 * </ul>
 */
public final class Confronto {

    private Confronto() { }

    /**
     * Joga a partida: 'brancas' e 'pretas' alternam com o mesmo orçamento de
     * tempo por lance (a comparação é JUSTA por tempo: um avaliador mais caro
     * por nó busca menos fundo — esse custo faz parte do experimento).
     */
    public static Resultado jogar(MotorIA brancas, MotorIA pretas,
                                  Aberturas.Abertura abertura,
                                  long tempoPorLanceMs, int tetoDeLances) {
        Partida partida = Partida.nova();
        for (String lance : abertura.lances()) {
            partida.mover(Posicao.de(lance.substring(0, 2)), Posicao.de(lance.substring(2)));
        }

        Map<String, Integer> repeticoes = new HashMap<>();
        for (int plies = 0; plies < tetoDeLances; plies++) {
            Cor vez = partida.getJogadorAtual();
            if (partida.estaEmXequeMate(vez)) {
                return vez == Cor.BRANCO ? Resultado.VITORIA_PRETO : Resultado.VITORIA_BRANCO;
            }
            if (partida.estaEmAfogamento(vez)) {
                return Resultado.EMPATE;
            }
            String chave = partida.getTabuleiro().serializar() + vez;
            if (repeticoes.merge(chave, 1, Integer::sum) >= 3) {
                return Resultado.EMPATE; // tripla repetição
            }

            MotorIA daVez = (vez == Cor.BRANCO) ? brancas : pretas;
            Optional<Jogada> jogada = daVez.melhorJogada(partida, tempoPorLanceMs);
            if (jogada.isEmpty()) {
                return Resultado.EMPATE; // sem lances (já coberto acima; defesa extra)
            }
            partida.mover(jogada.get().origem(), jogada.get().destino());
        }
        return Resultado.EMPATE; // teto de lances
    }
}
