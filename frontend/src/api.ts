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
  email: string
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
async function postAuth(caminho: 'register' | 'login', corpo: object): Promise<Autenticacao> {
  const resposta = await fetch(`${API}/auth/${caminho}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(corpo),
  })
  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => null)
    throw new Error(erro?.message ?? (resposta.status === 409 ? 'Apelido ou e-mail já cadastrado.' : 'Falha na autenticação.'))
  }
  return resposta.json() as Promise<Autenticacao>
}

/** Cadastro: apelido (único), e-mail (único) e senha. */
export const registrar = (usuario: string, email: string, senha: string) =>
  postAuth('register', { usuario, email, senha })

/** Login: identificador = e-mail OU apelido, + senha. */
export const login = (identificador: string, senha: string) =>
  postAuth('login', { identificador, senha })

// ---------- Login com Google ----------
/** Resposta do /auth/google: ou já loga (sessao), ou pede apelido (novo). */
export interface GoogleAuthResp {
  novo: boolean
  sessao: Autenticacao | null
  email: string | null
  sugestaoApelido: string | null
}

/** Manda o ID token do Google; back diz se já existe conta ou é 1º acesso. */
export async function googleLogin(credential: string): Promise<GoogleAuthResp> {
  const resposta = await fetch(`${API}/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ credential }),
  })
  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => null)
    throw new Error(erro?.message ?? 'Falha no login com Google.')
  }
  return resposta.json() as Promise<GoogleAuthResp>
}

/** 1º acesso via Google: cria a conta com o apelido escolhido e devolve a sessão. */
export async function googleFinalizar(credential: string, apelido: string): Promise<Autenticacao> {
  const resposta = await fetch(`${API}/auth/google/finalizar`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ credential, apelido }),
  })
  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => null)
    throw new Error(erro?.message ?? (resposta.status === 409 ? 'Esse apelido já está em uso.' : 'Falha ao finalizar o cadastro.'))
  }
  return resposta.json() as Promise<Autenticacao>
}
