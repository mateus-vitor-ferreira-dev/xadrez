import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { svgDaPeca, temaPorId, type TemaPecas } from './skins'

// Estado da SKIN equipada — vive num Context (como o auth), pois o tabuleiro, o
// inventário e o resto da árvore precisam ler/trocar a mesma skin. Persistimos a
// escolha no localStorage; "Clássico" é o padrão (equivale a "sem skin").

interface ContextoSkin {
  /** Tema atualmente equipado. */
  tema: TemaPecas
  /** Equipa um tema pelo id (persiste). */
  equipar: (id: string) => void
}

const SkinContext = createContext<ContextoSkin | null>(null)

export function SkinProvider({ children }: { children: ReactNode }) {
  const [id, setId] = useState<string>(() => localStorage.getItem('skinPecas') ?? 'classico')

  const valor: ContextoSkin = {
    tema: temaPorId(id),
    equipar: (novo) => {
      setId(novo)
      localStorage.setItem('skinPecas', novo)
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
