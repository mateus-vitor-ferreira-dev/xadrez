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
  /** Conta administradora: libera todas as skins independentemente do rank. */
  admin: boolean
  /** Id do título equipado (ex.: 'CAVALEIRO'), ou null se nenhum. */
  titulo: string | null
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

/**
 * Sessão expirada/inválida: um 401 numa chamada que EXIGE login significa que o
 * token não vale mais (o JWT expira em 24h, ou foi revogado). Antes o front
 * confiava no `auth` do localStorage para sempre e o usuário ficava preso numa
 * tela de erro. Agora limpamos a sessão e mandamos re-logar, sinalizando com
 * ?expirada=1 para a tela de login explicar o motivo.
 */
function sessaoExpirada(): never {
  localStorage.removeItem('auth')
  if (!location.pathname.startsWith('/login')) {
    location.assign('/login?expirada=1')
  }
  throw new Error('Sua sessão expirou. Entre novamente.')
}

/**
 * fetch para endpoints que EXIGEM login: já anexa o Bearer e trata 401 como
 * sessão expirada (limpa + redireciona). Não use em rotas que aceitam anônimo —
 * lá um 401 teria outro significado.
 */
async function fetchAuth(url: string, init: RequestInit = {}): Promise<Response> {
  const resposta = await fetch(url, {
    ...init,
    headers: comAuth(init.headers as Record<string, string> | undefined),
  })
  if (resposta.status === 401) sessaoExpirada()
  return resposta
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

/** Uma sala aberta do lobby: alguém esperando oponente (o criador joga de brancas). */
export interface PartidaAberta {
  id: number
  criador: string
  elo: number
  /** Rótulo do título equipado pelo criador (pronto para exibir), ou null. */
  titulo: string | null
}

/**
 * Lista as salas abertas (lobby), opcionalmente filtrando pela faixa de Elo do
 * criador. Passa o token para o back esconder as suas próprias salas.
 */
export async function listarPartidasAbertas(eloMin?: number, eloMax?: number): Promise<PartidaAberta[]> {
  const q = new URLSearchParams()
  if (eloMin != null) q.set('eloMin', String(eloMin))
  if (eloMax != null) q.set('eloMax', String(eloMax))
  const sufixo = q.toString() ? `?${q}` : ''
  const resposta = await fetch(`${BASE}/abertas${sufixo}`, { headers: comAuth() })
  if (!resposta.ok) throw new Error(`Erro ${resposta.status}`)
  return resposta.json() as Promise<PartidaAberta[]>
}

// ---------- Ranking (leaderboards) ----------
/** Uma linha de tabela do ranking: apelido, Elo, rótulo do rank e título equipado. */
export interface LinhaRanking {
  usuario: string
  elo: number
  rank: string
  /** Rótulo do título equipado (pronto para exibir), ou null. */
  titulo: string | null
}

/** Resposta do /ranking: as duas tabelas + a faixa do usuário (para o título da direita). */
export interface Ranking {
  meuRank: string
  meuElo: number
  topSite: LinhaRanking[]
  topRank: LinhaRanking[]
}

/**
 * Busca as duas tabelas de ranking numa só chamada. Manda o token (se houver)
 * para o back escolher a faixa da tabela da direita pelo Elo do usuário logado.
 */
export async function buscarRanking(): Promise<Ranking> {
  const resposta = await fetch(`${API}/ranking`, { headers: comAuth() })
  if (!resposta.ok) throw new Error(`Erro ${resposta.status}`)
  return resposta.json() as Promise<Ranking>
}

// ---------- Título (caminho de troféus) ----------
/**
 * Equipa (ou remove, passando null) o título exibido ao lado do apelido. O
 * servidor valida se o título está desbloqueado e devolve a sessão atualizada
 * (mesmo formato do login), para o front regravar o auth.
 */
export async function equiparTitulo(titulo: string | null): Promise<Autenticacao> {
  const resposta = await fetchAuth(`${API}/usuario/titulo`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ titulo }),
  })
  if (!resposta.ok) throw new Error(`Erro ${resposta.status}`)
  return resposta.json() as Promise<Autenticacao>
}

// ---------- Perfil (tela de editar informações) ----------
/** Dados do perfil para preencher/salvar a tela de edição. */
export interface Perfil {
  /** Apelido (público, exibido — não editável por aqui). */
  usuario: string
  email: string
  /** Opcional; null = não informado. */
  telefone: string | null
}

/** Busca o perfil atual do usuário logado (para preencher o formulário). */
export async function buscarPerfil(): Promise<Perfil> {
  const resposta = await fetchAuth(`${API}/usuario/perfil`)
  if (!resposta.ok) throw new Error(`Erro ${resposta.status}`)
  return resposta.json() as Promise<Perfil>
}

/**
 * Salva as alterações do perfil (e-mail e telefone). O back valida o e-mail
 * (formato + unicidade) e devolve o perfil já atualizado.
 */
export async function atualizarPerfil(dados: {
  email: string
  telefone: string | null
}): Promise<Perfil> {
  const resposta = await fetchAuth(`${API}/usuario/perfil`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados),
  })
  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => null)
    throw new Error(erro?.message ?? (resposta.status === 409 ? 'Esse e-mail já está cadastrado.' : 'Não foi possível salvar o perfil.'))
  }
  return resposta.json() as Promise<Perfil>
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
