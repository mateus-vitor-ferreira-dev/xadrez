package com.mateusferreira.xadrez.service;

import com.mateusferreira.xadrez.dominio.Posicao;
import com.mateusferreira.xadrez.dominio.Resultado;
import com.mateusferreira.xadrez.seguranca.Usuario;
import com.mateusferreira.xadrez.seguranca.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica, ponta a ponta (serviço + banco H2), que uma partida ONLINE entre
 * dois usuários aplica o Elo ao terminar. Usa o "mate do tolo": o mate mais
 * rápido do xadrez (1.f3 e5 2.g4 Qh4#), com as PRETAS vencendo.
 */
@SpringBootTest
class PartidaServiceEloTest {

    @Autowired
    PartidaService service;

    @Autowired
    UsuarioRepository usuarios;

    private void jogar(Long id, String origem, String destino) {
        service.jogar(id, Posicao.de(origem), Posicao.de(destino));
    }

    @Test
    void partidaOnline_aplicaEloAoTerminar() {
        // Dois jogadores começam com o mesmo rating padrão (1200).
        usuarios.save(new Usuario("branco_teste", "branco@teste.com", "hash"));
        usuarios.save(new Usuario("preto_teste", "preto@teste.com", "hash"));

        Long id = service.novaPartida(true, "branco_teste").id(); // criador = brancas
        service.entrar(id, "preto_teste"); // adversário = pretas

        jogar(id, "f2", "f3");
        jogar(id, "e7", "e5");
        jogar(id, "g2", "g4");
        var fim = service.jogar(id, Posicao.de("d8"), Posicao.de("h4")); // Qh4# — pretas vencem

        assertThat(fim.partida().estaEmXequeMate(fim.partida().getJogadorAtual())).isTrue();
        assertThat(fim.resultado()).isEqualTo(Resultado.VITORIA_PRETO);

        // Ratings iguais => vencedor +16, perdedor -16.
        assertThat(usuarios.findByUsuario("preto_teste").orElseThrow().getElo()).isEqualTo(1216);
        assertThat(usuarios.findByUsuario("branco_teste").orElseThrow().getElo()).isEqualTo(1184);
        assertThat(fim.deltaPreto()).isEqualTo(16);
        assertThat(fim.deltaBranco()).isEqualTo(-16);
    }

    @Test
    void partidaContraIA_naoMexeNoElo() {
        usuarios.save(new Usuario("solo_teste", "solo@teste.com", "hash"));
        int antes = usuarios.findByUsuario("solo_teste").orElseThrow().getElo();

        // Partida LOCAL (não-online): mesmo terminando, não pontua.
        Long id = service.novaPartida().id();
        jogar(id, "f2", "f3");
        jogar(id, "e7", "e5");
        jogar(id, "g2", "g4");
        var fim = service.jogar(id, Posicao.de("d8"), Posicao.de("h4"));

        assertThat(fim.resultado()).isEqualTo(Resultado.VITORIA_PRETO);
        assertThat(fim.deltaBranco()).isNull();
        assertThat(usuarios.findByUsuario("solo_teste").orElseThrow().getElo()).isEqualTo(antes);
    }
}
