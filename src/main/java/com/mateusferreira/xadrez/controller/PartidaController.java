package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.EstadoPartidaResponse;
import com.mateusferreira.xadrez.controller.dto.JogadaRequest;
import com.mateusferreira.xadrez.dominio.Posicao;
import com.mateusferreira.xadrez.dominio.TipoPromocao;
import com.mateusferreira.xadrez.service.PartidaService;
import com.mateusferreira.xadrez.service.ResultadoPartida;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST das partidas. Continua "fino": só conhece o serviço e os
 * DTOs — não sabe nada de banco/entidade. As partidas agora têm 'id' na URL.
 */
@RestController
@RequestMapping("/partidas")
public class PartidaController {

    private final PartidaService service;
    private final SimpMessagingTemplate messaging;

    public PartidaController(PartidaService service, SimpMessagingTemplate messaging) {
        this.service = service;
        this.messaging = messaging;
    }

    /** Publica o estado da partida no tópico, avisando os clientes conectados em tempo real. */
    private void publicar(EstadoPartidaResponse estado) {
        messaging.convertAndSend("/topic/partidas/" + estado.id(), estado);
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
        ResultadoPartida r = service.jogar(
                id,
                Posicao.de(jogada.origem()),
                Posicao.de(jogada.destino()),
                TipoPromocao.deNome(jogada.promocao()));
        EstadoPartidaResponse estado = EstadoPartidaResponse.de(r.id(), r.partida());
        publicar(estado); // avisa os dois jogadores em tempo real
        return estado;
    }

    /**
     * GET /partidas/{id}/movimentos?origem=e2 -> lista as casas de destino
     * legais da peça em 'origem' (ex.: ["e3","e4"]). Usado pelo front para
     * destacar os lances possíveis. @RequestParam lê o parâmetro da query string.
     */
    @GetMapping("/{id}/movimentos")
    public List<String> movimentosLegais(@PathVariable Long id, @RequestParam String origem) {
        return service.movimentosLegais(id, Posicao.de(origem))
                .stream()
                .map(Posicao::toString)
                .toList();
    }

    /**
     * POST /partidas/{id}/jogada-ia?nivel=2 -> a IA joga pelo jogador da vez.
     * 'nivel' (1–4) é a profundidade da busca minimax (default 2).
     */
    @PostMapping("/{id}/jogada-ia")
    public EstadoPartidaResponse jogadaIA(@PathVariable Long id,
                                          @RequestParam(defaultValue = "2") int nivel) {
        ResultadoPartida r = service.jogarIA(id, nivel);
        EstadoPartidaResponse estado = EstadoPartidaResponse.de(r.id(), r.partida());
        publicar(estado);
        return estado;
    }
}
