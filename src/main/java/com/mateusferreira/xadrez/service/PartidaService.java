package com.mateusferreira.xadrez.service;

import com.mateusferreira.xadrez.dominio.Cor;
import com.mateusferreira.xadrez.dominio.Jogada;
import com.mateusferreira.xadrez.dominio.MotorIA;
import com.mateusferreira.xadrez.dominio.Partida;
import com.mateusferreira.xadrez.dominio.Peca;
import com.mateusferreira.xadrez.dominio.Posicao;
import com.mateusferreira.xadrez.dominio.Resultado;
import com.mateusferreira.xadrez.dominio.TipoPromocao;
import com.mateusferreira.xadrez.elo.CalculadoraElo;
import com.mateusferreira.xadrez.persistencia.PartidaEntity;
import com.mateusferreira.xadrez.persistencia.PartidaMapper;
import com.mateusferreira.xadrez.persistencia.PartidaRepository;
import com.mateusferreira.xadrez.seguranca.Usuario;
import com.mateusferreira.xadrez.seguranca.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Camada de SERVIÇO: orquestra os casos de uso e PERSISTE as partidas no banco.
 *
 * <p>Na Fase 4 ganhou duas responsabilidades novas: (1) vincular os usuários
 * logados às partidas ONLINE (brancas = quem cria, pretas = quem entra pelo
 * link) e (2) ao fim da partida, aplicar o Elo aos dois jogadores.
 */
@Service
public class PartidaService {

    private final PartidaRepository repository;
    private final UsuarioRepository usuarios;

    // Motor de IA (Java puro, sem estado): pode ser reutilizado entre chamadas.
    private final MotorIA motor = new MotorIA();

    public PartidaService(PartidaRepository repository, UsuarioRepository usuarios) {
        this.repository = repository;
        this.usuarios = usuarios;
    }

    /** Cria uma partida LOCAL (contra IA ou 2 jogadores no mesmo aparelho). */
    public ResultadoPartida novaPartida() {
        return novaPartida(false, null);
    }

    /**
     * Cria uma partida nova. Se {@code online} e houver um usuário logado, ele
     * fica com as BRANCAS (o adversário entra depois pelo link, via {@link #entrar}).
     */
    public ResultadoPartida novaPartida(boolean online, String branco) {
        Partida partida = Partida.nova();
        PartidaEntity entity = new PartidaEntity(
                partida.getTabuleiro().serializar(),
                partida.getJogadorAtual(),
                partida.direitosDeRoque(),
                serializarAlvo(partida));
        entity.setOnline(online);
        if (online) {
            entity.setBrancoUsuario(branco);
        }
        entity = repository.save(entity); // após salvar, a entidade tem id
        return montar(entity, partida);
    }

    /**
     * Um usuário logado ENTRA numa partida online pelo link e assume as PRETAS.
     * É idempotente: se já houver pretas, ou for o próprio criador, nada muda.
     */
    public ResultadoPartida entrar(Long id, String usuario) {
        PartidaEntity entity = buscar(id);
        Partida partida = PartidaMapper.paraDominio(entity);
        if (usuario != null
                && entity.isOnline()
                && entity.getPretoUsuario() == null
                && !usuario.equals(entity.getBrancoUsuario())) {
            entity.setPretoUsuario(usuario);
            repository.save(entity);
        }
        return montar(entity, partida);
    }

    /** Carrega o estado atual de uma partida pelo id. */
    public ResultadoPartida verPartida(Long id) {
        PartidaEntity entity = buscar(id);
        return montar(entity, PartidaMapper.paraDominio(entity));
    }

    /** Aplica uma jogada (promoção padrão Rainha). */
    public ResultadoPartida jogar(Long id, Posicao origem, Posicao destino) {
        return jogar(id, origem, destino, TipoPromocao.RAINHA);
    }

    /** Aplica uma jogada, persiste o novo estado e, se a partida acabou, pontua o Elo. */
    public ResultadoPartida jogar(Long id, Posicao origem, Posicao destino, TipoPromocao promocao) {
        PartidaEntity entity = buscar(id);

        // 1. banco -> domínio
        Partida partida = PartidaMapper.paraDominio(entity);
        // 2. aplica a regra (pode lançar MovimentoInvalidoException -> vira HTTP 400)
        partida.mover(origem, destino, promocao);
        // 3. o lance acabou de encerrar a partida? (mate/afogamento) -> resolve Elo
        resolverFim(entity, partida);
        // 4. domínio -> banco (salva também resultado/deltas/eloAplicado, na mesma entidade)
        salvar(entity, partida);

        return montar(entity, partida);
    }

