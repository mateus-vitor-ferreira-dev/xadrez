package com.mateusferreira.xadrez.service;

/**
 * "Sala aberta" do lobby: uma partida online esperando oponente. Só carrega o
 * essencial para a lista — o id (para entrar), o apelido de quem criou e o Elo
 * dele (para o jogador escolher um adversário do seu nível). Quem cria joga de
 * brancas; quem entra pela sala assume as pretas.
 */
public record PartidaAberta(Long id, String criador, int elo) {
}
