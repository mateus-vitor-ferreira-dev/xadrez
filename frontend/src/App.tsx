import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  buscarMovimentos,
  buscarPartida,
  jogadaIA,
  jogar,
  novaPartida,
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

/** Descobre o lance (origem/destino) comparando o tabuleiro antes e depois. */
function diffLance(antes: string, depois: string): { origem: string; destino: string } | null {
  let origem = -1
  let destino = -1
  for (let i = 0; i < 64; i++) {
    if (antes[i] !== depois[i]) {
      if (depois[i] === '.') origem = i // casa que ficou vazia
      else destino = i // casa que recebeu peça
    }
  }
  if (origem < 0 || destino < 0) return null
  const notacao = (i: number) => String.fromCharCode(97 + (i % 8)) + (Math.floor(i / 8) + 1)
  return { origem: notacao(origem), destino: notacao(destino) }
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

  // A IA joga pelas PRETAS.
  const iaMutation = useMutation({
    mutationFn: () => jogadaIA(idPartida!, nivel),
    onSuccess: (depois) => {
      const antes = queryClient.getQueryData<EstadoPartida>(['partida', idPartida])
      queryClient.setQueryData(['partida', idPartida], depois)
      if (antes) setUltimoLance(diffLance(antes.tabuleiro, depois.tabuleiro))
    },
  })

  const criar = useMutation({
    mutationFn: novaPartida,
    onSuccess: (e) => {
      setIdPartida(e.id)
      queryClient.setQueryData(['partida', e.id], e)
      setSelecionada(null)
      setUltimoLance(null)
      setPromocaoPendente(null)
      setErro(null)
    },
  })

  const fazerJogada = useMutation({
    mutationFn: (j: { origem: string; destino: string; promocao?: TipoPromocao }) =>
      jogar(idPartida!, j.origem, j.destino, j.promocao),
    onSuccess: (e, vars) => {
      queryClient.setQueryData(['partida', idPartida], e)
      setUltimoLance({ origem: vars.origem, destino: vars.destino })
      setErro(null)
      // No modo vs IA, se agora é a vez das pretas, a IA responde.
      if (modo === 'ia' && e.vez === 'PRETO' && !fimDeJogo(e)) {
        iaMutation.mutate()
      }
    },
    onError: (e) => setErro(e instanceof Error ? e.message : 'Jogada inválida'),
  })

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

  // É a vez da IA (não deixa o humano clicar enquanto isso)?
  const vezDaIA = modo === 'ia' && estado?.vez === 'PRETO'

  function clicarCasa(notacao: string) {
    setErro(null)
    if (promocaoPendente || vezDaIA || iaMutation.isPending) return
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

  return (
    <div className="app">
      <h1>♟ Xadrez</h1>

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

      <button onClick={() => criar.mutate()} disabled={criar.isPending}>
        {criar.isPending ? 'Criando…' : 'Nova partida'}
      </button>

      {estado && (
        <>
          <p className="status">
            Vez das <strong>{estado.vez}</strong>
            {iaMutation.isPending && <span> — IA pensando…</span>}
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
              <button onClick={() => criar.mutate()}>Jogar de novo</button>
            </div>
          )}
        </>
      )}
    </div>
  )
}

export default App