    /**
     * Faz a IA jogar pelo jogador da vez (nível = profundidade da busca, 1–4).
     * Partidas contra a IA são locais e NÃO pontuam Elo.
     */
    public ResultadoPartida jogarIA(Long id, int nivel) {
        PartidaEntity entity = buscar(id);
        Partida partida = PartidaMapper.paraDominio(entity);

        int profundidade = Math.max(1, Math.min(nivel, 4));
        Optional<Jogada> jogada = motor.melhorJogada(partida, profundidade);
        if (jogada.isPresent()) {
            Jogada j = jogada.get();
            partida.mover(j.origem(), j.destino()); // IA promove para rainha (padrão)
            salvar(entity, partida);
        }
        return montar(entity, partida);
    }

    /**
     * Descobre se o lance recém-feito encerrou a partida e, em caso positivo,
     * registra o resultado e aplica o Elo. 'vez' é de quem jogaria AGORA — se ele
     * está em xeque-mate, quem venceu foi o OUTRO lado.
     */
    private void resolverFim(PartidaEntity entity, Partida partida) {
        Cor vez = partida.getJogadorAtual();
        Resultado resultado;
        if (partida.estaEmXequeMate(vez)) {
            resultado = (vez == Cor.BRANCO) ? Resultado.VITORIA_PRETO : Resultado.VITORIA_BRANCO;
        } else if (partida.estaEmAfogamento(vez)) {
            resultado = Resultado.EMPATE;
        } else {
            return; // partida continua
        }
        entity.setResultado(resultado);
        aplicarElo(entity, resultado);
    }

    /**
     * Aplica o Elo aos dois jogadores — SÓ para partidas online entre dois
     * usuários logados, e no máximo uma vez (trava 'eloAplicado').
     */
    private void aplicarElo(PartidaEntity entity, Resultado resultado) {
        if (!entity.isOnline() || entity.isEloAplicado()) {
            return;
        }
        String nomeBranco = entity.getBrancoUsuario();
        String nomePreto = entity.getPretoUsuario();
        if (nomeBranco == null || nomePreto == null) {
            return; // partida online sem os dois lados identificados: não pontua
        }
        Optional<Usuario> talvezBranco = usuarios.findByUsuario(nomeBranco);
        Optional<Usuario> talvezPreto = usuarios.findByUsuario(nomePreto);
        if (talvezBranco.isEmpty() || talvezPreto.isEmpty()) {
            return;
        }
        Usuario branco = talvezBranco.get();
        Usuario preto = talvezPreto.get();

        CalculadoraElo.Variacao v = CalculadoraElo.novosRatings(branco.getElo(), preto.getElo(), resultado);
        branco.setElo(v.eloBranco());
        preto.setElo(v.eloPreto());
        usuarios.save(branco);
        usuarios.save(preto);

        entity.setDeltaBranco(v.deltaBranco());
        entity.setDeltaPreto(v.deltaPreto());
        entity.setEloAplicado(true);
    }

    /** Persiste o estado atual do domínio na entidade (reaproveitado por jogar/jogarIA). */
    private void salvar(PartidaEntity entity, Partida partida) {
        entity.setTabuleiro(partida.getTabuleiro().serializar());
        entity.setJogadorAtual(partida.getJogadorAtual());
        entity.setRoque(partida.direitosDeRoque());
        entity.setEnPassant(serializarAlvo(partida));
        repository.save(entity);
    }

    /**
     * Lista os movimentos legais da peça que está em 'origem' — usado para
     * destacar os destinos no tabuleiro. Só devolve algo se a peça for da cor
     * de quem é a vez (não faz sentido destacar lances de peças do adversário).
     */
    public List<Posicao> movimentosLegais(Long id, Posicao origem) {
        PartidaEntity entity = buscar(id);
        Partida partida = PartidaMapper.paraDominio(entity);

        Peca peca = partida.getTabuleiro().pecaEm(origem);
        if (peca == null || peca.getCor() != partida.getJogadorAtual()) {
            return List.of();
        }
        return partida.movimentosLegais(origem);
    }

    /** Monta o "pacote" de resposta, buscando o Elo atual de cada jogador. */
    private ResultadoPartida montar(PartidaEntity e, Partida partida) {
        return new ResultadoPartida(
                e.getId(),
                partida,
                e.isOnline(),
                e.getBrancoUsuario(),
                e.getPretoUsuario(),
                e.getResultado(),
                eloDe(e.getBrancoUsuario()),
                eloDe(e.getPretoUsuario()),
                e.getDeltaBranco(),
                e.getDeltaPreto());
    }

    /** Elo atual de um usuário pelo nome, ou null (anônimo / não encontrado). */
    private Integer eloDe(String usuario) {
        if (usuario == null) {
            return null;
        }
        return usuarios.findByUsuario(usuario).map(Usuario::getElo).orElse(null);
    }

    /** Converte o alvo de en passant da partida em texto ("e3") ou null. */
    private String serializarAlvo(Partida partida) {
        return partida.getAlvoEnPassant() == null ? null : partida.getAlvoEnPassant().toString();
    }

    /** Busca a entidade ou lança 'não encontrada' (findById devolve um Optional). */
    private PartidaEntity buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PartidaNaoEncontradaException("Partida " + id + " não encontrada."));
    }
}
