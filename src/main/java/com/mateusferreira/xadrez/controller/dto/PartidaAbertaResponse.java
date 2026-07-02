package com.mateusferreira.xadrez.controller.dto;

import com.mateusferreira.xadrez.service.PartidaAberta;

/**
 * DTO de SAÍDA de uma sala aberta do lobby. Espelha {@link PartidaAberta} do
 * serviço, mantendo o controller sem depender direto de tipos internos.
 */
public record PartidaAbertaResponse(Long id, String criador, int elo, String titulo) {
    public static PartidaAbertaResponse de(PartidaAberta p) {
        return new PartidaAbertaResponse(p.id(), p.criador(), p.elo(), p.titulo());
    }
}
