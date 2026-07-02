/**
 * Faixas de habilidade ("ranks") derivadas do Elo — espelho do enum
 * `dominio/Rank.java` no backend. É a fonte única das faixas no front: o filtro
 * do lobby e (futuramente) o desbloqueio de skins devem reusar esta tabela em vez
 * de repetir os cortes de Elo em vários lugares.
 *
 * Cada faixa é definida só pelo seu PISO (`eloMin`). O teto de uma faixa é o piso
 * da seguinte menos 1; a última (Grande Mestre) não tem teto.
 */
export interface Rank {
  /** Identificador estável, igual ao nome do enum no backend. */
  id: string
  /** Nome amigável para exibir (com acento). */
  rotulo: string
  /** Menor Elo que ainda pertence a esta faixa. */
  eloMin: number
}

/** As faixas em ordem crescente de Elo — mesma ordem/cortes do `Rank.java`. */
export const RANKS: Rank[] = [
  { id: 'INICIANTE', rotulo: 'Iniciante', eloMin: 0 },
  { id: 'INTERMEDIARIO', rotulo: 'Intermediário', eloMin: 1000 },
  { id: 'AVANCADO', rotulo: 'Avançado', eloMin: 1400 },
  { id: 'ESPECIALISTA', rotulo: 'Especialista', eloMin: 1800 },
  { id: 'MESTRE', rotulo: 'Mestre', eloMin: 2100 },
  { id: 'GRAO_MESTRE', rotulo: 'Grande Mestre', eloMin: 2400 },
]

/**
 * Maior Elo da faixa no índice `i`: o piso da próxima menos 1. A última faixa não
 * tem teto, então devolvemos `undefined` (limite aberto pra cima).
 */
export function eloMaxDoRank(i: number): number | undefined {
  return i >= RANKS.length - 1 ? undefined : RANKS[i + 1].eloMin - 1
}

/**
 * O ÍNDICE da faixa em que um dado Elo cai. Percorre de cima para baixo e devolve
 * a primeira cujo piso o Elo alcança (Elos negativos caem em Iniciante).
 */
export function indiceDoRank(elo: number): number {
  for (let i = RANKS.length - 1; i >= 0; i--) {
    if (elo >= RANKS[i].eloMin) return i
  }
  return 0
}
