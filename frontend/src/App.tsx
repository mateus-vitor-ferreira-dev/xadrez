import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { buscarPartida, jogar, novaPartida } from './api'
import Tabuleiro from './Tabuleiro'

/**
 * Bloco 3 (final): jogar clicando.
 *
 * Distinção-chave de estado:
 *  - ESTADO DE SERVIDOR (a partida) -> TanStack Query (useQuery, por id).
 *  - ESTADO DE UI (casa selecionada, mensagem de erro) -> useState simples.
 */
function App() {
  const queryClient = useQueryClient()
  const [idPartida, setIdPartida] = useState<number | null>(null)
  const [selecionada, setSelecionada] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  // Lê a partida do backend pelo id. Só dispara quando já existe um id.
  const partida = useQuery({
    queryKey: ['partida', idPartida],
    queryFn: () => buscarPartida(idPartida!),
    enabled: idPartida !== null,
  })

  const criar = useMutation({
    mutationFn: novaPartida,
    onSuccess: (estado) => {
      setIdPartida(estado.id)
      queryClient.setQueryData(['partida', estado.id], estado) // semeia o cache
      setSelecionada(null)
      setErro(null)
    },
  })

  const fazerJogada = useMutation({
    mutationFn: (jogada: { origem: string; destino: string }) =>
      jogar(idPartida!, jogada.origem, jogada.destino),
    // Sucesso: substitui o estado em cache -> o tabuleiro re-renderiza sozinho.
    onSuccess: (estado) => {
      queryClient.setQueryData(['partida', idPartida], estado)
      setErro(null)
    },
    // Erro (ex.: jogada ilegal, HTTP 400): mostra a mensagem do backend.
    onError: (e) => setErro(e instanceof Error ? e.message : 'Jogada inválida'),
  })

  function clicarCasa(notacao: string) {
    setErro(null)
    if (selecionada === null) {
      setSelecionada(notacao) // 1º clique: escolhe a origem
    } else if (selecionada === notacao) {
      setSelecionada(null) // clicou na mesma casa: cancela
    } else {
      fazerJogada.mutate({ origem: selecionada, destino: notacao }) // 2º clique: joga
      setSelecionada(null)
    }
  }

  const estado = partida.data

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
            onClicarCasa={clicarCasa}
          />

          {erro && <p className="status alerta">{erro}</p>}
        </>
      )}
    </div>
  )
}

export default App
