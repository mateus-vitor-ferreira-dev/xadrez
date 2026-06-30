import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Client } from '@stomp/stompjs'
import {
  buscarMovimentos,
  buscarPartida,
  jogadaIA,
  jogar,
  novaPartida,
  type Cor,
  type EstadoPartida,
  type TipoPromocao,
} from './api'
import Tabuleiro from './Tabuleiro'

const GLIFO_PROMOCAO: Record<TipoPromocao, string> = {
  RAINHA: '♛',
  TORRE: '♜',
  BISPO: '♝',
  CAVALO: '♞',
}

type Modo = 'humano' | 'ia'

/** URL do WebSocket: deriva da API (https->wss) ou cai no backend local. */
function wsUrl(): string {
  const api = import.meta.env.VITE_API_URL
  return api ? `${api.replace(/^http/, 'ws')}/ws` : 'ws://localhost:8080/ws'
}

/** Descobre o lance (origem/destino) comparando o tabuleiro antes e depois. */
function diffLance(antes: string, depois: string): { origem: string; destino: string } | null {
  let origem = -1
  let destino = -1
  for (let i = 0; i < 64; i++) {
    if (antes[i] !== depois[i]) {
      if (depois[i] === '.') origem = i
      else destino = i
    }
  }
  if (origem < 0 || destino < 0) return null
  const not = (i: number) => String.fromCharCode(97 + (i % 8)) + (Math.floor(i / 8) + 1)
  return { origem: not(origem), destino: not(destino) }
}

