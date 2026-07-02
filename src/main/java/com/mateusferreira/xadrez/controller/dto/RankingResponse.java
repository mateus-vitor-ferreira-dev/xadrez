package com.mateusferreira.xadrez.controller.dto;

import java.util.List;

/**
 * Resposta do GET /ranking — alimenta as DUAS tabelas da tela de uma vez só:
 *
 * <ul>
 *   <li>{@code topSite}: os maiores pontuadores do site inteiro (tabela da esquerda).</li>
 *   <li>{@code topRank}: os maiores pontuadores DA SUA faixa (tabela da direita).</li>
 * </ul>
 *
 * <p>{@code meuRank}/{@code meuElo} descrevem a faixa usada na tabela da direita
 * (sem login, assumimos o Elo inicial 1200 → Intermediário). Uma única chamada
 * evita duas idas ao servidor e mantém as duas tabelas coerentes.
 */
public record RankingResponse(
        String meuRank,
        int meuElo,
        List<Linha> topSite,
        List<Linha> topRank) {

    /** Uma linha de tabela: apelido, Elo e o rótulo do rank daquele jogador. */
    public record Linha(String usuario, int elo, String rank) {
    }
}
