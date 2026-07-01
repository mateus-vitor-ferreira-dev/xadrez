// Camada de acesso à API do xadrez (backend Spring).

export type Cor = 'BRANCO' | 'PRETO'

export type Resultado = 'EM_ANDAMENTO' | 'VITORIA_BRANCO' | 'VITORIA_PRETO' | 'EMPATE'

export interface EstadoPartida {
  id: number
  vez: Cor
  xeque: boolean
  xequeMate: boolean
  afogamento: boolean
  /** 64 caracteres: 1 por casa. Maiúscula=branca, minúscula=preta, '.'=vazia. */
  tabuleiro: string
  // ----- Partida online / Elo (Fase 4) -----
  online: boolean
  /** Username de cada lado (null = ainda não entrou / anônimo). */
  branco: string | null
  preto: string | null
  resultado: Resultado
  /** Elo atual de cada lado (null se anônimo). */
  eloBranco: number | null
  eloPreto: number | null
  /** Variação de Elo desta partida (só quando termina; null antes disso). */
  deltaBranco: number | null
  deltaPreto: number | null
}

export type TipoPromocao = 'RAINHA' | 'TORRE' | 'BISPO' | 'CAVALO'

/** Dados devolvidos por cadastro/login. */
export interface Autenticacao {
  token: string
  usuario: string
  elo: number
}

// Em produção VITE_API_URL aponta para o backend; em dev fica vazio (proxy do Vite).
const API = import.meta.env.VITE_API_URL ?? ''
const BASE = `${API}/partidas`

/** Token JWT atual (guardado em localStorage sob a chave "auth"). */
function tokenAtual(): string | null {
  try {
    return (JSON.parse(localStorage.getItem('auth') ?? 'null') as Autenticacao | null)?.token ?? null
  } catch {
    return null
  }
}

/** Cabeçalhos com o Bearer token, se houver login. */
function comAuth(extra?: Record<string, string>): Record<string, string> {
  const h: Record<string, string> = { ...extra }
  const t = tokenAtual()
  if (t) h.Authorization = `Bearer ${t}`
  return h
}

async function lerOuFalhar(resposta: Response): Promise<EstadoPartida> {
  if (!resposta.ok) {
    const corpo = await resposta.json().catch(() => null)
    throw new Error(corpo?.erro ?? corpo?.message ?? `Erro ${resposta.status}`)
  }
  return resposta.json() as Promise<EstadoPartida>
}

// ---------- Partidas ----------
export async function novaPartida(online = false): Promise<EstadoPartida> {
  const url = online ? `${BASE}?online=true` : BASE
  return lerOuFalhar(await fetch(url, { method: 'POST', headers: comAuth() }))
}

/** Entra numa partida online (assume as pretas, se logado). Idempotente no back. */
export async function entrar(id: number): Promise<EstadoPartida> {
  return lerOuFalhar(await fetch(`${BASE}/${id}/entrar`, { method: 'POST', headers: comAuth() }))
}

export async function buscarPartida(id: number): Promise<EstadoPartida> {
  return lerOuFalhar(await fetch(`${BASE}/${id}`, { headers: comAuth() }))
}

export async function jogadaIA(id: number, nivel: number): Promise<EstadoPartida> {
  return lerOuFalhar(await fetch(`${BASE}/${id}/jogada-ia?nivel=${nivel}`, { method: 'POST', headers: comAuth() }))
}

export async function buscarMovimentos(id: number, origem: string): Promise<string[]> {
  const resposta = await fetch(`${BASE}/${id}/movimentos?origem=${origem}`, { headers: comAuth() })
  if (!resposta.ok) return []
  return resposta.json() as Promise<string[]>
}

export async function jogar(id: number, origem: string, destino: string, promocao?: TipoPromocao): Promise<EstadoPartida> {
  const resposta = await fetch(`${BASE}/${id}/jogadas`, {
    method: 'POST',
    headers: comAuth({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ origem, destino, promocao }),
  })
  return lerOuFalhar(resposta)
}

// ---------- Autenticação ----------
async function autenticar(caminho: 'register' | 'login', usuario: string, senha: string): Promise<Autenticacao> {
  const resposta = await fetch(`${API}/auth/${caminho}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ usuario, senha }),
  })
  if (!resposta.ok) {
    const corpo = await resposta.json().catch(() => null)
    throw new Error(corpo?.message ?? (resposta.status === 409 ? 'Usuário já existe.' : 'Falha na autenticação.'))
  }
  return resposta.json() as Promise<Autenticacao>
}

export const registrar = (usuario: string, senha: string) => autenticar('register', usuario, senha)
export const login = (usuario: string, senha: string) => autenticar('login', usuario, senha)
