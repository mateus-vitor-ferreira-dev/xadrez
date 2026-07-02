/**
 * Títulos do "caminho de troféus" — espelho do enum `dominio/Titulo.java`. Cada
 * título é um brinde desbloqueado por Elo, nas mesmas faixas do `Rank`.
 *
 * Diferente das skins (client-side), o título EQUIPADO mora no servidor (é público:
 * aparece no ranking). Aqui guardamos só a tabela para exibir rótulo/requisito e
 * decidir o que está liberado.
 */
export interface Titulo {
  /** Id estável, igual ao nome do enum no backend (ex.: 'CAVALEIRO'). */
  id: string
  /** Nome exibido (ex.: 'Cavaleiro'). */
  rotulo: string
  /** Elo necessário para desbloquear. */
  eloMin: number
  /** Faixa de rank que libera (só para exibir o requisito). */
  rank: string
}

export const TITULOS: Titulo[] = [
  { id: 'APRENDIZ', rotulo: 'Aprendiz', eloMin: 0, rank: 'Iniciante' },
  { id: 'ESCUDEIRO', rotulo: 'Escudeiro', eloMin: 1000, rank: 'Intermediário' },
  { id: 'CAVALEIRO', rotulo: 'Cavaleiro', eloMin: 1400, rank: 'Avançado' },
  { id: 'ESTRATEGISTA', rotulo: 'Estrategista', eloMin: 1800, rank: 'Especialista' },
  { id: 'TATICO_MESTRE', rotulo: 'Tático Mestre', eloMin: 2100, rank: 'Mestre' },
  { id: 'LENDA', rotulo: 'Lenda do Tabuleiro', eloMin: 2400, rank: 'Grande Mestre' },
]

/** Título pelo id (ex.: o que veio no `auth.titulo`), ou null. */
export function tituloPorId(id: string | null | undefined): Titulo | null {
  return id ? (TITULOS.find((t) => t.id === id) ?? null) : null
}

/** Um título está liberado quando o Elo alcança o piso — ou sempre, se admin. */
export function tituloLiberado(t: Titulo, elo: number, admin = false): boolean {
  return admin || elo >= t.eloMin
}
