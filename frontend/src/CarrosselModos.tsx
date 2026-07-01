import { useEffect, useRef, useState, type KeyboardEvent, type TransitionEvent, type WheelEvent } from 'react'
import { MODOS, type Modo } from './modos'

const N = MODOS.length

/**
 * Carrossel VERTICAL e INFINITO de modos (estilo "seleção de personagem"): os
 * modos ficam num trilho que desliza para deixar o ATIVO sempre centralizado e
 * em destaque; os vizinhos aparecem esmaecidos acima/abaixo — e, por ser
 * circular, SEMPRE há um vizinho de cada lado (nunca sobra slot vazio).
 *
 * <h3>Como o "infinito" funciona</h3>
 * Renderizamos a lista TRIPLICADA (3 blocos iguais). A posição visual `pos` é a
 * cópia centralizada; começamos no bloco do meio. Ao mover, `pos` anda ±1 e o
 * trilho desliza. Terminada a transição, "teleportamos" `pos` de volta ao bloco
 * do meio SEM animar — como as cópias são idênticas, é imperceptível, mas
 * garante que nunca ficamos sem itens para mostrar dos dois lados.
 *
 * Navegação: setas ↑/↓ (com foco), clique num vizinho, roda do mouse e ▲/▼.
 */
export default function CarrosselModos({
  ativo,
  onAtivoChange,
}: {
  ativo: Modo
  onAtivoChange: (m: Modo) => void
}) {
  const indiceAtivo = MODOS.findIndex((m) => m.id === ativo)
  // Cópias: [M0,M1,M2, M0,M1,M2, M0,M1,M2]. Começa na cópia do meio (bloco 1).
  const itens = [...MODOS, ...MODOS, ...MODOS]
  const [pos, setPos] = useState(N + indiceAtivo)
  const [transicao, setTransicao] = useState(true)

  const trilhoRef = useRef<HTMLDivElement>(null)
  const animando = useRef(false)
  const indiceAtivoRef = useRef(indiceAtivo)
  indiceAtivoRef.current = indiceAtivo // sempre o índice mais recente (para o teleporte)
  const backstop = useRef(0)

  // Fim do deslize: teleporta ao bloco do meio (sem animar) e destrava.
  function finalizar() {
    window.clearTimeout(backstop.current)
    setTransicao(false)
    setPos(N + indiceAtivoRef.current)
  }

  // Reabilita a transição no frame seguinte ao teleporte e libera novo movimento.
  useEffect(() => {
    if (!transicao) {
      const id = requestAnimationFrame(() => {
        setTransicao(true)
        animando.current = false
      })
      return () => cancelAnimationFrame(id)
    }
  }, [transicao])

  // Move a seleção em 'delta' passos (um por vez: ignora enquanto anima).
  function mover(delta: number) {
    if (delta === 0 || animando.current) return
    animando.current = true
    setTransicao(true)
    setPos((p) => p + delta)
    const novo = (((indiceAtivo + delta) % N) + N) % N // índice circular
    onAtivoChange(MODOS[novo].id)
    // Rede de segurança caso 'transitionend' não dispare (ex.: reduced-motion).
    backstop.current = window.setTimeout(finalizar, 480)
  }

  function aoFimTransicao(e: TransitionEvent) {
    if (e.target === trilhoRef.current && e.propertyName === 'transform') finalizar()
  }

  function aoTeclar(e: KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      mover(1)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      mover(-1)
    }
  }

  function aoRolar(e: WheelEvent) {
    mover(e.deltaY > 0 ? 1 : -1)
  }

  return (
    <div
      className="carrossel"
      role="listbox"
      aria-label="Modo de jogo"
      tabIndex={0}
      onKeyDown={aoTeclar}
      onWheel={aoRolar}
    >
      <button className="carrossel-seta" aria-label="Modo anterior" onClick={() => mover(-1)}>
        ▲
      </button>

      <div className="carrossel-janela">
        {/* Slot central = índice 1 de 3 visíveis, por isso o deslocamento é
            (1 - pos) alturas. A transição é desligada durante o teleporte. */}
        <div
          ref={trilhoRef}
          className="carrossel-trilho"
          style={{
            transform: `translateY(calc((1 - ${pos}) * var(--item-altura)))`,
            transition: transicao ? undefined : 'none',
          }}
          onTransitionEnd={aoFimTransicao}
        >
          {itens.map((m, g) => (
            <button
              key={g}
              role="option"
              aria-selected={g === pos}
              className={`carrossel-item${g === pos ? ' ativo' : ''}`}
              onClick={() => mover(g - pos)}
              tabIndex={-1}
            >
              <span className="icone">{m.icone}</span>
              <span className="texto">
                <span className="carrossel-titulo">{m.titulo}</span>
                <span className="carrossel-desc">{m.desc}</span>
              </span>
            </button>
          ))}
        </div>
      </div>

      <button className="carrossel-seta" aria-label="Próximo modo" onClick={() => mover(1)}>
        ▼
      </button>
    </div>
  )
}
