package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RankTest {

    @Test
    void eloInicialCaiEmIntermediario() {
        // Toda conta nasce com Elo 1200 — deve começar em Intermediário.
        assertThat(Rank.de(1200)).isEqualTo(Rank.INTERMEDIARIO);
    }

    @Test
    void limitesDasFaixasSaoInclusivosNoPiso() {
        // O piso pertence à própria faixa; um a menos cai na faixa de baixo.
        assertThat(Rank.de(1400)).isEqualTo(Rank.AVANCADO);
        assertThat(Rank.de(1399)).isEqualTo(Rank.INTERMEDIARIO);
    }

    @Test
    void eloMuitoBaixoOuNegativoCaiEmIniciante() {
        assertThat(Rank.de(0)).isEqualTo(Rank.INICIANTE);
        assertThat(Rank.de(-50)).isEqualTo(Rank.INICIANTE);
    }

    @Test
    void topoNaoTemTeto() {
        assertThat(Rank.de(9999)).isEqualTo(Rank.GRAO_MESTRE);
        assertThat(Rank.GRAO_MESTRE.getEloMaximo()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void tetoDeUmaFaixaEncostaNoPisoDaSeguinte() {
        // Sem "buracos": o teto de uma faixa é o piso da próxima menos 1.
        assertThat(Rank.INTERMEDIARIO.getEloMaximo()).isEqualTo(Rank.AVANCADO.getEloMinimo() - 1);
    }
}
