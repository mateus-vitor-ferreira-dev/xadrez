// Camada de acesso à API do xadrez (backend Spring).
// Os TIPOS aqui espelham o contrato do backend (EstadoPartidaResponse).
// Tipar isso é o ganho de TypeScript: se o back mudar um campo, o TS reclama aqui.

export type Cor = 'BRANCO' | 'PRETO'

export interface EstadoPartida {
  id: number
  vez: Cor
  xeque: boolean
  xequeMate: boolean
  afogamento: boolean
  /** 64 caracteres: 1 por casa (linha 0→7, coluna 0→7). Maiúscula=branca, minúscula=preta, '.'=vazia. */
  tabuleiro: string
}

// Em PRODUÇÃO, VITE_API_URL aponta para o backend na Railway (definido na Vercel).
// No DESENVOLVIMENTO ela fica vazia, e o "/partidas" cai no proxy do Vite -> 8080.
const API = import.meta.env.VITE_API_URL ?? ''
const BASE = `${API}/partidas`

/** Lê a resposta como JSON ou lança um erro com a mensagem do backend ({ "erro": ... }). */
async function lerOuFalhar(resposta: Response): Promise<EstadoPartida> {
  if (!resposta.ok) {
    const corpo = await resposta.json().catch(() => null)
    throw new Error(corpo?.erro ?? `Erro ${resposta.status}`)
  }
  return resposta.json() as Promise<EstadoPartida>
}

/** POST /partidas — cria uma partida nova. */
export async function novaPartida(): Promise<EstadoPartida> {
  return lerOuFalhar(await fetch(BASE, { method: 'POST' }))
}

/** GET /partidas/{id} — estado atual de uma partida. */
export async function buscarPartida(id: number): Promise<EstadoPartida> {
  return lerOuFalhar(await fetch(`${BASE}/${id}`))
}

/** POST /partidas/{id}/jogadas — aplica uma jogada (origem/destino em notação, ex.: "e2"). */
export async function jogar(id: number, origem: string, destino: string): Promise<EstadoPartida> {
  const resposta = await fetch(`${BASE}/${id}/jogadas`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ origem, destino }),
  })
  return lerOuFalhar(resposta)
}
