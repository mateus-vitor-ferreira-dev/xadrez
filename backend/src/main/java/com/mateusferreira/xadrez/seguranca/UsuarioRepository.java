package com.mateusferreira.xadrez.seguranca;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuario(String usuario);

    Optional<Usuario> findByEmail(String email);

    boolean existsByUsuario(String usuario);

    boolean existsByEmail(String email);

    // ----- Ranking (leaderboards) -----
    // O Spring Data gera o SQL a partir do NOME do método: nada de query manual.

    /** Os 10 maiores Elos do site inteiro (tabela da esquerda). */
    List<Usuario> findTop10ByOrderByEloDesc();

    /** Os 10 maiores Elos DENTRO de uma faixa [min, max] (tabela da direita, "seu rank"). */
    List<Usuario> findTop10ByEloBetweenOrderByEloDesc(int eloMin, int eloMax);
}
