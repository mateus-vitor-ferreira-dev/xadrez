// Mapeia o tipo de peça (em minúsculo) para o símbolo Unicode de xadrez.
const GLIFOS: Record<string, string> = {
  r: '♚', // rei
  d: '♛', // dama
  t: '♜', // torre
  b: '♝', // bispo
  c: '♞', // cavalo
  p: '♟', // peão
}

interface Props {
  tabuleiro: string
  selecionada: string | null
  destaques: string[]
  casaXeque: string | null
  ultimoLance: { origem: string; destino: string } | null
  onClicarCasa: (notacao: string) => void
}

function Tabuleiro({ tabuleiro, selecionada, destaques, casaXeque, ultimoLance, onClicarCasa }: Props) {
  const casas = []

  for (let linha = 7; linha >= 0; linha--) {
    for (let coluna = 0; coluna < 8; coluna++) {
      const caractere = tabuleiro[linha * 8 + coluna]
      const vazia = caractere === '.'
      const corDaCasa = (linha + coluna) % 2 === 0 ? 'escura' : 'clara'
      const branca = !vazia && caractere === caractere.toUpperCase()
      const glifo = vazia ? '' : GLIFOS[caractere.toLowerCase()]

      const notacao = String.fromCharCode(97 + coluna) + (linha + 1)
      const ehDestino = destaques.includes(notacao)

      // Monta as classes da casa (destaques visuais).
      let classe = `casa ${corDaCasa}`
      if (ultimoLance && (notacao === ultimoLance.origem || notacao === ultimoLance.destino)) {
        classe += ' ultimo-lance'
      }
      if (notacao === selecionada) classe += ' selecionada'
      if (notacao === casaXeque) classe += ' em-xeque'

      casas.push(
        <button key={notacao} data-casa={notacao} className={classe} onClick={() => onClicarCasa(notacao)}>
          {/* coordenadas: número na 1ª coluna, letra na 1ª fileira (de baixo) */}
          {coluna === 0 && <span className="rotulo rotulo-rank">{linha + 1}</span>}
          {linha === 0 && <span className="rotulo rotulo-file">{String.fromCharCode(97 + coluna)}</span>}

          <span className={branca ? 'peca branca' : 'peca preta'}>{glifo}</span>
          {ehDestino && <span className={vazia ? 'marcador' : 'marcador captura'} />}
        </button>,
      )
    }
  }

  return <div className="tabuleiro">{casas}</div>
}

export default Tabuleiro
