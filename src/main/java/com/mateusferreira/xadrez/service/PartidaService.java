package com.mateusferreira.xadrez.service;

import com.mateusferreira.xadrez.dominio.Jogada;
import com.mateusferreira.xadrez.dominio.MotorIA;
import com.mateusferreira.xadrez.dominio.Partida;
import com.mateusferreira.xadrez.dominio.Peca;
import com.mateusferreira.xadrez.dominio.Posicao;
import com.mateusferreira.xadrez.dominio.TipoPromocao;
import com.mateusferreira.xadrez.persistencia.PartidaEntity;
import com.mateusferreira.xadrez.persistencia.PartidaMapper;
import com.mateusferreira.xadrez.persistencia.PartidaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Camada de SERVIÇO: orquestra os casos de uso e agora PERSISTE as partidas no
 * banco através do PartidaRepository (injetado por construtor).
 *
 * Não guarda mais nada em memória: cada operação carrega a partida do banco,
 * age sobre ela e salva de volta. Isso já nos dá VÁRIAS partidas simultâneas,
 * identificadas por id, e que sobrevivem entre requisições.
 */
@Service
public class PartidaService {

    private final PartidaRepository repository;

    // Motor de IA (Java puro, sem estado): pode ser reutilizado entre chamadas.
    private final MotorIA motor = new MotorIA();

    public PartidaService(PartidaRepository repository) {
        this.repository = repository;
    }

    /** Cria uma partida nova, salva no banco e devolve (id + domínio). */
    public ResultadoPartida novaPartida() {
        Partida partida = Partida.nova();
        PartidaEntity entity = new PartidaEntity(
                partida.getTabuleiro().serializar(),
                partida.getJogadorAtual(),
                partida.direitosDeRoque(),
                serializarAlvo(partida));
        entity = repository.save(entity); // após salvar, a entidade tem id
        return new ResultadoPartida(entity.getId(), partida);
    }

    /** Carrega o estado atual de uma partida pelo id. */
    public ResultadoPartida verPartida(Long id) {
        PartidaEntity entity = buscar(id);
        return new ResultadoPartida(entity.getId(), PartidaMapper.paraDominio(entity));
    }

    /** Aplica uma jogada (promoção padrão Rainha). */
    public ResultadoPartida jogar(Long id, Posicao origem, Posicao destino) {
        return jogar(id, origem, destino, TipoPromocao.RAINHA);
    }

    /** Aplica uma jogada numa partida e persiste o novo estado. */
    public ResultadoPartida jogar(Long id, Posicao origem, Posicao destino, TipoPromocao promocao) {
        PartidaEntity entity = buscar(id);

        // 1. banco -> domínio
        Partida partida = PartidaMapper.paraDominio(entity);
        // 2. aplica a regra (pode lançar MovimentoInvalidoException -> vira HTTP 400)
        partida.mover(origem, destino, promocao);
        // 3. domínio -> banco
        salvar(entity, partida);

        return new ResultadoPartida(entity.getId(), partida);
    }

    /**
     * Faz a IA jogar pelo jogador da vez (nível = profundidade da busca, 1–4).
     * Se não houver lance possível (fim de jogo), apenas devolve o estado atual.
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
        return new ResultadoPartida(entity.getId(), partida);
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
