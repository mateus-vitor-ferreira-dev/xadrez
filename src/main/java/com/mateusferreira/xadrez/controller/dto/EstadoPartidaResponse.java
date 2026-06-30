package com.mateusferreira.xadrez.controller.dto;

import com.mateusferreira.xadrez.dominio.Cor;
import com.mateusferreira.xadrez.dominio.Partida;

/**
 * DTO de SAÍDA: a "fotografia" da partida que a API expõe. Agora inclui o 'id'
 * (gerado pelo banco), para o cliente saber em qual partida continuar jogando.
 */
public record EstadoPartidaResponse(
        Long id,
        Cor vez,
        boolean xeque,
        boolean xequeMate,
        boolean afogamento,
        String tabuleiro // 64 caracteres (serialização compacta) — fácil de parsear no frontend
) {
    public static EstadoPartidaResponse de(Long id, Partida partida) {
        Cor vez = partida.getJogadorAtual();
        return new EstadoPartidaResponse(
                id,
                vez,
                partida.estaEmXeque(vez),
                partida.estaEmXequeMate(vez),
                partida.estaEmAfogamento(vez),
                partida.getTabuleiro().serializar()
        );
    }
}
