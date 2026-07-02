import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { Client } from '@stomp/stompjs'
import {
  buscarMovimentos,
  buscarPartida,
  entrar,
  jogadaIA,
  jogar,
  novaPartida,
  type Cor,
  type EstadoPartida,
  type TipoPromocao,
} from '../lib/api'
import Tabuleiro from '../components/Tabuleiro'
import CarrosselModos from '../components/CarrosselModos'
import PecaModo from '../components/PecaModo'
import TeaserRanking from '../components/TeaserRanking'
import BarraTopo from '../layouts/BarraTopo'
import { PecaSvg } from '../contexts/skin'
import { useAuth } from '../contexts/auth'
import { type Modo } from '../lib/modos'
import * as jogoCache from '../lib/jogoCache'
import { tocarSom } from '../lib/sons'

const LETRA_PROMOCAO: Record<TipoPromocao, string> = { RAINHA: 'd', TORRE: 't', BISPO: 'b', CAVALO: 'c' }
const TABULEIRO_INICIAL = 'TCBDRBCTPPPPPPPP................................pppppppptcbdrbct'
const INICIAIS: Record<string, number> = { p: 8, t: 2, c: 2, b: 2, d: 1, r: 1 }

function wsUrl(): string {
  const api = import.meta.env.VITE_API_URL
  return api ? `${api.replace(/^http/, 'ws')}/ws` : 'ws://localhost:8080/ws'
}

function diffLance(antes: string, depois: string): { origem: string; destino: string } | null {
  let origem = -1
  let destino = -1
  for (let i = 0; i < 64; i++) {
    if (antes[i] !== depois[i]) {
      if (depois[i] === '.') origem = i
      else destino = i
    }
  }
  if (origem < 0 || destino < 0) return null
  const not = (i: number) => String.fromCharCode(97 + (i % 8)) + (Math.floor(i / 8) + 1)
  return { origem: not(origem), destino: not(destino) }
}

/** Peças capturadas, deduzidas do tabuleiro (starting set - peças atuais). */
function capturadas(tab: string) {
  const conta = (branca: boolean) => {
    const c: Record<string, number> = {}
    for (const ch of tab) {
      if (ch === '.') continue
      if ((ch === ch.toUpperCase()) !== branca) continue
      const t = ch.toLowerCase()
      c[t] = (c[t] ?? 0) + 1
    }
    return c
  }
  const faltando = (branca: boolean) => {
    const atual = conta(branca)
    const out: string[] = []
    for (const [t, n] of Object.entries(INICIAIS)) {
      for (let i = 0; i < n - (atual[t] ?? 0); i++) out.push(t)
    }
    return out
  }
  const pretasCapturadas = faltando(false) // peças pretas que sumiram = brancas capturaram
  const brancasCapturadas = faltando(true) // peças brancas que sumiram = pretas capturaram
  // Vantagem material pela perspectiva das brancas (pontos de xadrez clássicos).
  const soma = (pecas: string[]) => pecas.reduce((s, t) => s + (VALOR_PECA[t] ?? 0), 0)
  const vantagem = soma(pretasCapturadas) - soma(brancasCapturadas)
  return { pretasCapturadas, brancasCapturadas, vantagem }
}

/** Valor material clássico (rei não conta). t=torre, c=cavalo, b=bispo, d=dama, p=peão. */
const VALOR_PECA: Record<string, number> = { p: 1, c: 3, b: 3, t: 5, d: 9, r: 0 }

