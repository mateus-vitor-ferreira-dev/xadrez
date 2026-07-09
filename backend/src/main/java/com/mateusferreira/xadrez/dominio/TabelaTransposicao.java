package com.mateusferreira.xadrez.dominio;

import java.util.HashMap;
import java.util.Map;

/**
 * Tabela de transposição: cache das posições já avaliadas pela busca, indexadas
 * pelo hash de Zobrist. A MESMA posição costuma ser alcançada por várias ordens
 * de lances diferentes ("transposições"); guardando o resultado evitamos
 * re-buscar a subárvore inteira.
 *
 * <p>NÃO é thread-safe e é de propósito: cada busca ({@code melhorJogada}) cria a
 * sua própria instância. O {@link MotorIA} é um singleton compartilhado entre
 * requisições, então nada de estado de busca mutável mora nele.
 */
class TabelaTransposicao {

    /**
     * Natureza do valor guardado em relação à janela alpha-beta em que ele foi
     * obtido: valor EXATO, ou apenas um limite INFERIOR (fail-high) / SUPERIOR
     * (fail-low). Sem isso não dá para reusar com segurança um valor podado.
     */
    enum Tipo { EXATO, INFERIOR, SUPERIOR }

    static final class Entrada {
        final long chave;
        final int profundidade;
        final int valor;
        final Tipo tipo;
        final Jogada melhor;

        Entrada(long chave, int profundidade, int valor, Tipo tipo, Jogada melhor) {
            this.chave = chave;
            this.profundidade = profundidade;
            this.valor = valor;
            this.tipo = tipo;
            this.melhor = melhor;
        }
    }

    private final Map<Long, Entrada> mapa = new HashMap<>();

    /** Entrada da posição, ou null se não houver (ou se a chave não bater). */
    Entrada buscar(long chave) {
        Entrada e = mapa.get(chave);
        return (e != null && e.chave == chave) ? e : null;
    }

    /**
     * Guarda o resultado da posição. Política "depth-preferred": só sobrescreve
     * se a nova busca for pelo menos tão profunda quanto a guardada (resultados
     * mais profundos são mais confiáveis).
     */
    void guardar(long chave, int profundidade, int valor, Tipo tipo, Jogada melhor) {
        Entrada atual = mapa.get(chave);
        if (atual == null || profundidade >= atual.profundidade) {
            mapa.put(chave, new Entrada(chave, profundidade, valor, tipo, melhor));
        }
    }
}
