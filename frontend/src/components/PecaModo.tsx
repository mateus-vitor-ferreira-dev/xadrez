import { MODOS, type Modo } from '../lib/modos'

/**
 * "Hero" da tela inicial: a peça-símbolo do modo selecionado, que CAI do alto e
 * assenta em pé (como os personagens trocando no menu do UNS4). O `key={modo}`
 * remonta o elemento a cada troca de modo, fazendo a animação de queda tocar de
 * novo. A sombra no chão cresce em sincronia.
 */
export default function PecaModo({ modo }: { modo: Modo }) {
  const info = MODOS.find((m) => m.id === modo)!
  return (
    <div className="peca-modo">
      <span key={`sombra-${modo}`} className="peca-sombra" />
      <img
        key={modo}
        className="peca-cair"
        src={`/pecas/${info.peca}.svg`}
        alt={info.titulo}
        draggable={false}
      />
    </div>
  )
}
