import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { listarPartidasAbertas, novaPartida } from './api'
import { useAuth } from './auth'

// Largura da faixa "perto do meu Elo": pega adversários até 150 pontos acima/abaixo.
const RAIO_ELO = 150

/**
 * LOBBY online: lista as salas abertas (gente esperando oponente), com busca por
 * faixa de Elo. Entrar numa sala ou criar a sua leva de volta ao jogo (rota "/")
 * pela URL ?partida=&cor=, que o App já sabe abrir. A lista se atualiza sozinha.
 */
export default function PaginaLobby() {
  const { auth } = useAuth()
  const navigate = useNavigate()

  // Faixa de Elo do filtro. Começa centrada no MEU Elo (±RAIO), que é o caso de
  // uso mais comum: achar oponente do meu nível. String vazia = limite aberto.
  const [eloMin, setEloMin] = useState('')
  const [eloMax, setEloMax] = useState('')

  // Jogar online exige conta: sem login, manda para o /login.
  useEffect(() => {
    if (!auth) navigate('/login')
  }, [auth, navigate])

  // Preenche a faixa inicial assim que souber o Elo do usuário (só uma vez).
  useEffect(() => {
    if (auth) {
      setEloMin(String(Math.max(0, auth.elo - RAIO_ELO)))
      setEloMax(String(auth.elo + RAIO_ELO))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth?.usuario])

  // Converte os campos de texto em número (ou undefined = limite aberto).
  const min = eloMin.trim() === '' ? undefined : Number(eloMin)
  const max = eloMax.trim() === '' ? undefined : Number(eloMax)

  // Lista das salas: refetch a cada 3s para refletir quem entrou/criou/saiu.
  const salas = useQuery({
    queryKey: ['salas', min, max],
    queryFn: () => listarPartidasAbertas(min, max),
    enabled: !!auth,
    refetchInterval: 3000,
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

  function faixaPertoDeMim() {
    if (!auth) return
    setEloMin(String(Math.max(0, auth.elo - RAIO_ELO)))
    setEloMax(String(auth.elo + RAIO_ELO))
  }
  function faixaTodos() {
    setEloMin('')
    setEloMax('')
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

      <div className="lobby-box">
        <div className="lobby-cabecalho">
          <h2>Salas online</h2>
          <p className="auth-sub">Encontre um oponente do seu nível (Elo {meuElo}).</p>
        </div>

        {/* Filtro por faixa de Elo. Atalhos preenchem os campos; dá pra editar à mão. */}
        <div className="lobby-filtro">
          <div className="lobby-chips">
            <button onClick={faixaPertoDeMim}>Perto de mim (±{RAIO_ELO})</button>
            <button onClick={faixaTodos}>Todos os níveis</button>
          </div>
          <div className="lobby-faixa">
            <label>
              Elo de
              <input
                type="number"
                inputMode="numeric"
                placeholder="0"
                value={eloMin}
                onChange={(e) => setEloMin(e.target.value)}
              />
            </label>
            <label>
              até
              <input
                type="number"
                inputMode="numeric"
                placeholder="∞"
                value={eloMax}
                onChange={(e) => setEloMax(e.target.value)}
              />
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
                    <span className="sala-elo">Elo {s.elo}</span>
                  </div>
                </div>
                <button className="primario" onClick={() => entrarNaSala(s.id)}>
                  Entrar
                </button>
              </div>
            ))
          )}
        </div>

        <button className="primario grande" onClick={() => criar.mutate()} disabled={criar.isPending}>
          {criar.isPending ? 'Criando sala…' : '+ Criar sala'}
        </button>
      </div>
    </div>
  )
}
