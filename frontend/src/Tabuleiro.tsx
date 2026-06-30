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
  /** A string de 64 caracteres vinda da API (linha 0→7, coluna 0→7). */
  tabuleiro: string
  /** A casa de origem selecionada (ex.: "e2"), ou null. */
  selecionada: string | null
  /** Casas de destino legais a destacar (ex.: ["e3","e4"]). */
  destaques: string[]
  /** Chamado quando o usuário clica numa casa, passando a notação (ex.: "e4"). */
  onClicarCasa: (notacao: string) => void
}

function Tabuleiro({ tabuleiro, selecionada, destaques, onClicarCasa }: Props) {
  const casas = []

  for (let linha = 7; linha >= 0; linha--) {
    for (let coluna = 0; coluna < 8; coluna++) {
      const caractere = tabuleiro[linha * 8 + coluna]
      const vazia = caractere === '.'
      const corDaCasa = (linha + coluna) % 2 === 0 ? 'escura' : 'clara'
      const branca = !vazia && caractere === caractere.toUpperCase()
      const glifo = vazia ? '' : GLIFOS[caractere.toLowerCase()]

      const notacao = String.fromCharCode(97 + coluna) + (linha + 1)
      const destaque = notacao === selecionada ? ' selecionada' : ''
      const ehDestino = destaques.includes(notacao)

      casas.push(
        <button
          key={notacao}
          data-casa={notacao}
          className={`casa ${corDaCasa}${destaque}`}
          onClick={() => onClicarCasa(notacao)}
        >
          <span className={branca ? 'peca branca' : 'peca preta'}>{glifo}</span>
          {/* marcador de lance legal: bolinha se a casa está vazia; anel se for captura */}
          {ehDestino && <span className={vazia ? 'marcador' : 'marcador captura'} />}
        </button>,
      )
    }
  }

  return <div className="tabuleiro">{casas}</div>
}

export default Tabuleiro
