package com.mateusferreira.xadrez.service;

import com.mateusferreira.xadrez.dominio.Partida;

/**
 * Pequeno "pacote" devolvido pelo serviço: junta o id (do banco) com a Partida
 * já reconstruída (do domínio). Assim o controller recebe tudo o que precisa
 * para montar a resposta, sem nunca tocar em entidade ou banco.
 */
public record ResultadoPartida(Long id, Partida partida) {
}