function App() {
  const queryClient = useQueryClient()
  const [idPartida, setIdPartida] = useState<number | null>(null)
  const [selecionada, setSelecionada] = useState<string | null>(null)
  const [ultimoLance, setUltimoLance] = useState<{ origem: string; destino: string } | null>(null)
  const [promocaoPendente, setPromocaoPendente] = useState<{ origem: string; destino: string } | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [modo, setModo] = useState<Modo>('humano')
  const [nivel, setNivel] = useState(2)
  // Online: a cor que ESTE cliente controla (null = jogo local).
  const [minhaCor, setMinhaCor] = useState<Cor | null>(null)
  const [conectado, setConectado] = useState(false)

  const partida = useQuery({
    queryKey: ['partida', idPartida],
    queryFn: () => buscarPartida(idPartida!),
    enabled: idPartida !== null,
  })
  const estado = partida.data

  const movimentos = useQuery({
    queryKey: ['movimentos', idPartida, selecionada],
    queryFn: () => buscarMovimentos(idPartida!, selecionada!),
    enabled: idPartida !== null && selecionada !== null,
  })
  const destaques = movimentos.data ?? []

  const fimDeJogo = (e?: EstadoPartida) => !!e && (e.xequeMate || e.afogamento)

  // --- Ao abrir com ?partida=ID na URL, entra na partida online ---
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const pid = params.get('partida')
    if (pid) {
      setIdPartida(Number(pid))
      setMinhaCor(params.get('cor') === 'branco' ? 'BRANCO' : 'PRETO')
    }
  }, [])

  // --- Conexão WebSocket (STOMP): recebe os lances em tempo real ---
  const clienteRef = useRef<Client | null>(null)
  useEffect(() => {
    if (idPartida === null || minhaCor === null) return

    const cliente = new Client({
      brokerURL: wsUrl(),
      reconnectDelay: 3000,
      onConnect: () => {
        setConectado(true)
        cliente.subscribe(`/topic/partidas/${idPartida}`, (msg) => {
          const novo = JSON.parse(msg.body) as EstadoPartida
          const antes = queryClient.getQueryData<EstadoPartida>(['partida', idPartida])
          queryClient.setQueryData(['partida', idPartida], novo)
          if (antes && antes.tabuleiro !== novo.tabuleiro) {
            setUltimoLance(diffLance(antes.tabuleiro, novo.tabuleiro))
          }
        })
      },
      onWebSocketClose: () => setConectado(false),
    })
    cliente.activate()
    clienteRef.current = cliente
    return () => {
      cliente.deactivate()
      setConectado(false)
    }
  }, [idPartida, minhaCor, queryClient])

  // --- Mutações ---
  const iaMutation = useMutation({
    mutationFn: () => jogadaIA(idPartida!, nivel),
    onSuccess: (depois) => {
      const antes = queryClient.getQueryData<EstadoPartida>(['partida', idPartida])
      queryClient.setQueryData(['partida', idPartida], depois)
      if (antes) setUltimoLance(diffLance(antes.tabuleiro, depois.tabuleiro))
    },
  })

  const criar = useMutation({
    mutationFn: (_online: boolean) => novaPartida(),
    onSuccess: (e, online) => {
      setIdPartida(e.id)
      queryClient.setQueryData(['partida', e.id], e)
      setSelecionada(null)
      setUltimoLance(null)
      setPromocaoPendente(null)
      setErro(null)
      if (online) {
        setMinhaCor('BRANCO')
        window.history.replaceState(null, '', `?partida=${e.id}&cor=branco`)
      } else {
        setMinhaCor(null)
        window.history.replaceState(null, '', window.location.pathname)
      }
    },
  })

  const fazerJogada = useMutation({
    mutationFn: (j: { origem: string; destino: string; promocao?: TipoPromocao }) =>
      jogar(idPartida!, j.origem, j.destino, j.promocao),
    onSuccess: (e, vars) => {
      queryClient.setQueryData(['partida', idPartida], e)
      setUltimoLance({ origem: vars.origem, destino: vars.destino })
      setErro(null)
      // No modo vs IA (local), a IA responde pelas pretas.
      if (modo === 'ia' && minhaCor === null && e.vez === 'PRETO' && !fimDeJogo(e)) {
        iaMutation.mutate()
      }
    },
    onError: (e) => setErro(e instanceof Error ? e.message : 'Jogada inválida'),
  })

  // --- Helpers ---
  function pecaEm(notacao: string): string {
    if (!estado) return '.'
    const coluna = notacao.charCodeAt(0) - 97
    const linha = Number(notacao[1]) - 1
    return estado.tabuleiro[linha * 8 + coluna]
  }
  function temPecaDaVez(notacao: string): boolean {
    if (!estado) return false
    const ch = pecaEm(notacao)
    if (ch === '.') return false
    const branca = ch === ch.toUpperCase()
    return (branca && estado.vez === 'BRANCO') || (!branca && estado.vez === 'PRETO')
  }
  function ehPromocao(origem: string, destino: string): boolean {
    if (pecaEm(origem).toLowerCase() !== 'p') return false
    const fileira = Number(destino[1])
    return fileira === 8 || fileira === 1
  }
  function casaDoReiEmXeque(): string | null {
    if (!estado || !estado.xeque) return null
    const rei = estado.vez === 'BRANCO' ? 'R' : 'r'
    const idx = estado.tabuleiro.indexOf(rei)
    if (idx < 0) return null
    return String.fromCharCode(97 + (idx % 8)) + (Math.floor(idx / 8) + 1)
  }

  const vezDaIA = modo === 'ia' && minhaCor === null && estado?.vez === 'PRETO'
  // No online, só posso agir na minha vez.
  const naoEhMinhaVez = minhaCor !== null && estado?.vez !== minhaCor

  function clicarCasa(notacao: string) {
    setErro(null)
    if (promocaoPendente || vezDaIA || iaMutation.isPending || naoEhMinhaVez) return
    if (selecionada === null) {
      if (temPecaDaVez(notacao)) setSelecionada(notacao)
    } else if (selecionada === notacao) {
      setSelecionada(null)
    } else {
      if (destaques.includes(notacao)) {
        if (ehPromocao(selecionada, notacao)) {
          setPromocaoPendente({ origem: selecionada, destino: notacao })
        } else {
          fazerJogada.mutate({ origem: selecionada, destino: notacao })
        }
      }
      setSelecionada(null)
    }
  }

  function escolherPromocao(tipo: TipoPromocao) {
    if (!promocaoPendente) return
    fazerJogada.mutate({ ...promocaoPendente, promocao: tipo })
    setPromocaoPendente(null)
  }

  const linkConvite =
    idPartida && minhaCor === 'BRANCO'
      ? `${window.location.origin}${window.location.pathname}?partida=${idPartida}&cor=preto`
      : null

  return (
    <div className="app">
      <h1>♟ Xadrez</h1>
      <p className="tagline">Local · contra a IA · online em tempo real</p>

      {/* Controles: local (modo/IA) ou info da partida online */}
      {minhaCor === null ? (
        <>
          <div className="config">
            <span>Modo:</span>
            <button className={modo === 'humano' ? 'ativo' : ''} onClick={() => setModo('humano')}>
              2 jogadores
            </button>
            <button className={modo === 'ia' ? 'ativo' : ''} onClick={() => setModo('ia')}>
              vs IA
            </button>
            {modo === 'ia' && (
              <span className="niveis">
                Nível:
                {[1, 2, 3].map((n) => (
                  <button key={n} className={nivel === n ? 'ativo' : ''} onClick={() => setNivel(n)}>
                    {n}
                  </button>
                ))}
              </span>
            )}
          </div>
          <div className="config">
            <button onClick={() => criar.mutate(false)} disabled={criar.isPending}>
              {criar.isPending ? 'Criando…' : 'Nova partida'}
            </button>
            <button onClick={() => criar.mutate(true)} disabled={criar.isPending}>
              Criar partida online
            </button>
          </div>
        </>
      ) : (
        <div className="online-info">
          <p>
            Partida online — você joga de <strong>{minhaCor}</strong>{' '}
            <span className={conectado ? 'ok' : 'off'}>{conectado ? '🟢 tempo real' : '🔌 conectando…'}</span>
          </p>
          {linkConvite && (
            <p className="convite">
              Convide o oponente (joga de PRETO):
              <input readOnly value={linkConvite} onFocus={(e) => e.currentTarget.select()} />
            </p>
          )}
        </div>
      )}

      {estado && (
        <>
          <p className="status">
            Vez das <strong>{estado.vez}</strong>
            {iaMutation.isPending && <span> — IA pensando…</span>}
            {naoEhMinhaVez && !fimDeJogo(estado) && <span> — aguardando o oponente…</span>}
            {!estado.xequeMate && estado.xeque && <span className="alerta"> — xeque!</span>}
          </p>

          <Tabuleiro
            tabuleiro={estado.tabuleiro}
            selecionada={selecionada}
            destaques={destaques}
            casaXeque={casaDoReiEmXeque()}
            ultimoLance={ultimoLance}
            onClicarCasa={clicarCasa}
          />

          {erro && <p className="status alerta">{erro}</p>}

          {promocaoPendente && (
            <div className="modal-promocao">
              <p>Promover para:</p>
              <div className="opcoes">
                {(Object.keys(GLIFO_PROMOCAO) as TipoPromocao[]).map((tipo) => (
                  <button key={tipo} onClick={() => escolherPromocao(tipo)} title={tipo}>
                    {GLIFO_PROMOCAO[tipo]}
                  </button>
                ))}
              </div>
            </div>
          )}

          {fimDeJogo(estado) && (
            <div className="fim">
              <p>
                {estado.xequeMate
                  ? `Xeque-mate! Vencem as ${estado.vez === 'BRANCO' ? 'PRETAS' : 'BRANCAS'}.`
                  : 'Afogamento — empate.'}
              </p>
              {minhaCor === null && <button onClick={() => criar.mutate(false)}>Jogar de novo</button>}
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default App
