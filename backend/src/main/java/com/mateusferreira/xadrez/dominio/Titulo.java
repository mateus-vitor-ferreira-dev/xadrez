package com.mateusferreira.xadrez.dominio;

import java.util.Arrays;
import java.util.List;

/**
 * Título cosmético exibido ao lado do apelido (ranking, perfil) — um brinde do
 * "caminho de troféus". É desbloqueado por Elo, nas MESMAS faixas do {@link Rank}.
 *
 * <p><b>Por que mora no servidor (e as skins não):</b> o título é <b>público</b> —
 * aparece para os outros jogadores no ranking —, então o escolhido precisa ser
 * guardado no banco ({@code Usuario.tituloEquipado}). As skins só mudam o SEU
 * tabuleiro, por isso podem ser client-side.
 */
public enum Titulo {

    APRENDIZ("Aprendiz", 0),
    ESCUDEIRO("Escudeiro", 1000),
    CAVALEIRO("Cavaleiro", 1400),
    ESTRATEGISTA("Estrategista", 1800),
    TATICO_MESTRE("Tático Mestre", 2100),
    LENDA("Lenda do Tabuleiro", 2400);

    private final String rotulo;
    private final int eloMinimo;

    Titulo(String rotulo, int eloMinimo) {
        this.rotulo = rotulo;
        this.eloMinimo = eloMinimo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getEloMinimo() {
        return eloMinimo;
    }

    /** Um título está liberado quando o Elo alcança o piso da sua faixa. */
    public boolean liberadoPara(int elo) {
        return elo >= eloMinimo;
    }

    /** Os títulos que um dado Elo desbloqueia (do menor para o maior). */
    public static List<Titulo> desbloqueados(int elo) {
        return Arrays.stream(values()).filter(t -> t.liberadoPara(elo)).toList();
    }
}
