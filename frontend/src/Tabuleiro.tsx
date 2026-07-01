import type { CSSProperties } from 'react'

interface Props {
  tabuleiro: string
  selecionada: string | null
  destaques: string[]
  casaXeque: string | null
  ultimoLance: { origem: string; destino: string } | null
  onClicarCasa: (notacao: string) => void
  /** true = perspectiva das pretas (tabuleiro girado). */
  girar?: boolean
}

function arquivoDaPeca(caractere: string): string {
  const cor = caractere === caractere.toUpperCase() ? 'w' : 'b'
  const tipo = caractere.toLowerCase()
  return `/pecas/${cor}${tipo}.svg`
}

function Tabuleiro({ tabuleiro, selecionada, destaques, casaXeque, ultimoLance, onClicarCasa, girar = false }: Props) {
  const casas = []

  // Ordem de varredura: normal (brancas embaixo) ou girada (pretas embaixo).
  const linhas = girar ? [0, 1, 2, 3, 4, 5, 6, 7] : [7, 6, 5, 4, 3, 2, 1, 0]
  const colunas = girar ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7]

  for (const linha of linhas) {
    for (const coluna of colunas) {
      const caractere = tabuleiro[linha * 8 + coluna]
      const vazia = caractere === '.'
      const corDaCasa = (linha + coluna) % 2 === 0 ? 'escura' : 'clara'
      const notacao = String.fromCharCode(97 + coluna) + (linha + 1)
      const ehDestino = destaques.includes(notacao)

      let classe = `casa ${corDaCasa}`
      if (ultimoLance && (notacao === ultimoLance.origem || notacao === ultimoLance.destino)) {
        classe += ' ultimo-lance'
      }
      if (notacao === selecionada) classe += ' selecionada'
      if (notacao === casaXeque) classe += ' em-xeque'

      let deslize = false
      let estiloDeslize: CSSProperties | undefined
      if (!vazia && ultimoLance && notacao === ultimoLance.destino) {
        const oCol = ultimoLance.origem.charCodeAt(0) - 97
        const dCol = ultimoLance.destino.charCodeAt(0) - 97
        const oRank = Number(ultimoLance.origem[1])
        const dRank = Number(ultimoLance.destino[1])
        const sinal = girar ? -1 : 1 // ao girar, o deslize inverte nos dois eixos
        deslize = true
        estiloDeslize = { '--dx': (oCol - dCol) * sinal, '--dy': (dRank - oRank) * sinal } as CSSProperties
      }

      casas.push(
        <button key={notacao} data-casa={notacao} className={classe} onClick={() => onClicarCasa(notacao)}>
          {coluna === 0 && <span className="rotulo rotulo-rank">{linha + 1}</span>}
          {linha === 0 && <span className="rotulo rotulo-file">{String.fromCharCode(97 + coluna)}</span>}

          {!vazia && (
            <span className={`peca-wrap${deslize ? ' desliza' : ''}`} style={estiloDeslize}>
              <img className="peca" src={arquivoDaPeca(caractere)} alt="" draggable={false} />
            </span>
          )}
          {ehDestino && <span className={vazia ? 'marcador' : 'marcador captura'} />}
        </button>,
      )
    }
  }

  return <div className="tabuleiro">{casas}</div>
}

export default Tabuleiro
