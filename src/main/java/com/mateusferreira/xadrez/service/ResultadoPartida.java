package com.mateusferreira.xadrez.service;

import com.mateusferreira.xadrez.dominio.Partida;
import com.mateusferreira.xadrez.dominio.Resultado;

/**
 * "Pacote" devolvido pelo serviço: junta o id (do banco) e a Partida do domínio
 * com os dados de partida online / Elo (Fase 4). Assim o controller monta a
 * resposta HTTP sem nunca tocar em entidade ou banco.
 *
 * <p>Os campos de Elo (elos e deltas) ficam {@code null} enquanto a partida não
 * termina — e sempre que não for uma partida online entre dois usuários.
 */
public record ResultadoPartida(
        Long id,
        Partida partida,
        boolean online,
        String branco,
        String preto,
        Resultado resultado,
        Integer eloBranco,
        Integer eloPreto,
        Integer deltaBranco,
        Integer deltaPreto) {
}
