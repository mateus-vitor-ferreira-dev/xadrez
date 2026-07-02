import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { svgDaPeca, temaPorId, type TemaPecas } from '../themes/skins'
import { tabuleiroPorId, type TemaTabuleiro } from '../themes/tabuleiros'

// Estado das SKINS equipadas — vive num Context (como o auth), pois o tabuleiro, o
// inventário e o resto da árvore precisam ler/trocar as mesmas skins. Persistimos as
// escolhas no localStorage; "Clássico" é o padrão (equivale a "sem skin"). Aqui moram
// duas skins: das PEÇAS (recolore os SVGs) e do TABULEIRO (troca as cores das casas).

interface ContextoSkin {
  /** Tema de PEÇAS equipado. */
  tema: TemaPecas
  /** Equipa um tema de peças pelo id (persiste). */
  equipar: (id: string) => void
  /** Tema de TABULEIRO equipado. */
  tabuleiro: TemaTabuleiro
  /** Equipa um tema de tabuleiro pelo id (persiste + aplica as cores). */
  equiparTabuleiro: (id: string) => void
}

const SkinContext = createContext<ContextoSkin | null>(null)

export function SkinProvider({ children }: { children: ReactNode }) {
  const [id, setId] = useState<string>(() => localStorage.getItem('skinPecas') ?? 'classico')
  const [tabId, setTabId] = useState<string>(() => localStorage.getItem('skinTabuleiro') ?? 'classico')

  // Aplica o tabuleiro equipado trocando as variáveis CSS que as casas usam
  // (--wood-light/--wood-dark). Como todo o tabuleiro — e os diagramas do manual —
  // lê essas variáveis, muda tudo de uma vez, sem tocar em Tabuleiro.tsx. Roda ao
  // montar e a cada troca.
  useEffect(() => {
    const t = tabuleiroPorId(tabId)
    const raiz = document.documentElement.style
    raiz.setProperty('--wood-light', t.claro)
    raiz.setProperty('--wood-dark', t.escuro)
  }, [tabId])

  const valor: ContextoSkin = {
    tema: temaPorId(id),
    equipar: (novo) => {
      setId(novo)
      localStorage.setItem('skinPecas', novo)
    },
    tabuleiro: tabuleiroPorId(tabId),
    equiparTabuleiro: (novo) => {
      setTabId(novo)
      localStorage.setItem('skinTabuleiro', novo)
    },
  }

  return <SkinContext.Provider value={valor}>{children}</SkinContext.Provider>
}

export function useSkin(): ContextoSkin {
  const ctx = useContext(SkinContext)
  if (!ctx) throw new Error('useSkin precisa estar dentro de <SkinProvider>.')
  return ctx
}

/**
 * Renderiza a peça como SVG inline, recolorido pelo tema. Sem `tema`, usa o
 * equipado (via contexto) — é como o tabuleiro consome. O inventário passa um
 * `tema` explícito para pré-visualizar cada skin.
 *
 * @param code código da peça, ex.: 'wp' (peão branco)
 */
export function PecaSvg({ code, tema, className }: { code: string; tema?: TemaPecas; className?: string }) {
  const ctx = useContext(SkinContext)
  const ativo = tema ?? ctx?.tema ?? temaPorId('classico')
  const html = useMemo(() => svgDaPeca(code, ativo), [code, ativo])
  return <span className={className} dangerouslySetInnerHTML={{ __html: html }} />
}
