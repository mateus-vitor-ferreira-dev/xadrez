import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { buscarMovimentos, buscarPartida, jogar, novaPartida, type TipoPromocao } from './api'
import Tabuleiro from './Tabuleiro'

// Glifos para o seletor de promoção.
const GLIFO_PROMOCAO: Record<TipoPromocao, string> = {
  RAINHA: '♛',
  TORRE: '♜',
  BISPO: '♝',
  CAVALO: '♞',
}

function App() {
  const queryClient = useQueryClient()
  const [idPartida, setIdPartida] = useState<number | null>(null)
  const [selecionada, setSelecionada] = useState<string | null>(null)
  const [ultimoLance, setUltimoLance] = useState<{ origem: string; destino: string } | null>(null)
  const [promocaoPendente, setPromocaoPendente] = useState<{ origem: string; destino: string } | null>(null)
  const [erro, setErro] = useState<string | null>(null)

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
    // o 2º argumento do onSuccess são as variáveis enviadas -> guardamos o último lance
    onSuccess: (e, vars) => {
      queryClient.setQueryData(['partida', idPartida], e)
      setUltimoLance({ origem: vars.origem, destino: vars.destino })
      setErro(null)
    },
    onError: (e) => setErro(e instanceof Error ? e.message : 'Jogada inválida'),
  })

  /** Char da peça na casa (ou '.') a partir da notação. */
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

  /** O lance é a promoção de um peão (peão indo para a última fileira)? */
  function ehPromocao(origem: string, destino: string): boolean {
    if (pecaEm(origem).toLowerCase() !== 'p') return false
    const fileira = Number(destino[1])
    return fileira === 8 || fileira === 1
  }

  /** Casa do rei em xeque (para destacar em vermelho), ou null. */
  function casaDoReiEmXeque(): string | null {
    if (!estado || !estado.xeque) return null
    const rei = estado.vez === 'BRANCO' ? 'R' : 'r'
    const idx = estado.tabuleiro.indexOf(rei)
    if (idx < 0) return null
    return String.fromCharCode(97 + (idx % 8)) + (Math.floor(idx / 8) + 1)
  }

  function clicarCasa(notacao: string) {
    setErro(null)
    if (promocaoPendente) return // aguardando escolha da promoção
    if (selecionada === null) {
      if (temPecaDaVez(notacao)) setSelecionada(notacao)
    } else if (selecionada === notacao) {
      setSelecionada(null)
    } else {
      // só trata como jogada se o destino for um lance legal destacado
      if (destaques.includes(notacao)) {
        if (ehPromocao(selecionada, notacao)) {
          setPromocaoPendente({ origem: selecionada, destino: notacao }) // abre o seletor
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

  const fimDeJogo = estado && (estado.xequeMate || estado.afogamento)

  return (
    <div className="app">
      <h1>♟ Xadrez</h1>

      <button onClick={() => criar.mutate()} disabled={criar.isPending}>
        {criar.isPending ? 'Criando…' : 'Nova partida'}
      </button>

      {estado && (
        <>
          <p className="status">
            Vez das <strong>{estado.vez}</strong>
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

          {/* Seletor de promoção */}
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

          {/* Fim de jogo */}
          {fimDeJogo && (
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
