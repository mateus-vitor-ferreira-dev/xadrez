// Os modos da tela inicial (carrossel). Fica num módulo próprio para ser
// compartilhado entre App, CarrosselModos e PecaModo sem import circular.
//
// Além dos 3 modos de JOGO, há o "tutorial": não inicia partida — leva ao manual
// /como-jogar. O App trata esse caso à parte (ver acaoPrincipal/rotuloAcao).

export type Modo = 'humano' | 'ia' | 'online' | 'tutorial'

export interface ModoInfo {
  id: Modo
  icone: string
  titulo: string
  desc: string
  /** Peça-símbolo do modo (arquivo em /pecas, ex.: 'wr' = rei branco). */
  peca: string
}

export const MODOS: ModoInfo[] = [
  { id: 'humano', icone: '👥', titulo: '2 jogadores', desc: 'No mesmo dispositivo', peca: 'wr' },
  { id: 'ia', icone: '🤖', titulo: 'Contra a IA', desc: 'Escolha o nível', peca: 'wc' },
  { id: 'online', icone: '🌐', titulo: 'Online', desc: 'Ranqueado por Elo', peca: 'wd' },
  { id: 'tutorial', icone: '📖', titulo: 'Tutorial', desc: 'Aprenda a jogar', peca: 'wp' },
]
