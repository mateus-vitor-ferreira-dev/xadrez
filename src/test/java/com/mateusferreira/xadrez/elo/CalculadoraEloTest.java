package com.mateusferreira.xadrez.elo;

import com.mateusferreira.xadrez.dominio.Resultado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraEloTest {

    @Test
    void jogadoresIguais_vencedorGanhaMetadeDoK() {
        // Ratings iguais => esperado 0.5 para os dois.
        // Vencedor: 1200 + 32*(1 - 0.5) = 1216 ; perdedor: 1200 + 32*(0 - 0.5) = 1184.
        var v = CalculadoraElo.novosRatings(1200, 1200, Resultado.VITORIA_BRANCO);
        assertThat(v.eloBranco()).isEqualTo(1216);
        assertThat(v.eloPreto()).isEqualTo(1184);
        assertThat(v.deltaBranco()).isEqualTo(16);
        assertThat(v.deltaPreto()).isEqualTo(-16);
    }

    @Test
    void eloEhZeroSoma_oQueUmGanhaOutroPerde() {
        // Independentemente dos ratings, a soma das variações é ~0 (K é o mesmo).
        var v = CalculadoraElo.novosRatings(1400, 1000, Resultado.VITORIA_PRETO);
        assertThat(v.deltaBranco() + v.deltaPreto()).isEqualTo(0);
    }

    @Test
    void baterFavoritoRendeMaisQueBaterAzarao() {
        // Azarão (1000) vence favorito (1400): ganha MUITO (esperava perder).
        var upset = CalculadoraElo.novosRatings(1400, 1000, Resultado.VITORIA_PRETO);
        // Favorito (1400) vence azarão (1000): ganha POUCO (já era esperado).
        var esperado = CalculadoraElo.novosRatings(1400, 1000, Resultado.VITORIA_BRANCO);
        assertThat(upset.deltaPreto()).isGreaterThan(esperado.deltaBranco());
    }

    @Test
    void empateEntreDesiguais_favoritoPerdePontos() {
        // Empatar com um mais fraco custa pontos ao favorito (esperava vencer).
        var v = CalculadoraElo.novosRatings(1400, 1000, Resultado.EMPATE);
        assertThat(v.deltaBranco()).isNegative();
        assertThat(v.deltaPreto()).isPositive();
    }

    @Test
    void emAndamento_naoTemEloParaAplicar() {
        assertThatThrownBy(() -> CalculadoraElo.novosRatings(1200, 1200, Resultado.EM_ANDAMENTO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kProvisorioAteDezJogos() {
        // Jogador novo (poucos jogos) usa K maior; a partir de 10, o K padrão.
        assertThat(CalculadoraElo.kDe(0)).isEqualTo(CalculadoraElo.K_PROVISORIO);
        assertThat(CalculadoraElo.kDe(9)).isEqualTo(CalculadoraElo.K_PROVISORIO);
        assertThat(CalculadoraElo.kDe(10)).isEqualTo(CalculadoraElo.K);
    }

    @Test
    void kProvisorioFazOEloSaltarMais() {
        // Mesma vitória equilibrada: com K provisório (64) o ganho é o dobro do padrão (32).
        var provisorio = CalculadoraElo.novosRatings(800, 800, Resultado.VITORIA_BRANCO, 64, 64);
        var padrao = CalculadoraElo.novosRatings(800, 800, Resultado.VITORIA_BRANCO, 32, 32);
        assertThat(provisorio.deltaBranco()).isEqualTo(32);
        assertThat(padrao.deltaBranco()).isEqualTo(16);
    }

    @Test
    void bonusStreak_soComeçaNaTerceiraVitoriaEtemTeto() {
        assertThat(CalculadoraElo.bonusStreak(1)).isZero();
        assertThat(CalculadoraElo.bonusStreak(2)).isZero();
        assertThat(CalculadoraElo.bonusStreak(3)).isEqualTo(3);
        assertThat(CalculadoraElo.bonusStreak(4)).isEqualTo(6);
        assertThat(CalculadoraElo.bonusStreak(5)).isEqualTo(9);
        assertThat(CalculadoraElo.bonusStreak(20)).isEqualTo(9); // teto
    }
}
