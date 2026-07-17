package com.mateusferreira.xadrez.torneio;

import java.util.List;

/**
 * Suíte de aberturas do torneio: linhas curtas (4 meios-lances) e equilibradas
 * a partir das quais os motores seguem sozinhos.
 *
 * <p>Por que ela existe: nossos motores são DETERMINÍSTICOS — da posição
 * inicial, dois avaliadores fixos produzem sempre a MESMA partida, e "100 jogos"
 * virariam 1 jogo copiado 100 vezes. Variar a abertura (e jogar cada uma com as
 * duas cores, para anular a vantagem das brancas) é o que dá amostras de
 * verdade ao experimento.
 */
public final class Aberturas {

    /** Uma linha de abertura: nome humano + lances em notação origem-destino. */
    public record Abertura(String nome, String[] lances) { }

    private Aberturas() { }

    /** Linhas clássicas, todas legais e razoavelmente equilibradas. */
    public static final List<Abertura> SUITE = List.of(
            new Abertura("Abertura Italiana/Espanhola", new String[]{"e2e4", "e7e5", "g1f3", "b8c6"}),
            new Abertura("Siciliana", new String[]{"e2e4", "c7c5", "g1f3", "d7d6"}),
            new Abertura("Francesa", new String[]{"e2e4", "e7e6", "d2d4", "d7d5"}),
            new Abertura("Caro-Kann", new String[]{"e2e4", "c7c6", "d2d4", "d7d5"}),
            new Abertura("Gambito da Dama Recusado", new String[]{"d2d4", "d7d5", "c2c4", "e7e6"}),
            new Abertura("Índia do Rei", new String[]{"d2d4", "g8f6", "c2c4", "g7g6"}),
            new Abertura("Nimzo/Índia da Dama", new String[]{"d2d4", "g8f6", "c2c4", "e7e6"}),
            new Abertura("Eslava", new String[]{"d2d4", "d7d5", "c2c4", "c7c6"}),
            new Abertura("Inglesa", new String[]{"c2c4", "e7e5", "b1c3", "g8f6"}),
            new Abertura("Sistema d4/Cf3", new String[]{"g1f3", "d7d5", "d2d4", "g8f6"}),
            new Abertura("Italiana (bispo)", new String[]{"e2e4", "e7e5", "f1c4", "g8f6"}),
            new Abertura("Pirc", new String[]{"e2e4", "d7d6", "d2d4", "g8f6"}),
            new Abertura("Holandesa", new String[]{"d2d4", "f7f5", "g2g3", "g8f6"}),
            new Abertura("Moderna", new String[]{"e2e4", "g7g6", "d2d4", "f8g7"}),
            new Abertura("Inglesa Simétrica", new String[]{"c2c4", "c7c5", "b1c3", "b8c6"}),
            new Abertura("Réti", new String[]{"g1f3", "g8f6", "c2c4", "c7c5"}),
            new Abertura("Vienense", new String[]{"e2e4", "e7e5", "b1c3", "g8f6"}),
            new Abertura("Siciliana Fechada", new String[]{"e2e4", "c7c5", "b1c3", "b8c6"}),
            new Abertura("Escandinava", new String[]{"e2e4", "d7d5", "e4d5", "d8d5"}),
            new Abertura("Londres", new String[]{"d2d4", "d7d5", "c1f4", "g8f6"})
    );
}
