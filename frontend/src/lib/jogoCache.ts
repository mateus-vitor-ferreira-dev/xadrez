// Cache do jogo LOCAL (2 jogadores) ou contra a IA — modos que NÃO exigem login.
//
// Usamos sessionStorage (não localStorage) de propósito: ele sobrevive a um F5
// na mesma aba, mas é apagado quando a aba fecha. Isso casa exatamente com a
// regra "salva em cache e apaga ao sair". Também limpamos manualmente ao voltar
// ao lobby (ver App.voltar).
//
// Partidas ONLINE não passam por aqui: elas exigem conta e já são restauráveis
// pela URL (?partida=&cor=).

const CHAVE = 'jogo-local'

/** O que guardamos para retomar um jogo local/IA após um F5. */
export interface JogoCache {
  id: number
  modo: 'humano' | 'ia'
  nivel: number
  historico: string[]
}

export function salvar(jogo: JogoCache): void {
  try {
    sessionStorage.setItem(CHAVE, JSON.stringify(jogo))
  } catch {
    // sem sessionStorage (aba privada antiga etc.): apenas ignora.
  }
}

export function ler(): JogoCache | null {
  try {
    return JSON.parse(sessionStorage.getItem(CHAVE) ?? 'null') as JogoCache | null
  } catch {
    return null
  }
}

export function limpar(): void {
  try {
    sessionStorage.removeItem(CHAVE)
  } catch {
    // ignora
  }
}
