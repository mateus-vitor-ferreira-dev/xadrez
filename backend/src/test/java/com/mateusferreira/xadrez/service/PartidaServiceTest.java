package com.mateusferreira.xadrez.service;

import com.mateusferreira.xadrez.dominio.Cor;
import com.mateusferreira.xadrez.dominio.Posicao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração do serviço COM o banco (H2 em memória sobe junto).
 */
@SpringBootTest
class PartidaServiceTest {

    @Autowired
    PartidaService service;

    @Test
    void springInjetaOServico() {
        assertNotNull(service);
    }

    @Test
    void novaPartidaPersisteEGeraId() {
        var resultado = service.novaPartida();

        assertNotNull(resultado.id(), "deveria ter recebido um id do banco");
        assertEquals(Cor.BRANCO, resultado.partida().getJogadorAtual());
    }

    @Test
    void jogadaFicaSalvaEntreChamadas() {
        Long id = service.novaPartida().id();

        service.jogar(id, Posicao.de("e2"), Posicao.de("e4"));

        // Recarrega DO BANCO numa nova chamada: a jogada persistiu.
        var recarregada = service.verPartida(id);
        assertEquals(Cor.PRETO, recarregada.partida().getJogadorAtual());
        assertTrue(recarregada.partida().getTabuleiro().estaVazia(Posicao.de("e2")));
    }

    @Test
    void partidaInexistenteLancaExcecao() {
        assertThrows(PartidaNaoEncontradaException.class,
                () -> service.verPartida(999_999L));
    }
}
