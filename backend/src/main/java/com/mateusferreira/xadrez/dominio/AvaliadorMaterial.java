package com.mateusferreira.xadrez.dominio;

/**
 * Avaliador puramente MATERIAL: soma o valor das peças de 'cor' e subtrai as do
 * adversário (em centipeões). É a heurística mais simples — não tem opinião
 * sobre ONDE as peças estão — e serve de linha de base em comparações.
 */
public class AvaliadorMaterial implements Avaliador {

    @Override
    public int avaliar(Partida partida, Cor cor) {
        Tabuleiro t = partida.getTabuleiro();
        int total = 0;
        for (Posicao p : t.posicoesDe(cor)) {
            total += valor(t.pecaEm(p));
        }
        for (Posicao p : t.posicoesDe(cor.oposta())) {
            total -= valor(t.pecaEm(p));
        }
        return total;
    }

    /**
     * Valor clássico de cada peça em centipeões (o rei não conta: nunca é
     * capturado). Fonte ÚNICA desses valores, reusada pelo avaliador posicional
     * e pela ordenação MVV-LVA do {@link MotorIA}.
     */
    public static int valor(Peca peca) {
        if (peca instanceof Peao) return 100;
        if (peca instanceof Cavalo) return 320;
        if (peca instanceof Bispo) return 330;
        if (peca instanceof Torre) return 500;
        if (peca instanceof Rainha) return 900;
        return 0;
    }
}
