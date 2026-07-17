package com.mateusferreira.xadrez.torneio;

import com.mateusferreira.xadrez.dominio.AvaliadorMaterial;
import com.mateusferreira.xadrez.dominio.MotorIA;
import com.mateusferreira.xadrez.dominio.Partida;
import com.mateusferreira.xadrez.dominio.Posicao;
import com.mateusferreira.xadrez.dominio.Resultado;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfrontoTest {

    @Test
    void todasAsAberturasDaSuiteSaoLegais() {
        // Se alguma linha da suíte tiver um lance ilegal, mover() lança exceção
        // e o teste falha — protege o torneio de aberturas digitadas errado.
        for (Aberturas.Abertura abertura : Aberturas.SUITE) {
            Partida partida = Partida.nova();
            for (String lance : abertura.lances()) {
                partida.mover(Posicao.de(lance.substring(0, 2)), Posicao.de(lance.substring(2)));
            }
        }
    }

    @Test
    void confrontoTerminaEDevolveResultado() {
        // Dois motores idênticos e determinísticos tenderiam a jogar para sempre;
        // o confronto precisa terminar via mate, repetição ou teto de lances.
        MotorIA motor = new MotorIA(new AvaliadorMaterial());

        Resultado r = Confronto.jogar(motor, motor, Aberturas.SUITE.get(0), 10, 120);

        assertNotNull(r);
        assertNotEquals(Resultado.EM_ANDAMENTO, r);
    }
}
