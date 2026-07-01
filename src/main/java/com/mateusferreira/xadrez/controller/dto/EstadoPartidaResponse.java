package com.mateusferreira.xadrez.controller.dto;

import com.mateusferreira.xadrez.dominio.Cor;
import com.mateusferreira.xadrez.dominio.Resultado;
import com.mateusferreira.xadrez.service.ResultadoPartida;

/**
 * DTO de SAÍDA: a "fotografia" da partida que a API expõe. Além do estado do
 * tabuleiro, carrega os dados de partida online / Elo (Fase 4): quem joga de
 * cada cor, o desfecho e, quando a partida termina, o Elo e a variação de cada
 * lado. Campos de Elo são {@code null} enquanto não há resultado.
 */
public record EstadoPartidaResponse(
        Long id,
        Cor vez,
        boolean xeque,
        boolean xequeMate,
        boolean afogamento,
        String tabuleiro, // 64 caracteres (serialização compacta) — fácil de parsear no frontend
        boolean online,
        String branco,
        String preto,
        Resultado resultado,
        Integer eloBranco,
        Integer eloPreto,
        Integer deltaBranco,
        Integer deltaPreto
) {
    /** Monta a resposta a partir do pacote devolvido pelo serviço. */
    public static EstadoPartidaResponse de(ResultadoPartida r) {
        var partida = r.partida();
        Cor vez = partida.getJogadorAtual();
        return new EstadoPartidaResponse(
                r.id(),
                vez,
                partida.estaEmXeque(vez),
                partida.estaEmXequeMate(vez),
                partida.estaEmAfogamento(vez),
                partida.getTabuleiro().serializar(),
                r.online(),
                r.branco(),
                r.preto(),
                r.resultado(),
                r.eloBranco(),
                r.eloPreto(),
                r.deltaBranco(),
                r.deltaPreto()
        );
    }
}
