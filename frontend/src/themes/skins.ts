// Sistema de SKINS das peças ("piece sets" estilo chess.com), desbloqueadas por
// rank (derivado do Elo). Um tema recolore só o CORPO das peças, preservando os
// contornos/detalhes (stroke) — assim o contraste brancas×pretas nunca se perde.

// Os 12 SVGs são importados como TEXTO (?raw) e recoloridos em runtime. Bundle
// minúsculo e permite trocar a cor sem gerar dezenas de arquivos.
const RAW = import.meta.glob('./pecas/*.svg', { query: '?raw', eager: true, import: 'default' }) as Record<
  string,
  string
>
const SVGS: Record<string, string> = {}
for (const [caminho, texto] of Object.entries(RAW)) {
  const code = caminho.split('/').pop()!.replace('.svg', '') // './pecas/wp.svg' -> 'wp'
  SVGS[code] = texto
}

export interface TemaPecas {
  id: string
  nome: string
  /** Elo necessário para desbloquear (0 = livre). */
  eloMin: number
  /** Nome do rank que desbloqueia (só para exibir). */
  rankNome: string
  /** Cor do corpo das peças BRANCAS. */
  claro: string
  /** Cor do corpo das peças PRETAS. */
  escuro: string
}

// Faixas alinhadas ao enum Rank do backend (Iniciante→Grande Mestre). O Clássico
// mantém EXATAMENTE o visual atual (#fff / #000) — nada muda para quem não troca.
export const TEMAS: TemaPecas[] = [
  { id: 'classico', nome: 'Clássico', eloMin: 0, rankNome: 'Todos', claro: '#ffffff', escuro: '#000000' },
  { id: 'ambar', nome: 'Âmbar', eloMin: 1400, rankNome: 'Avançado', claro: '#f0d488', escuro: '#7a5316' },
  { id: 'marmore', nome: 'Mármore', eloMin: 1800, rankNome: 'Especialista', claro: '#eaeef3', escuro: '#5f6b7d' },
  { id: 'floresta', nome: 'Floresta', eloMin: 2100, rankNome: 'Mestre', claro: '#d3ead6', escuro: '#245c3a' },
  { id: 'rubi', nome: 'Rubi', eloMin: 2400, rankNome: 'Grande Mestre', claro: '#f3ccc6', escuro: '#7d211a' },
]

export const TEMA_PADRAO = TEMAS[0]

export function temaPorId(id: string | null): TemaPecas {
  return TEMAS.find((t) => t.id === id) ?? TEMA_PADRAO
}

/**
 * Um tema está liberado quando o Elo do jogador alcança o piso da faixa — ou
 * sempre, se a conta for administradora (acesso total, independentemente do rank).
 */
export function desbloqueado(tema: TemaPecas, elo: number, admin = false): boolean {
  return admin || elo >= tema.eloMin
}

/**
 * SVG (texto) da peça já recolorido pelo tema. Peças brancas (código começa com
 * 'w') têm o corpo em fill:#ffffff; pretas ('b'), em fill:#000000. Trocamos só
 * esse fill — os stroke (contornos e detalhes internos) ficam intactos.
 *
 * @param code código da peça, ex.: 'wp' (peão branco), 'bt' (torre preta)
 */
export function svgDaPeca(code: string, tema: TemaPecas): string {
  const raw = SVGS[code]
  if (!raw) return ''
  const branca = code.startsWith('w')
  const de = branca ? /fill:#ffffff/g : /fill:#000000/g
  return raw.replace(de, `fill:${branca ? tema.claro : tema.escuro}`)
}
