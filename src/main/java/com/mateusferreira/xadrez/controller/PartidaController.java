package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.EstadoPartidaResponse;
import com.mateusferreira.xadrez.controller.dto.JogadaRequest;
import com.mateusferreira.xadrez.dominio.Posicao;
import com.mateusferreira.xadrez.service.PartidaService;
import com.mateusferreira.xadrez.service.ResultadoPartida;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST das partidas. Continua "fino": só conhece o serviço e os
 * DTOs — não sabe nada de banco/entidade. As partidas agora têm 'id' na URL.
 */
@RestController
@RequestMapping("/partidas")
public class PartidaController {

    private final PartidaService service;

    public PartidaController(PartidaService service) {
        this.service = service;
    }

    /** POST /partidas -> cria uma partida nova e devolve o estado (com o id). */
    @PostMapping
    public EstadoPartidaResponse novaPartida() {
        ResultadoPartida r = service.novaPartida();
        return EstadoPartidaResponse.de(r.id(), r.partida());
    }

    /**
     * GET /partidas/{id} -> estado de uma partida específica.
     * @PathVariable liga o {id} da URL ao parâmetro do método.
     */
    @GetMapping("/{id}")
    public EstadoPartidaResponse verPartida(@PathVariable Long id) {
        ResultadoPartida r = service.verPartida(id);
        return EstadoPartidaResponse.de(r.id(), r.partida());
    }

    /** POST /partidas/{id}/jogadas -> aplica uma jogada na partida {id}. */
    @PostMapping("/{id}/jogadas")
    public EstadoPartidaResponse jogar(@PathVariable Long id, @RequestBody JogadaRequest jogada) {
        ResultadoPartida r = service.jogar(id, Posicao.de(jogada.origem()), Posicao.de(jogada.destino()));
        return EstadoPartidaResponse.de(r.id(), r.partida());
    }
}
