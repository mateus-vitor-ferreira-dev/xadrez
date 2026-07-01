import { useRef, type KeyboardEvent, type WheelEvent } from 'react'
import { MODOS, type Modo } from './modos'

/**
 * Carrossel VERTICAL de modos (estilo "seleção de personagem"): os 3 modos ficam
 * num trilho que desliza para deixar o modo ATIVO sempre no centro, em destaque;
 * os vizinhos aparecem esmaecidos acima/abaixo.
 *
 * Navegação: setas ↑/↓ (com foco), clique num item, roda do mouse (scroll) e os
 * botões ▲/▼. O índice é "clampeado" (0..2) — combina com o deslocamento por
 * translateY (sem dar a volta).
 */
export default function CarrosselModos({
  ativo,
  onAtivoChange,
}: {
  ativo: Modo
  onAtivoChange: (m: Modo) => void
}) {
  const indiceAtivo = MODOS.findIndex((m) => m.id === ativo)
  const ultimoScroll = useRef(0)

  // Move a seleção em 'delta' passos, sem sair dos limites [0, MODOS.length-1].
  function mover(delta: number) {
    const proximo = Math.min(Math.max(indiceAtivo + delta, 0), MODOS.length - 1)
    if (proximo !== indiceAtivo) onAtivoChange(MODOS[proximo].id)
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

  // Throttle simples: no máximo um passo a cada 220ms, senão um único gesto de
  // scroll pularia vários modos de uma vez.
  function aoRolar(e: WheelEvent) {
    const agora = Date.now()
    if (agora - ultimoScroll.current < 220) return
    ultimoScroll.current = agora
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
      <button className="carrossel-seta" aria-label="Modo anterior" onClick={() => mover(-1)} disabled={indiceAtivo === 0}>
        ▲
      </button>

      <div className="carrossel-janela">
        {/* Trilho deslizante: centraliza o ativo. Slot central = índice 1 de 3
            visíveis, por isso o deslocamento é (1 - indiceAtivo) alturas. */}
        <div className="carrossel-trilho" style={{ transform: `translateY(calc((1 - ${indiceAtivo}) * var(--item-altura)))` }}>
          {MODOS.map((m, i) => (
            <button
              key={m.id}
              role="option"
              aria-selected={i === indiceAtivo}
              className={`carrossel-item${i === indiceAtivo ? ' ativo' : ''}`}
              onClick={() => onAtivoChange(m.id)}
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

      <button
        className="carrossel-seta"
        aria-label="Próximo modo"
        onClick={() => mover(1)}
        disabled={indiceAtivo === MODOS.length - 1}
      >
        ▼
      </button>
    </div>
  )
}
