import { createContext, useContext, useState, type ReactNode } from 'react'
import type { Autenticacao } from './api'

// Antes, o estado de login vivia dentro do App. Agora que login/registro moram
// em telas (rotas) separadas, a sessão precisa ser COMPARTILHADA entre elas e o
// jogo. Um Context resolve isso: um único "dono" da sessão no topo da árvore, e
// qualquer tela lê/escreve via o hook useAuth().

interface ContextoAuth {
  /** Sessão atual (usuário + token + elo), ou null se deslogado. */
  auth: Autenticacao | null
  /** Salva a sessão após login/cadastro bem-sucedido. */
  definir: (a: Autenticacao) => void
  /** Encerra a sessão (logout). */
  sair: () => void
  /** Atualiza só o Elo (ex.: depois de uma partida online mudar a pontuação). */
  atualizarElo: (elo: number) => void
}

const AuthContext = createContext<ContextoAuth | null>(null)

/** Lê a sessão do localStorage na primeira renderização (tolerante a JSON inválido). */
function lerSessao(): Autenticacao | null {
  try {
    return JSON.parse(localStorage.getItem('auth') ?? 'null') as Autenticacao | null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<Autenticacao | null>(lerSessao)

  // Toda mudança de sessão espelha no localStorage — assim ela sobrevive a um F5.
  function persistir(a: Autenticacao | null) {
    setAuth(a)
    if (a) localStorage.setItem('auth', JSON.stringify(a))
    else localStorage.removeItem('auth')
  }

  const valor: ContextoAuth = {
    auth,
    definir: (a) => persistir(a),
    sair: () => persistir(null),
    atualizarElo: (elo) =>
      setAuth((a) => {
        if (!a || a.elo === elo) return a // nada mudou
        const novo = { ...a, elo }
        localStorage.setItem('auth', JSON.stringify(novo))
        return novo
      }),
  }

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

/** Acesso à sessão em qualquer tela. Lança se usado fora do <AuthProvider>. */
export function useAuth(): ContextoAuth {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth precisa estar dentro de <AuthProvider>.')
  return ctx
}
