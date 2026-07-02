import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { buscarRanking, listarPartidasAbertas, novaPartida } from '../lib/api'
import { useAuth } from '../contexts/auth'
import { RANKS, eloMaxDoRank, indiceDoRank } from '../themes/ranks'
import TabelaRanking from '../components/TabelaRanking'

/**
 * LOBBY online: lista as salas abertas (gente esperando oponente), com busca por
 * faixa de Elo. Entrar numa sala ou criar a sua leva de volta ao jogo (rota "/")
 * pela URL ?partida=&cor=, que o App já sabe abrir. A lista se atualiza sozinha.
 */
export default function PaginaLobby() {
  const { auth } = useAuth()
  const navigate = useNavigate()

  // Filtro por FAIXA de rank (não Elo solto): o jogador escolhe a faixa mínima e a
  // máxima (Iniciante…Grande Mestre) e nós convertemos nos limites de Elo. Guardamos
  // os ÍNDICES em RANKS. Começa na minha própria faixa — o caso de uso mais comum.
  const [rankMin, setRankMin] = useState(0)
  const [rankMax, setRankMax] = useState(RANKS.length - 1)

  // Jogar online exige conta: sem login, manda para o /login.
  useEffect(() => {
    if (!auth) navigate('/login')
  }, [auth, navigate])

  // Centraliza o filtro na minha faixa assim que souber o Elo do usuário.
  useEffect(() => {
    if (auth) {
      const meuRank = indiceDoRank(auth.elo)
      setRankMin(meuRank)
      setRankMax(meuRank)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth?.usuario])

  // Converte a faixa escolhida nos limites de Elo que a API entende: piso da faixa
  // mínima e teto da faixa máxima (undefined = aberto pra cima, no Grande Mestre).
  const min = RANKS[rankMin].eloMin
  const max = eloMaxDoRank(rankMax)

  // Lista das salas: refetch a cada 3s para refletir quem entrou/criou/saiu.
  const salas = useQuery({
    queryKey: ['salas', min, max],
    queryFn: () => listarPartidasAbertas(min, max),
    enabled: !!auth,
    refetchInterval: 3000,
  })

  // Ranking das laterais: as duas tabelas vêm juntas do /ranking. Atualiza a cada
  // 10s (muda mais devagar que as salas) e depende do usuário na queryKey, para
  // recarregar a faixa "do meu rank" ao trocar de conta.
  const ranking = useQuery({
    queryKey: ['ranking', auth?.usuario],
    queryFn: buscarRanking,
    enabled: !!auth,
    refetchInterval: 10000,
  })

  // Criar sala: nasce como partida online (eu = brancas) e vou para o jogo,
  // onde espero o oponente. A sala aparece no lobby dos outros na hora.
  const criar = useMutation({
    mutationFn: () => novaPartida(true),
    onSuccess: (e) => navigate(`/?partida=${e.id}&cor=branco`),
  })

  // Entrar numa sala: assumo as pretas (o App chama /entrar ao abrir a URL).
  function entrarNaSala(id: number) {
    navigate(`/?partida=${id}&cor=preto`)
  }

  // Ajusta uma ponta do intervalo mantendo min <= max (mexer numa arrasta a outra).
  function mudarRankMin(i: number) {
    setRankMin(i)
    if (i > rankMax) setRankMax(i)
  }
  function mudarRankMax(i: number) {
    setRankMax(i)
    if (i < rankMin) setRankMin(i)
  }

  function faixaMeuNivel() {
    if (!auth) return
    const meuRank = indiceDoRank(auth.elo)
    setRankMin(meuRank)
    setRankMax(meuRank)
  }
  function faixaTodos() {
    setRankMin(0)
    setRankMax(RANKS.length - 1)
  }

  const meuElo = auth?.elo ?? 0

  // Ordena por proximidade do meu Elo (adversários mais parecidos primeiro).
  // Depende de salas.data (referência estável do React Query), não de um array
  // recriado a cada render — assim o memo realmente memoiza.
  const ordenadas = useMemo(
    () => [...(salas.data ?? [])].sort((a, b) => Math.abs(a.elo - meuElo) - Math.abs(b.elo - meuElo)),
    [salas.data, meuElo],
  )

  if (!auth) return null // enquanto o efeito acima redireciona

  return (
    <div className="lobby-page">
      <Link to="/" className="voltar-auth">
        ← Voltar ao jogo
      </Link>

      {/* 3 colunas: top do site (esq.) | salas (centro) | top do meu rank (dir.).
          As tabelas ficam nas laterais em telas largas e empilham no mobile (CSS). */}
      <div className="lobby-3col">
        <TabelaRanking
          titulo="🏆 Top do site"
          subtitulo="Maiores pontuadores"
          linhas={ranking.data?.topSite ?? []}
          destaque={auth.usuario}
        />

        <div className="lobby-box">
        <div className="lobby-cabecalho">
          <h2>Salas online</h2>
          <p className="auth-sub">Encontre um oponente do seu nível (Elo {meuElo}).</p>
        </div>

        {/* Filtro por FAIXA de rank. Atalhos ajustam os dois seletores; dá pra
            escolher a faixa mínima e a máxima à mão. */}
        <div className="lobby-filtro">
          <div className="lobby-chips">
            <button onClick={faixaMeuNivel}>Meu nível</button>
            <button onClick={faixaTodos}>Todos os níveis</button>
          </div>
          <div className="lobby-faixa">
            <label>
              De
              <select value={rankMin} onChange={(e) => mudarRankMin(Number(e.target.value))}>
                {RANKS.map((r, i) => (
                  <option key={r.id} value={i}>
                    {r.rotulo}
                  </option>
                ))}
              </select>
            </label>
            <label>
              até
              <select value={rankMax} onChange={(e) => mudarRankMax(Number(e.target.value))}>
                {RANKS.map((r, i) => (
                  <option key={r.id} value={i}>
                    {r.rotulo}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>

        {/* Lista de salas */}
        <div className="lobby-lista">
          {salas.isLoading ? (
            <p className="lobby-vazio">Carregando salas…</p>
          ) : salas.isError ? (
            <p className="status alerta">Não foi possível carregar as salas.</p>
          ) : ordenadas.length === 0 ? (
            <p className="lobby-vazio">Nenhuma sala aberta nesta faixa. Que tal criar a sua?</p>
          ) : (
            ordenadas.map((s) => (
              <div key={s.id} className="sala">
                <div className="sala-jogador">
                  <span className="sala-inicial">{s.criador.charAt(0).toUpperCase()}</span>
                  <div className="sala-nome">
                    <strong>{s.criador}</strong>
                    <span className="sala-status">🟢 Aguardando oponente</span>
                  </div>
                </div>
                <div className="sala-acao">
                  <span className="sala-elo-badge">Elo {s.elo}</span>
                  <button className="primario" onClick={() => entrarNaSala(s.id)}>
                    Entrar
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <button className="primario grande" onClick={() => criar.mutate()} disabled={criar.isPending}>
          {criar.isPending ? 'Criando sala…' : '+ Criar sala'}
        </button>
      </div>

        <TabelaRanking
          titulo="⭐ Seu rank"
          subtitulo={ranking.data ? `${ranking.data.meuRank} · Elo ${ranking.data.meuElo}` : undefined}
          linhas={ranking.data?.topRank ?? []}
          destaque={auth.usuario}
        />
      </div>
    </div>
  )
}
