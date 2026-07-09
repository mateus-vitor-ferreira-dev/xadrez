package com.mateusferreira.xadrez.persistencia;

import com.mateusferreira.xadrez.dominio.Partida;
import com.mateusferreira.xadrez.dominio.Posicao;
import com.mateusferreira.xadrez.dominio.Tabuleiro;

/**
 * Tradutor entre o modelo de PERSISTÊNCIA (PartidaEntity) e o de DOMÍNIO
 * (Partida). É aqui — e só aqui — que as duas representações se conhecem,
 * mantendo o domínio sem qualquer dependência de JPA.
 */
public class PartidaMapper {

    private PartidaMapper() {
        // classe utilitária: só métodos estáticos, não se instancia.
    }

    /** Banco -> domínio: reconstrói uma Partida jogável a partir da entidade. */
    public static Partida paraDominio(PartidaEntity entity) {
        Tabuleiro tabuleiro = Tabuleiro.deTexto(entity.getTabuleiro());
        String roque = entity.getRoque();
        // Reconstrói o alvo de en passant (coluna null = sem alvo).
        String ep = entity.getEnPassant();
        Posicao alvoEnPassant = (ep == null || ep.isBlank()) ? null : Posicao.de(ep);
        // Reconstrói os 4 direitos de roque a partir do código "KQkq".
        return new Partida(
                tabuleiro,
                entity.getJogadorAtual(),
                roque.indexOf('K') >= 0,
                roque.indexOf('Q') >= 0,
                roque.indexOf('k') >= 0,
                roque.indexOf('q') >= 0,
                alvoEnPassant);
    }
}
