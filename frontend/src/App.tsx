import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { buscarMovimentos, buscarPartida, jogar, novaPartida } from './api'
import Tabuleiro from './Tabuleiro'

/**
 * Distinção de estado:
 *  - ESTADO DE SERVIDOR (a partida, os lances legais) -> TanStack Query.
 *  - ESTADO DE UI (casa selecionada, mensagem de erro) -> useState.
 */
function App() {
  const queryClient = useQueryClient()
  const [idPartida, setIdPartida] = useState<number | null>(null)
  const [selecionada, setSelecionada] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  const partida = useQuery({
    queryKey: ['partida', idPartida],
    queryFn: () => buscarPartida(idPartida!),
    enabled: idPartida !== null,
  })
  const estado = partida.data

  // Lances legais da casa selecionada (para destacar no tabuleiro).
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
      setErro(null)
    },
  })

  const fazerJogada = useMutation({
    mutationFn: (j: { origem: string; destino: string }) => jogar(idPartida!, j.origem, j.destino),
    onSuccess: (e) => {
      queryClient.setQueryData(['partida', idPartida], e)
      setErro(null)
    },
    onError: (e) => setErro(e instanceof Error ? e.message : 'Jogada inválida'),
  })

  /** A casa tem uma peça de quem é a vez? (só deixamos selecionar essas) */
  function temPecaDaVez(notacao: string): boolean {
    if (!estado) return false
    const coluna = notacao.charCodeAt(0) - 97 // 'a' -> 0
    const linha = Number(notacao[1]) - 1 // '1' -> 0
    const ch = estado.tabuleiro[linha * 8 + coluna]
    if (ch === '.') return false
    const branca = ch === ch.toUpperCase()
    return (branca && estado.vez === 'BRANCO') || (!branca && estado.vez === 'PRETO')
  }

  function clicarCasa(notacao: string) {
    setErro(null)
    if (selecionada === null) {
      if (temPecaDaVez(notacao)) setSelecionada(notacao) // 1º clique: escolhe a origem
    } else if (selecionada === notacao) {
      setSelecionada(null) // clicou de novo: cancela
    } else {
      fazerJogada.mutate({ origem: selecionada, destino: notacao }) // 2º clique: joga
      setSelecionada(null)
    }
  }

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
            {estado.xequeMate && <span className="alerta"> — XEQUE-MATE!</span>}
            {estado.afogamento && <span className="alerta"> — afogamento (empate)</span>}
            {!estado.xequeMate && estado.xeque && <span className="alerta"> — xeque!</span>}
          </p>

          <Tabuleiro
            tabuleiro={estado.tabuleiro}
            selecionada={selecionada}
            destaques={destaques}
            onClicarCasa={clicarCasa}
          />

          {erro && <p className="status alerta">{erro}</p>}
        </>
      )}
    </div>
  )
}

export default App
