// Skins de TABULEIRO: temas de cor das casas, desbloqueados por rank (como as
// skins de peças). O truque é simples — a cor de TODO o tabuleiro vem de duas
// variáveis CSS (`--wood-light` = casas claras, `--wood-dark` = casas escuras).
// Equipar um tema é só trocar essas duas variáveis em runtime (ver skin.tsx).
//
// Por ser puramente visual, o desbloqueio fica CLIENT-SIDE (via auth.elo), igual
// às skins de peças; a conta admin libera tudo.

export interface TemaTabuleiro {
  id: string
  nome: string
  /** Elo necessário para desbloquear (0 = livre). */
  eloMin: number
  /** Nome do rank que desbloqueia (só para exibir). */
  rankNome: string
  /** Cor das casas CLARAS. */
  claro: string
  /** Cor das casas ESCURAS. */
  escuro: string
}

// Cortes de Elo alinhados às skins de peças. O Clássico mantém EXATAMENTE a
// madeira atual (mesmos valores do :root), então nada muda para quem não troca.
export const TABULEIROS: TemaTabuleiro[] = [
  { id: 'classico', nome: 'Clássico', eloMin: 0, rankNome: 'Todos', claro: '#ecd6a8', escuro: '#a3743f' },
  { id: 'ceu', nome: 'Céu', eloMin: 1400, rankNome: 'Avançado', claro: '#cfe0ef', escuro: '#5a7fa6' },
  { id: 'nevoa', nome: 'Névoa', eloMin: 1800, rankNome: 'Especialista', claro: '#e8ebee', escuro: '#7d8896' },
  { id: 'bosque', nome: 'Bosque', eloMin: 2100, rankNome: 'Mestre', claro: '#d6e6c8', escuro: '#5a7d4a' },
  { id: 'brasa', nome: 'Brasa', eloMin: 2400, rankNome: 'Grande Mestre', claro: '#f0d9d2', escuro: '#9a5148' },
]

export const TABULEIRO_PADRAO = TABULEIROS[0]

export function tabuleiroPorId(id: string | null): TemaTabuleiro {
  return TABULEIROS.find((t) => t.id === id) ?? TABULEIRO_PADRAO
}

/** Liberado quando o Elo alcança o piso da faixa — ou sempre, se admin. */
export function tabuleiroLiberado(t: TemaTabuleiro, elo: number, admin = false): boolean {
  return admin || elo >= t.eloMin
}
