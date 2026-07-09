package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.RankingResponse;
import com.mateusferreira.xadrez.dominio.Rank;
import com.mateusferreira.xadrez.seguranca.Usuario;
import com.mateusferreira.xadrez.seguranca.UsuarioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Ranking (leaderboards). Um único GET /ranking devolve as duas tabelas:
 * top do site e top da faixa do usuário logado. Continua "fino": só usa o
 * repositório e o enum {@link Rank} do domínio.
 */
@RestController
@RequestMapping("/ranking")
public class RankingController {

    /** Elo de quem não está logado: o mesmo com que toda conta nasce (Intermediário). */
    private static final int ELO_PADRAO = 1200;

    private final UsuarioRepository repository;

    public RankingController(UsuarioRepository repository) {
        this.repository = repository;
    }

    /**
     * GET /ranking — as duas tabelas de uma vez.
     *
     * <p>A faixa da direita vem do Elo do usuário logado; sem login, cai no
     * {@link #ELO_PADRAO} (a faixa inicial), para a tela nunca ficar sem a 2ª tabela.
     */
    @GetMapping
    public RankingResponse ranking(Principal principal) {
        int meuElo = eloDoUsuario(principal);
        Rank meuRank = Rank.de(meuElo);

        // Esquerda: os maiores do site, sem filtro de faixa.
        List<RankingResponse.Linha> topSite = paraLinhas(repository.findTop10ByOrderByEloDesc());

        // Direita: os maiores DENTRO da minha faixa [piso, teto].
        List<RankingResponse.Linha> topRank = paraLinhas(
                repository.findTop10ByEloBetweenOrderByEloDesc(meuRank.getEloMinimo(), meuRank.getEloMaximo()));

        return new RankingResponse(meuRank.getRotulo(), meuElo, topSite, topRank);
    }

    /** Elo atual do usuário logado (buscado no banco, sempre fresco), ou o padrão se anônimo. */
    private int eloDoUsuario(Principal principal) {
        if (principal == null || "anonymousUser".equals(principal.getName())) {
            return ELO_PADRAO;
        }
        return repository.findByUsuario(principal.getName())
                .map(Usuario::getElo)
                .orElse(ELO_PADRAO);
    }

    /** Converte entidades em linhas de tabela, com o rótulo do rank e o título equipado. */
    private List<RankingResponse.Linha> paraLinhas(List<Usuario> usuarios) {
        return usuarios.stream()
                .map(u -> new RankingResponse.Linha(
                        u.getUsuario(),
                        u.getElo(),
                        Rank.de(u.getElo()).getRotulo(),
                        u.getTituloEquipado() == null ? null : u.getTituloEquipado().getRotulo()))
                .toList();
    }
}
