package com.mateusferreira.xadrez.persistencia;

import com.mateusferreira.xadrez.dominio.Resultado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositório da PartidaEntity — a porta de acesso ao banco.
 *
 * "Mágica" do Spring Data JPA: NÃO escrevemos implementação. Só de estender
 * JpaRepository<PartidaEntity, Long>, o Spring gera automaticamente, em tempo
 * de execução, uma classe com os métodos prontos:
 *   save(entity)      -> INSERT/UPDATE
 *   findById(id)      -> SELECT por id (devolve Optional)
 *   findAll()         -> SELECT *
 *   deleteById(id)    -> DELETE
 *   ... e vários outros.
 *
 * Os dois tipos genéricos dizem: <a Entidade gerenciada, o tipo da chave (@Id)>.
 *
 * Mais pra frente poderíamos adicionar consultas só declarando o método
 * (ex.: List<PartidaEntity> findByJogadorAtual(Cor cor);) — o Spring entende
 * pelo nome e cria a query. Por ora, os métodos herdados já bastam.
 */
public interface PartidaRepository extends JpaRepository<PartidaEntity, Long> {

    /**
     * SALAS ABERTAS do lobby: partidas online que ainda esperam oponente
     * (pretas vazias) e que não terminaram. O Spring Data lê o próprio nome do
     * método e monta a query: online=true AND pretoUsuario IS NULL AND
     * resultado=? — ordenadas da mais recente para a mais antiga (id desc).
     */
    List<PartidaEntity> findByOnlineTrueAndPretoUsuarioIsNullAndResultadoOrderByIdDesc(Resultado resultado);
}
