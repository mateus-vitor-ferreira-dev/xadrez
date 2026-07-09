package com.mateusferreira.xadrez.dominio;

/**
 * Desfecho de uma partida. É um conceito de DOMÍNIO (o resultado de um jogo de
 * xadrez), mas também é persistido como texto na entidade — por isso mora aqui,
 * no domínio puro, e é reaproveitado pela camada de persistência e pela API.
 */
public enum Resultado {
    /** Partida ainda em andamento (estado inicial). */
    EM_ANDAMENTO,
    /** Brancas venceram (deram xeque-mate). */
    VITORIA_BRANCO,
    /** Pretas venceram (deram xeque-mate). */
    VITORIA_PRETO,
    /** Empate (por ora: afogamento / stalemate). */
    EMPATE
}
