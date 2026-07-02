// Os 3 modos de jogo da tela inicial. Fica num módulo próprio para ser
// compartilhado entre App, CarrosselModos e PecaModo sem import circular.

export type Modo = 'humano' | 'ia' | 'online'

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
]