function App() {
  const queryClient = useQueryClient()
  const [idPartida, setIdPartida] = useState<number | null>(null)
  const [selecionada, setSelecionada] = useState<string | null>(null)
  const [ultimoLance, setUltimoLance] = useState<{ origem: string; destino: string } | null>(null)
  const [promocaoPendente, setPromocaoPendente] = useState<{ origem: string; destino: string } | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [modo, setModo] = useState<Modo>('humano')
  const [nivel, setNivel] = useState(2)
  const [minhaCor, setMinhaCor] = useState<Cor | null>(null)
  const [conectado, setConectado] = useState(false)
  const [linkEntrada, setLinkEntrada] = useState('')
  const [historico, setHistorico] = useState<string[]>([])
  const [tema, setTema] = useState<'escuro' | 'claro'>(() => (localStorage.getItem('tema') === 'claro' ? 'claro' : 'escuro'))
  const [mudo, setMudo] = useState<boolean>(() => localStorage.getItem('mudo') === '1')
  // A sessão agora mora no AuthProvider (compartilhada com as telas /login e /registro).
  const { auth, atualizarElo } = useAuth()
  const navigate = useNavigate()

  const partida = useQuery({
    queryKey: ['partida', idPartida],
    queryFn: () => buscarPartida(idPartida!),
    enabled: idPartida !== null,
  })
  const estado = partida.data

  const movimentos = useQuery({
    queryKey: ['movimentos', idPartida, selecionada],
    queryFn: () => buscarMovimentos(idPartida!, selecionada!),
    enabled: idPartida !== null && selecionada !== null,
  })
  const destaques = movimentos.data ?? []

  const fimDeJogo = (e?: EstadoPartida) => !!e && (e.xequeMate || e.afogamento)

  // tema (claro/escuro) aplicado no <html> e persistido
  useEffect(() => {
    document.documentElement.dataset.tema = tema
    localStorage.setItem('tema', tema)
  }, [tema])
  useEffect(() => {
    localStorage.setItem('mudo', mudo ? '1' : '0')
  }, [mudo])

  // Quando uma partida online termina, o Elo do usuário mudou no backend:
  // reflete o novo valor no topo (e no localStorage).
  useEffect(() => {
    if (!estado || !auth || estado.resultado === 'EM_ANDAMENTO') return
    const meu = minhaCor === 'BRANCO' ? estado.eloBranco : estado.eloPreto
    if (meu != null) atualizarElo(meu) // atualizarElo já ignora se nada mudou
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [estado?.resultado, estado?.eloBranco, estado?.eloPreto])

  // Ao montar: retoma uma partida. Prioridade para o link ?partida= (convite
  // online); senão, restaura um jogo LOCAL/IA guardado em cache (sessionStorage).
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const pid = params.get('partida')
    if (pid) {
      // Jogar online exige conta: sem login, manda para a tela de login.
      if (!auth) {
        navigate('/login')
        return
      }
      const cor: Cor = params.get('cor') === 'branco' ? 'BRANCO' : 'PRETO'
      setIdPartida(Number(pid))
      setMinhaCor(cor)
      if (cor === 'PRETO') entrar(Number(pid)).catch(() => {}) // registra nas pretas (Elo)
      return
    }
    const cache = jogoCache.ler()
    if (cache) {
      setIdPartida(cache.id)
      setModo(cache.modo)
      setNivel(cache.nivel)
      setHistorico(cache.historico)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Mantém o cache do jogo local/IA em dia (sobrevive a um F5 na mesma aba).
  // Partidas ONLINE nunca entram no cache: elas têm um lado (minhaCor) definido e
  // voltam pela URL/link. Sem esse guard, uma partida online restaurada pela URL
  // (que deixa 'modo' no default 'humano') era gravada no cache e reaberta ao ir
  // para "/", prendendo o jogador no ciclo jogo↔lobby sem voltar ao menu.
  useEffect(() => {
    if (idPartida !== null && minhaCor === null && (modo === 'humano' || modo === 'ia')) {
      jogoCache.salvar({ id: idPartida, modo, nivel, historico })
    }
  }, [idPartida, minhaCor, modo, nivel, historico])

  // Se a partida restaurada do cache não existe mais no backend, descarta e
  // volta ao lobby (evita ficar preso numa tela de erro).
  useEffect(() => {
    if (partida.isError && idPartida !== null && minhaCor === null) {
      jogoCache.limpar()
      voltar()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [partida.isError])

  // Rede de segurança: partidas online NUNCA deveriam vir do cache (que é só
  // local/IA). Se uma cair aqui (minhaCor null porém a partida é online — ex.:
  // cache antigo gravado antes da correção), limpa e volta ao menu, quebrando o
  // ciclo jogo↔lobby que impedia de voltar ao início.
  useEffect(() => {
    if (estado?.online && minhaCor === null) {
      jogoCache.limpar()
      voltar()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [estado?.online, minhaCor])

  // Cada vez que o tabuleiro muda: registra o lance no histórico e toca o som.
  const boardAntesRef = useRef<string | null>(null)
  useEffect(() => {
    if (!estado) {
      boardAntesRef.current = null
      return
    }
    const antes = boardAntesRef.current
    if (antes && antes !== estado.tabuleiro) {
      const l = diffLance(antes, estado.tabuleiro)
      if (l) {
        setHistorico((h) => [...h, `${l.origem}-${l.destino}`])
        const dIdx = (Number(l.destino[1]) - 1) * 8 + (l.destino.charCodeAt(0) - 97)
        if (!mudo) tocarSom(antes[dIdx] !== '.' ? 'capturar' : 'mover')
      }
    }
    boardAntesRef.current = estado.tabuleiro
  }, [estado?.tabuleiro, mudo])

  // WebSocket (STOMP) — tempo real no modo online
  const clienteRef = useRef<Client | null>(null)
  useEffect(() => {
    if (idPartida === null || minhaCor === null) return
    const cliente = new Client({
      brokerURL: wsUrl(),
      reconnectDelay: 3000,
      onConnect: () => {
        setConectado(true)
        cliente.subscribe(`/topic/partidas/${idPartida}`, (msg) => {
          const novo = JSON.parse(msg.body) as EstadoPartida
          const antes = queryClient.getQueryData<EstadoPartida>(['partida', idPartida])
          queryClient.setQueryData(['partida', idPartida], novo)
          if (antes && antes.tabuleiro !== novo.tabuleiro) setUltimoLance(diffLance(antes.tabuleiro, novo.tabuleiro))
        })
      },
      onWebSocketClose: () => setConectado(false),
    })
    cliente.activate()
    clienteRef.current = cliente
    return () => {
      cliente.deactivate()
      setConectado(false)
    }
  }, [idPartida, minhaCor, queryClient])

  const iaMutation = useMutation({
    mutationFn: () => jogadaIA(idPartida!, nivel),
    onSuccess: (depois) => {
      const antes = queryClient.getQueryData<EstadoPartida>(['partida', idPartida])
      queryClient.setQueryData(['partida', idPartida], depois)
      if (antes) setUltimoLance(diffLance(antes.tabuleiro, depois.tabuleiro))
    },
  })

  const criar = useMutation({
    mutationFn: (online: boolean) => novaPartida(online),
    onSuccess: (e, online) => {
      setIdPartida(e.id)
      queryClient.setQueryData(['partida', e.id], e)
      setSelecionada(null)
      setUltimoLance(null)
      setPromocaoPendente(null)
      setErro(null)
      setHistorico([])
      boardAntesRef.current = null
      if (online) {
        setMinhaCor('BRANCO')
        window.history.replaceState(null, '', `?partida=${e.id}&cor=branco`)
      } else {
        setMinhaCor(null)
        window.history.replaceState(null, '', window.location.pathname)
      }
    },
  })

  const fazerJogada = useMutation({
    mutationFn: (j: { origem: string; destino: string; promocao?: TipoPromocao }) =>
      jogar(idPartida!, j.origem, j.destino, j.promocao),
    onSuccess: (e, vars) => {
      queryClient.setQueryData(['partida', idPartida], e)
      setUltimoLance({ origem: vars.origem, destino: vars.destino })
      setErro(null)
      if (modo === 'ia' && minhaCor === null && e.vez === 'PRETO' && !fimDeJogo(e)) iaMutation.mutate()
    },
    onError: (e) => setErro(e instanceof Error ? e.message : 'Jogada inválida'),
  })

  function pecaEm(notacao: string): string {
    if (!estado) return '.'
    return estado.tabuleiro[(Number(notacao[1]) - 1) * 8 + (notacao.charCodeAt(0) - 97)]
  }
  function temPecaDaVez(notacao: string): boolean {
    if (!estado) return false
    const ch = pecaEm(notacao)
    if (ch === '.') return false
    const branca = ch === ch.toUpperCase()
    return (branca && estado.vez === 'BRANCO') || (!branca && estado.vez === 'PRETO')
  }
  function ehPromocao(origem: string, destino: string): boolean {
    if (pecaEm(origem).toLowerCase() !== 'p') return false
    const f = Number(destino[1])
    return f === 8 || f === 1
  }
  function casaDoReiEmXeque(): string | null {
    if (!estado || !estado.xeque) return null
    const rei = estado.vez === 'BRANCO' ? 'R' : 'r'
    const idx = estado.tabuleiro.indexOf(rei)
    if (idx < 0) return null
    return String.fromCharCode(97 + (idx % 8)) + (Math.floor(idx / 8) + 1)
  }

  const vezDaIA = modo === 'ia' && minhaCor === null && estado?.vez === 'PRETO'
  const naoEhMinhaVez = minhaCor !== null && estado?.vez !== minhaCor

  function clicarCasa(notacao: string) {
    setErro(null)
    if (!estado || promocaoPendente || vezDaIA || iaMutation.isPending || naoEhMinhaVez) return
    if (selecionada === null) {
      if (temPecaDaVez(notacao)) setSelecionada(notacao)
    } else if (selecionada === notacao) {
      setSelecionada(null)
    } else {
      if (destaques.includes(notacao)) {
        if (ehPromocao(selecionada, notacao)) setPromocaoPendente({ origem: selecionada, destino: notacao })
        else fazerJogada.mutate({ origem: selecionada, destino: notacao })
      }
      setSelecionada(null)
    }
  }

  function escolherPromocao(tipo: TipoPromocao) {
    if (!promocaoPendente) return
    fazerJogada.mutate({ ...promocaoPendente, promocao: tipo })
    setPromocaoPendente(null)
  }

  function voltar() {
    jogoCache.limpar() // sair do jogo apaga o cache local/IA
    setIdPartida(null)
    setMinhaCor(null)
    setSelecionada(null)
    setUltimoLance(null)
    setPromocaoPendente(null)
    setErro(null)
    setHistorico([])
    window.history.replaceState(null, '', window.location.pathname)
  }

  /** Ação do botão principal do lobby — muda conforme o modo (e o login, no online). */
  function acaoPrincipal() {
    if (modo === 'tutorial') {
      // Tutorial não é partida: leva ao manual "como jogar".
      navigate('/como-jogar')
      return
    }
    if (modo === 'online') {
      // Jogar online exige conta. Sem login, vai para /login; com login, abre o
      // lobby de salas por Elo (onde dá para criar a sua ou entrar em uma).
      navigate(auth ? '/lobby' : '/login')
      return
    }
    criar.mutate(false) // humano ou IA: sem login
  }

  function entrarPorLink() {
    // Entrar numa partida online também exige conta.
    if (!auth) {
      navigate('/login')
      return
    }
    const txt = linkEntrada.trim()
    const m = txt.match(/partida=(\d+)/)
    const pid = m ? m[1] : /^\d+$/.test(txt) ? txt : null
    if (!pid) {
      setErro('Cole um link válido de partida online (ou o número da partida).')
      return
    }
    const cor: Cor = /cor=branco/.test(txt) ? 'BRANCO' : 'PRETO'
    setIdPartida(Number(pid))
    setMinhaCor(cor)
    if (cor === 'PRETO') entrar(Number(pid)).catch(() => {}) // registra o jogador nas pretas (Elo)
    window.history.replaceState(null, '', `?partida=${pid}&cor=${cor === 'BRANCO' ? 'branco' : 'preto'}`)
    setLinkEntrada('')
    setErro(null)
  }

  // Rótulo do botão principal conforme o modo/login.
  const rotuloAcao = criar.isPending
    ? 'Criando…'
    : modo === 'tutorial'
      ? '📖 Abrir tutorial'
      : modo === 'online'
        ? auth
          ? 'Jogar online'
          : 'Entrar para jogar online'
        : 'Começar partida'

  const linkConvite =
    idPartida && minhaCor === 'BRANCO'
      ? `${window.location.origin}${window.location.pathname}?partida=${idPartida}&cor=preto`
      : null
  const emJogo = idPartida !== null
  const caps = estado ? capturadas(estado.tabuleiro) : null

  // Perspectiva do jogador logado numa partida online (Fase 4 / Elo).
  const souBranco = minhaCor === 'BRANCO'
  const meuElo = estado ? (souBranco ? estado.eloBranco : estado.eloPreto) : null
  const meuDelta = estado ? (souBranco ? estado.deltaBranco : estado.deltaPreto) : null
  const advNome = estado ? (souBranco ? estado.preto : estado.branco) : null
  const advElo = estado ? (souBranco ? estado.eloPreto : estado.eloBranco) : null

  return (
    <div className="app">
      <BarraTopo
        tema={tema}
        onToggleTema={() => setTema((t) => (t === 'escuro' ? 'claro' : 'escuro'))}
        mudo={mudo}
        onToggleMudo={() => setMudo((m) => !m)}
      />

      {/* Na tela inicial o cabeçalho é o "hero" grande; dentro do jogo ele encolhe
          (sem tagline) para o tabuleiro caber na tela sem rolar. */}
      <header className={emJogo ? 'topo topo-compacto' : 'topo'}>
        <h1>♞ Xadrez</h1>
        {!emJogo && <p className="tagline">Local · contra a IA · online em tempo real</p>}
      </header>

      {!emJogo ? (
        <div className="lobby">
          {/* "Cena": carrossel de modos à esquerda e a peça-hero à direita,
              centralizados juntos (estilo seleção de personagem). */}
          <div className="lobby-cena">
            <CarrosselModos ativo={modo} onAtivoChange={setModo} />
            <PecaModo modo={modo} />
          </div>

          {/* Ações abaixo da cena: nível (IA), botão principal e entrar por convite. */}
          <div className="lobby-acoes">
            {modo === 'ia' && (
              <div className="niveis">
                <span>Nível:</span>
                {[1, 2, 3].map((n) => (
                  <button key={n} className={nivel === n ? 'ativo' : ''} onClick={() => setNivel(n)}>
                    {n}
                  </button>
                ))}
              </div>
            )}

            <button className="primario grande" onClick={acaoPrincipal} disabled={criar.isPending}>
              {rotuloAcao}
            </button>

            {modo !== 'tutorial' && (
              <div className="entrar-link">
                <span>Recebeu um convite?</span>
                <div className="linha">
                  <input
                    placeholder="Cole o link da partida online…"
                    value={linkEntrada}
                    onChange={(e) => setLinkEntrada(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && entrarPorLink()}
                  />
                  <button onClick={entrarPorLink}>Entrar</button>
                </div>
              </div>
            )}

            {erro && <p className="status alerta">{erro}</p>}

            {/* Onboarding: quem nunca jogou vai direto para o manual. */}
            <Link to="/como-jogar" className="como-jogar-link">
              📖 Primeira vez? Aprenda a jogar
            </Link>

            {/* Chamariz: mini-pódio do site + CTA pro online. Some sozinho se
                ainda não há ninguém pontuando (ver TeaserRanking). */}
            <TeaserRanking />
          </div>
        </div>
      ) : (
        <div className="jogo">
          {/* Num jogo online voltamos para o LOBBY (achar outro oponente); num
              jogo local, para o menu inicial. */}
          <button className="voltar" onClick={estado?.online ? () => navigate('/lobby') : voltar}>
            {estado?.online ? '← Voltar às salas' : '← Voltar ao início'}
          </button>

          {estado && (
            <p className="status">
              Vez das <strong>{estado.vez}</strong>
              {iaMutation.isPending && <span> — IA pensando…</span>}
              {naoEhMinhaVez && !fimDeJogo(estado) && <span> — aguardando o oponente…</span>}
              {!estado.xequeMate && estado.xeque && <span className="alerta"> — xeque!</span>}
            </p>
          )}

          <div className="jogo-area">
            {/* ESQUERDA: info da partida online (ao lado do tabuleiro, não mais
                empurrando-o pra baixo) + histórico recolhível. */}
            <aside className="painel painel-esq">
              {minhaCor !== null && (
                <div className="online-info">
                  <p>
                    Você joga de <strong>{minhaCor}</strong>{' '}
                    <span className={conectado ? 'ok' : 'off'}>{conectado ? '🟢 tempo real' : '🔌 conectando…'}</span>
                  </p>
                  {estado?.online &&
                    (advNome ? (
                      <p className="adversario">
                        Adversário: <strong>{advNome}</strong>
                        {advElo != null && ` · Elo ${advElo}`}
                      </p>
                    ) : (
                      <p className="adversario aguardando">Aguardando o adversário entrar…</p>
                    ))}
                  {linkConvite && (
                    <p className="convite">
                      Convide o oponente (joga de PRETO):
                      <input readOnly value={linkConvite} onFocus={(e) => e.currentTarget.select()} />
                    </p>
                  )}
                </div>
              )}
              <details className="historico" open={historico.length > 0}>
                <summary>
                  <span>Histórico de lances</span>
                  <span className="contador">{historico.length}</span>
                </summary>
                {historico.length === 0 ? (
                  <p className="vazio">Sem lances ainda.</p>
                ) : (
                  <>
                    <div className="historico-cab">
                      <span>Brancas</span>
                      <span>Pretas</span>
                    </div>
                    <ol>
                      {Array.from({ length: Math.ceil(historico.length / 2) }, (_, i) => (
                        <li key={i}>
                          <span className="lance lance-b">{historico[i * 2]}</span>
                          <span className="lance lance-p">{historico[i * 2 + 1] ?? ''}</span>
                        </li>
                      ))}
                    </ol>
                  </>
                )}
              </details>
            </aside>

            {/* CENTRO: tabuleiro (inalterado). */}
            <div className="tabuleiro-wrap">
              <Tabuleiro
                tabuleiro={estado?.tabuleiro ?? TABULEIRO_INICIAL}
                selecionada={selecionada}
                destaques={destaques}
                casaXeque={casaDoReiEmXeque()}
                ultimoLance={ultimoLance}
                onClicarCasa={clicarCasa}
                girar={minhaCor === 'PRETO'}
              />
            </div>

            {/* DIREITA: material capturado, em pilha sobreposta por lado. */}
            <aside className="painel painel-dir">
              {caps && (caps.pretasCapturadas.length > 0 || caps.brancasCapturadas.length > 0) && (
                <div className="material">
                  {caps.pretasCapturadas.length > 0 && (
                    <div className="captura-lado">
                      <span className="captura-rotulo">Brancas</span>
                      <div className="captura-pilha" title="Peças capturadas pelas brancas">
                        {caps.pretasCapturadas.map((t, i) => (
                          <PecaSvg key={`p${i}`} code={`b${t}`} className="captura-peca" />
                        ))}
                        {caps.vantagem > 0 && <span className="vantagem">+{caps.vantagem}</span>}
                      </div>
                    </div>
                  )}
                  {caps.brancasCapturadas.length > 0 && (
                    <div className="captura-lado">
                      <span className="captura-rotulo">Pretas</span>
                      <div className="captura-pilha" title="Peças capturadas pelas pretas">
                        {caps.brancasCapturadas.map((t, i) => (
                          <PecaSvg key={`b${i}`} code={`w${t}`} className="captura-peca" />
                        ))}
                        {caps.vantagem < 0 && <span className="vantagem">+{-caps.vantagem}</span>}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </aside>
          </div>

          {erro && <p className="status alerta">{erro}</p>}

          {promocaoPendente && estado && (
            <div className="modal-promocao">
              <p>Promover para:</p>
              <div className="opcoes">
                {(Object.keys(LETRA_PROMOCAO) as TipoPromocao[]).map((tipo) => (
                  <button key={tipo} onClick={() => escolherPromocao(tipo)} title={tipo}>
                    <PecaSvg code={`${estado.vez === 'BRANCO' ? 'w' : 'b'}${LETRA_PROMOCAO[tipo]}`} className="peca-opcao" />
                  </button>
                ))}
              </div>
            </div>
          )}

          {fimDeJogo(estado) && estado && (
            <div className="fim">
              <p>
                {estado.xequeMate
                  ? `Xeque-mate! Vencem as ${estado.vez === 'BRANCO' ? 'PRETAS' : 'BRANCAS'}.`
                  : 'Afogamento — empate.'}
              </p>
              {estado.online && meuDelta != null && (
                <p className="elo-delta">
                  Seu Elo: <strong>{meuElo}</strong>{' '}
                  <span className={meuDelta >= 0 ? 'sobe' : 'desce'}>
                    ({meuDelta >= 0 ? '+' : ''}
                    {meuDelta})
                  </span>
                </p>
              )}
              <button onClick={estado.online ? () => navigate('/lobby') : voltar}>
                {estado.online ? 'Voltar às salas' : 'Voltar ao início'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default App
