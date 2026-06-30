package com.mateusferreira.xadrez.dominio;

/**
 * Uma casa do tabuleiro, identificada por (linha, coluna).
 *
 * ───────────────────────────────────────────────────────────────────────────
 * DECISÃO 1 — usar um 'record' em vez de uma classe normal.
 *
 *   Uma Posicao é um "value object": ela só carrega dados e não tem identidade
 *   própria (a casa (1,4) é igual a QUALQUER outra casa (1,4) — não importa
 *   "qual" objeto é). O 'record' do Java foi feito exatamente para isso: numa
 *   linha ele gera automaticamente para nós:
 *      - o construtor              new Posicao(1, 4)
 *      - os "getters"              posicao.linha(), posicao.coluna()
 *      - equals() e hashCode()     baseados nos valores (essencial! permite
 *                                  comparar e usar em Set/Map corretamente)
 *      - toString()
 *   Se fosse uma classe comum, teríamos que escrever tudo isso à mão (uns 30
 *   linhas). O record também é IMUTÁVEL: criada, não muda mais. Isso evita uma
 *   classe inteira de bugs (ninguém altera uma posição "por baixo dos panos").
 *
 * DECISÃO 2 — guardar linha/coluna como int de 0 a 7 (e NÃO como "e4").
 *
 *   Internamente, números 0–7 tornam a matemática de movimento trivial
 *   (andar uma casa = somar 1). A notação humana ("e4") fica só na "borda"
 *   do sistema (exibição/API). Misturar as duas no miolo é fonte de bug.
 *
 * DECISÃO 3 — NÃO validar o intervalo 0–7 no construtor.
 *
 *   Ao calcular movimentos, vamos gerar posições "candidatas" que podem cair
 *   fora do tabuleiro (ex.: a torre tentando andar além da borda). É mais
 *   simples gerar livremente e depois filtrar com dentroDoTabuleiro().
 * ───────────────────────────────────────────────────────────────────────────
 *
 * Convenção de coordenadas (decore, pra não se confundir depois):
 *   linha  0..7  ->  fileiras "1".."8" do xadrez
 *   coluna 0..7  ->  colunas  "a".."h" do xadrez
 *   Ex.: Posicao(0,0) = "a1" (canto da torre branca);  Posicao(7,7) = "h8".
 */
public record Posicao(int linha, int coluna) {

    /**
     * Cria uma Posicao a partir da notação de xadrez (ex.: "e4").
     * É o INVERSO do toString(): converte "coluna+fileira" de volta em índices.
     *   'e' - 'a' = 4  (coluna)     |     '4' - '1' = 3  (linha)
     * Útil para escrever jogadas de forma legível (Posicao.de("e2")).
     */
    public static Posicao de(String notacao) {
        int coluna = notacao.charAt(0) - 'a';
        int linha = notacao.charAt(1) - '1';
        return new Posicao(linha, coluna);
    }

    /** A casa existe no tabuleiro 8x8? (filtra as candidatas que "saíram" do tabuleiro) */
    public boolean dentroDoTabuleiro() {
        return linha >= 0 && linha <= 7 && coluna >= 0 && coluna <= 7;
    }

    /**
     * Cria uma nova posição deslocada por um "passo" (delta) em linha e coluna.
     * Como o record é imutável, NÃO alteramos esta posição: devolvemos uma NOVA.
     * Ex.: a partir de (1,4), deslocar(+1, 0) -> (2,4).  É a base do movimento.
     */
    public Posicao deslocar(int dLinha, int dColuna) {
        return new Posicao(linha + dLinha, coluna + dColuna);
    }

    /**
     * Representação em notação de xadrez (ex.: "e4"), útil para logs e para a
     * futura API. Sobrescreve o toString() que o record geraria.
     */
    @Override
    public String toString() {
        if (!dentroDoTabuleiro()) {
            return "(fora: " + linha + "," + coluna + ")";
        }
        char colunaLetra = (char) ('a' + coluna); // 0->'a', 1->'b', ...
        int fileiraNumero = linha + 1;             // 0->1,  1->2,  ...
        return "" + colunaLetra + fileiraNumero;
    }
}
