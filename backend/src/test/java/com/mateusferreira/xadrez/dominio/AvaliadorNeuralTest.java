package com.mateusferreira.xadrez.dominio;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static com.mateusferreira.xadrez.dominio.Posicao.de;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a integração completa do AvaliadorNeural (features -> ONNX Runtime ->
 * conversão em centipeões -> sinal) usando o modelo DUMMY, cujos pesos
 * reproduzem a contagem de material (ver ml/gera_modelo_dummy.py). Assim o
 * comportamento esperado é conhecido sem depender de um modelo treinado.
 */
class AvaliadorNeuralTest {

    private static final AvaliadorNeural avaliador =
            AvaliadorNeural.deRecurso("/ia/modelo-dummy.onnx");

    @AfterAll
    static void fechar() {
        avaliador.close();
    }

    @Test
    void dummyReproduzADiferencaDeMaterial() {
        // Brancas com uma dama a mais (+900 cp). Tolerância pequena por
        // arredondamento de float na sigmoid/logit.
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rainha(Cor.BRANCO), de("a4"));
        t.colocarPeca(new Rei(Cor.PRETO), de("e8"));
        Partida partida = new Partida(t, Cor.BRANCO);

        int notaBrancas = avaliador.avaliar(partida, Cor.BRANCO);
        int notaPretas = avaliador.avaliar(partida, Cor.PRETO);

        assertTrue(Math.abs(notaBrancas - 900) <= 2, "esperava ~+900, veio " + notaBrancas);
        // Contrato do negamax: a MESMA posição, vista pelas pretas, nega o sinal.
        assertEquals(-notaBrancas, notaPretas);
    }

    @Test
    void posicaoEquilibradaValeZero() {
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rei(Cor.PRETO), de("e8"));
        Partida partida = new Partida(t, Cor.BRANCO);

        assertEquals(0, avaliador.avaliar(partida, Cor.BRANCO));
    }

    @Test
    void motorComAvaliadorNeuralCapturaDamaPendurada() {
        // O mesmo cenário do MotorIATest, agora com a rede como avaliadora:
        // prova que a busca funciona de ponta a ponta sobre o ONNX.
        MotorIA motor = new MotorIA(avaliador);
        Tabuleiro t = new Tabuleiro();
        t.colocarPeca(new Rei(Cor.BRANCO), de("e1"));
        t.colocarPeca(new Rainha(Cor.BRANCO), de("a4"));
        t.colocarPeca(new Rei(Cor.PRETO), de("e8"));
        t.colocarPeca(new Torre(Cor.PRETO), de("a8"));
        Partida partida = new Partida(t, Cor.PRETO);

        Jogada jogada = motor.melhorJogada(partida, 1).orElseThrow();

        assertEquals(de("a8"), jogada.origem());
        assertEquals(de("a4"), jogada.destino());
    }
}
