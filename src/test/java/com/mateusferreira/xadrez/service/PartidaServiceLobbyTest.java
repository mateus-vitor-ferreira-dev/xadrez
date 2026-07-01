package com.mateusferreira.xadrez.service;

import com.mateusferreira.xadrez.seguranca.Usuario;
import com.mateusferreira.xadrez.seguranca.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do LOBBY (serviço + banco H2): a listagem de salas abertas
 * ({@link PartidaService#partidasAbertas}). Foca nas regras que a query e o
 * filtro impõem — faixa de Elo, esconder as salas do próprio jogador e sumir
 * quando a sala já tem oponente.
 *
 * <p>As asserções miram salas ESPECÍFICAS (por id/criador) em vez do tamanho
 * da lista, porque o H2 é compartilhado entre as classes de teste e pode conter
 * salas de outros cenários.
 */
@SpringBootTest
class PartidaServiceLobbyTest {

    @Autowired
    PartidaService service;

    @Autowired
    UsuarioRepository usuarios;

    /** Cria um usuário com um Elo específico (o construtor nasce com 1200). */
    private void criarUsuario(String apelido, int elo) {
        Usuario u = new Usuario(apelido, apelido + "@teste.com", "hash");
        u.setElo(elo);
        usuarios.save(u);
    }

    /** Abre uma sala online (o criador joga de brancas) e devolve o id gerado. */
    private Long abrirSala(String criador) {
        return service.novaPartida(true, criador).id();
    }

    /** true se a lista contém uma sala com aquele id. */
    private boolean contemSala(List<PartidaAberta> lista, Long id) {
        return lista.stream().anyMatch(s -> s.id().equals(id));
    }

    @Test
    void listaSalaAbertaComCriadorEElo() {
        criarUsuario("lobby_ana", 1250);
        Long sala = abrirSala("lobby_ana");

        // Consultada por outro jogador, sem faixa: a sala da Ana aparece completa.
        List<PartidaAberta> abertas = service.partidasAbertas("lobby_outro", null, null);

        PartidaAberta daAna = abertas.stream()
                .filter(s -> s.id().equals(sala))
                .findFirst().orElseThrow();
        assertThat(daAna.criador()).isEqualTo("lobby_ana");
        assertThat(daAna.elo()).isEqualTo(1250);
    }

    @Test
    void escondeAsSalasDoProprioJogador() {
        criarUsuario("lobby_eu", 1200);
        Long minhaSala = abrirSala("lobby_eu");

        // Não faz sentido entrar na própria sala: ela some da MINHA lista...
        assertThat(contemSala(service.partidasAbertas("lobby_eu", null, null), minhaSala)).isFalse();
        // ...mas continua visível para os outros.
        assertThat(contemSala(service.partidasAbertas("lobby_outro", null, null), minhaSala)).isTrue();
    }

    @Test
    void filtraPelaFaixaDeElo() {
        criarUsuario("lobby_baixo", 1000);
        criarUsuario("lobby_meio", 1200);
        criarUsuario("lobby_alto", 1400);
        Long salaBaixo = abrirSala("lobby_baixo");
        Long salaMeio = abrirSala("lobby_meio");
        Long salaAlto = abrirSala("lobby_alto");

        // Faixa [1100, 1300]: só o do meio passa; os extremos ficam de fora.
        List<PartidaAberta> naFaixa = service.partidasAbertas("lobby_outro", 1100, 1300);
        assertThat(contemSala(naFaixa, salaMeio)).isTrue();
        assertThat(contemSala(naFaixa, salaBaixo)).isFalse();
        assertThat(contemSala(naFaixa, salaAlto)).isFalse();

        // Limites null = abertos: piso 1300 sem teto pega o alto, não o meio/baixo.
        List<PartidaAberta> soAltos = service.partidasAbertas("lobby_outro", 1300, null);
        assertThat(contemSala(soAltos, salaAlto)).isTrue();
        assertThat(contemSala(soAltos, salaMeio)).isFalse();
    }

    @Test
    void salaComOponenteSaiDaLista() {
        criarUsuario("lobby_host", 1200);
        criarUsuario("lobby_rival", 1200);
        Long sala = abrirSala("lobby_host");

        // Enquanto espera, aparece no lobby.
        assertThat(contemSala(service.partidasAbertas("lobby_outro", null, null), sala)).isTrue();

        // Assim que o rival entra (assume as pretas), a sala deixa de estar aberta.
        service.entrar(sala, "lobby_rival");
        assertThat(contemSala(service.partidasAbertas("lobby_outro", null, null), sala)).isFalse();
    }
}
