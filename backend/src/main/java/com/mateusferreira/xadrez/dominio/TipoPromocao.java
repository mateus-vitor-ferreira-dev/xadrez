package com.mateusferreira.xadrez.dominio;

/**
 * A peça escolhida quando um peão é promovido. Por padrão é RAINHA (o caso
 * quase universal), mas o jogador pode escolher torre, bispo ou cavalo.
 */
public enum TipoPromocao {
    RAINHA,
    TORRE,
    BISPO,
    CAVALO;

    /** Cria a peça correspondente, na cor informada. */
    public Peca cria(Cor cor) {
        return switch (this) {
            case RAINHA -> new Rainha(cor);
            case TORRE -> new Torre(cor);
            case BISPO -> new Bispo(cor);
            case CAVALO -> new Cavalo(cor);
        };
    }

    /** Converte um nome (vindo da API) em TipoPromocao; usa RAINHA se vazio/invalido. */
    public static TipoPromocao deNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return RAINHA;
        }
        try {
            return TipoPromocao.valueOf(nome.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RAINHA;
        }
    }
}
