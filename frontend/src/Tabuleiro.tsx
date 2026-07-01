import type { CSSProperties } from 'react'

interface Props {
  tabuleiro: string
  selecionada: string | null
  destaques: string[]
  casaXeque: string | null
  ultimoLance: { origem: string; destino: string } | null
  onClicarCasa: (notacao: string) => void
}

/** Caminho do SVG da peça a partir do caractere serializado (ex.: 'T' -> /pecas/wt.svg). */
function arquivoDaPeca(caractere: string): string {
  const cor = caractere === caractere.toUpperCase() ? 'w' : 'b' // maiúscula = branca
  const tipo = caractere.toLowerCase() // t,c,b,d,r,p
  return `/pecas/${cor}${tipo}.svg`
}

function Tabuleiro({ tabuleiro, selecionada, destaques, casaXeque, ultimoLance, onClicarCasa }: Props) {
  const casas = []

  for (let linha = 7; linha >= 0; linha--) {
    for (let coluna = 0; coluna < 8; coluna++) {
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

      // Animação de deslize: a peça que chegou ao destino do último lance
      // "escorrega" da casa de origem até aqui.
      let deslize = false
      let estiloDeslize: CSSProperties | undefined
      if (!vazia && ultimoLance && notacao === ultimoLance.destino) {
        const oCol = ultimoLance.origem.charCodeAt(0) - 97
        const dCol = ultimoLance.destino.charCodeAt(0) - 97
        const oRank = Number(ultimoLance.origem[1])
        const dRank = Number(ultimoLance.destino[1])
        deslize = true
        estiloDeslize = { '--dx': oCol - dCol, '--dy': dRank - oRank } as CSSProperties
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
