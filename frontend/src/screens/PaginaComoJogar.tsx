import { Link } from 'react-router-dom'
import Tabuleiro from '../components/Tabuleiro'
import { PecaSvg } from '../contexts/skin'
import { RANKS } from '../themes/ranks'

// MANUAL "Como jogar" (rota /como-jogar): explica o xadrez para quem nunca jogou,
// reusando o próprio componente Tabuleiro para os diagramas — cada peça aparece
// num mini-tabuleiro com os lances possíveis destacados. Puramente informativo.

// Índice no vetor de 64 casas a partir da notação ('d4' -> (4-1)*8 + 3).
const idx = (n: string) => (Number(n[1]) - 1) * 8 + (n.charCodeAt(0) - 97)

/** Monta a string de 64 casas a partir de um mapa notação -> letra da peça. */
function montar(pecas: Record<string, string>): string {
  const arr: string[] = Array(64).fill('.')
  for (const [casa, ch] of Object.entries(pecas)) arr[idx(casa)] = ch
  return arr.join('')
}

/** Mini-tabuleiro estático (não clicável) usado como diagrama. */
function Diagrama({
  pecas,
  foco = null,
  destaques = [],
  xeque = null,
}: {
  pecas: Record<string, string>
  foco?: string | null
  destaques?: string[]
  xeque?: string | null
}) {
  return (
    <div className="diagrama">
      <Tabuleiro
        tabuleiro={montar(pecas)}
        selecionada={foco}
        destaques={destaques}
        casaXeque={xeque}
        ultimoLance={null}
        onClicarCasa={() => {}}
      />
    </div>
  )
}

// Posição inicial (mesma convenção do jogo: T torre, C cavalo, B bispo, D dama,
// R rei, P peão — maiúscula = brancas).
const INICIAL = 'TCBDRBCT' + 'PPPPPPPP' + '.'.repeat(32) + 'pppppppp' + 'tcbdrbct'

// Lances de cada peça a partir de d4 (casa central), para os diagramas.
const TORRE = ['d1', 'd2', 'd3', 'd5', 'd6', 'd7', 'd8', 'a4', 'b4', 'c4', 'e4', 'f4', 'g4', 'h4']
const BISPO = ['e5', 'f6', 'g7', 'h8', 'c5', 'b6', 'a7', 'e3', 'f2', 'g1', 'c3', 'b2', 'a1']
const CAVALO = ['e6', 'e2', 'c6', 'c2', 'f5', 'f3', 'b5', 'b3']
const REI = ['c3', 'c4', 'c5', 'd3', 'd5', 'e3', 'e4', 'e5']
const DAMA = [...TORRE, ...BISPO]

const PECAS = [
  {
    ch: 'P',
    casa: 'e2',
    extra: { d3: 'p', f3: 'p' },
    destaques: ['e3', 'e4', 'd3', 'f3'],
    nome: 'Peão',
    texto: 'Anda uma casa para frente (duas no seu primeiro lance) e captura na diagonal. Nunca anda para trás.',
  },
  { ch: 'T', casa: 'd4', destaques: TORRE, nome: 'Torre', texto: 'Move-se em linha reta pelas colunas e linhas, quantas casas quiser.' },
  { ch: 'B', casa: 'd4', destaques: BISPO, nome: 'Bispo', texto: 'Move-se pelas diagonais. Cada bispo fica sempre na mesma cor de casa.' },
  { ch: 'C', casa: 'd4', destaques: CAVALO, nome: 'Cavalo', texto: 'Move-se em "L" (duas casas + uma). É a única peça que pula por cima das outras.' },
  { ch: 'D', casa: 'd4', destaques: DAMA, nome: 'Dama', texto: 'Combina torre e bispo: linhas, colunas e diagonais. A peça mais poderosa.' },
  { ch: 'R', casa: 'd4', destaques: REI, nome: 'Rei', texto: 'Anda uma casa em qualquer direção. É a peça que você precisa proteger a todo custo.' },
]

// Saídas ("aberturas") famosas: cada uma é uma sequência de lances [de, para]
// aplicada à posição inicial, para o diagrama mostrar como o tabuleiro fica.
const SAIDAS = [
  {
    nome: 'Abertura Italiana',
    lances: [['e2', 'e4'], ['e7', 'e5'], ['g1', 'f3'], ['b8', 'c6'], ['f1', 'c4']],
    notacao: '1.e4 e5 2.Cf3 Cc6 3.Bc4',
    ideia: 'Clássica e direta: ocupa o centro, desenvolve as peças e já mira o ponto fraco f7.',
  },
  {
    nome: 'Ruy López (Espanhola)',
    lances: [['e2', 'e4'], ['e7', 'e5'], ['g1', 'f3'], ['b8', 'c6'], ['f1', 'b5']],
    notacao: '1.e4 e5 2.Cf3 Cc6 3.Bb5',
    ideia: 'O bispo pressiona o cavalo que defende o peão e5. Uma das aberturas mais estudadas da história.',
  },
  {
    nome: 'Defesa Siciliana',
    lances: [['e2', 'e4'], ['c7', 'c5']],
    notacao: '1.e4 c5',
    ideia: 'A resposta mais combativa a 1.e4: as pretas disputam o centro de forma assimétrica, jogando para ganhar.',
  },
  {
    nome: 'Gambito da Dama',
    lances: [['d2', 'd4'], ['d7', 'd5'], ['c2', 'c4']],
    notacao: '1.d4 d5 2.c4',
    ideia: 'As brancas oferecem um peão de flanco para, em troca, dominar o centro do tabuleiro.',
  },
]

export default function PaginaComoJogar() {
  return (
    <div className="como-jogar-page">
      <Link to="/" className="voltar-auth">
        ← Voltar ao jogo
      </Link>

      <header className="manual-cabecalho">
        <h1>♞ Como jogar xadrez</h1>
        <p className="auth-sub">Um guia rápido para quem está começando. Em poucos minutos você já joga sua primeira partida.</p>
      </header>

      <section className="manual-secao">
        <h2>🎯 O objetivo</h2>
        <p>
          O xadrez é disputado por dois jogadores, <strong>brancas</strong> e <strong>pretas</strong>, num tabuleiro de 8×8.
          As brancas começam. O objetivo é dar <strong>xeque-mate</strong> no rei adversário — deixá-lo sob ataque sem
          nenhuma forma de escapar.
        </p>
      </section>

      <section className="manual-secao">
        <h2>♟️ A posição inicial</h2>
        <p>
          Cada lado começa com 8 peões, 2 torres, 2 cavalos, 2 bispos, 1 dama e 1 rei. As colunas são nomeadas de
          <strong> a</strong> a <strong>h</strong> e as linhas de <strong>1</strong> a <strong>8</strong> — é assim que
          se anota cada casa (ex.: <em>e4</em>).
        </p>
        <Diagrama pecas={objetoInicial()} />
      </section>

      <section className="manual-secao">
        <h2>🧩 As peças e seus movimentos</h2>
        <p>A casa dourada é a peça; os círculos mostram para onde ela pode ir (o anel indica uma captura).</p>
        <div className="manual-pecas">
          {PECAS.map((p) => (
            <div key={p.nome} className="manual-peca">
              <Diagrama pecas={{ [p.casa]: p.ch, ...(p.extra ?? {}) }} foco={p.casa} destaques={p.destaques} />
              <div className="manual-peca-info">
                <strong>{p.nome}</strong>
                <span>{p.texto}</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="manual-secao">
        <h2>✨ Lances especiais</h2>

        <div className="manual-especial">
          <Diagrama pecas={{ e1: 'R', a1: 'T', h1: 'T' }} foco="e1" destaques={['g1', 'c1']} />
          <div className="manual-peca-info">
            <strong>Roque</strong>
            <span>
              Rei e torre num só lance: o rei anda duas casas em direção a uma torre e ela salta para o outro lado. Só
              vale se nem o rei nem a torre já se moveram, não há peças no caminho e o rei não está (nem passa) em xeque.
            </span>
          </div>
        </div>

        <div className="manual-especial">
          <Diagrama pecas={{ e5: 'P', d5: 'p' }} foco="e5" destaques={['d6']} />
          <div className="manual-peca-info">
            <strong>En passant ("de passagem")</strong>
            <span>
              Se um peão adversário avança duas casas e para ao lado do seu peão, você pode capturá-lo como se ele
              tivesse andado só uma — mas apenas no lance imediatamente seguinte.
            </span>
          </div>
        </div>

        <div className="manual-especial">
          <Diagrama pecas={{ e7: 'P' }} foco="e7" destaques={['e8']} />
          <div className="manual-peca-info">
            <strong>Promoção</strong>
            <span>
              Um peão que chega à última fileira vira outra peça — quase sempre uma dama, a mais forte:
            </span>
            <div className="manual-promocao">
              {['wd', 'wt', 'wb', 'wc'].map((c) => (
                <PecaSvg key={c} code={c} className="manual-promocao-peca" />
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="manual-secao">
        <h2>🏁 Fim de jogo</h2>

        <div className="manual-especial">
          <Diagrama pecas={{ e8: 'r', e1: 'T' }} xeque="e8" />
          <div className="manual-peca-info">
            <strong>Xeque</strong>
            <span>O rei está sob ataque (casa em vermelho). Você é obrigado a sair do xeque no próximo lance.</span>
          </div>
        </div>

        <div className="manual-especial">
          <Diagrama pecas={{ g8: 'r', f7: 'p', g7: 'p', h7: 'p', e8: 'T' }} xeque="g8" />
          <div className="manual-peca-info">
            <strong>Xeque-mate</strong>
            <span>O rei está em xeque e não há como escapar, bloquear ou capturar o atacante. Fim de jogo — quem deu o mate vence.</span>
          </div>
        </div>

        <div className="manual-especial">
          <Diagrama pecas={{ h8: 'r', f7: 'R', g6: 'D' }} />
          <div className="manual-peca-info">
            <strong>Afogamento (empate)</strong>
            <span>
              Quando é sua vez, seu rei <em>não</em> está em xeque, mas você não tem nenhum lance legal. Isso é um empate
              por afogamento — cuidado ao ter vantagem para não deixar o adversário sem jogadas!
            </span>
          </div>
        </div>
      </section>

      <section className="manual-secao">
        <h2>♜ Saídas famosas</h2>
        <p>
          Os primeiros lances de uma partida são a <strong>abertura</strong>. Reconhecer algumas ajuda a saber o que
          fazer no começo — todas seguem a mesma ideia: <strong>ocupar o centro</strong> e <strong>desenvolver as peças</strong>.
        </p>
        <div className="manual-pecas">
          {SAIDAS.map((s) => (
            <div key={s.nome} className="manual-peca">
              <Diagrama pecas={posicaoApos(s.lances)} />
              <div className="manual-peca-info">
                <strong>{s.nome}</strong>
                <span className="manual-saida-lances">{s.notacao}</span>
                <span>{s.ideia}</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="manual-secao">
        <h2>💡 Dicas para começar bem</h2>
        <ul className="manual-dicas">
          <li>
            <strong>Controle o centro.</strong> As casas centrais (e4, d4, e5, d5) dão mobilidade às suas peças.
          </li>
          <li>
            <strong>Desenvolva cedo.</strong> Tire cavalos e bispos para casas ativas; evite mover a mesma peça várias
            vezes na abertura.
          </li>
          <li>
            <strong>Proteja o rei.</strong> Faça o <em>roque</em> logo para deixá-lo seguro num canto.
          </li>
          <li>
            <strong>Não exponha a dama cedo.</strong> Ela vira alvo fácil e você perde tempo fugindo com ela.
          </li>
          <li>
            <strong>Antes de capturar, conte.</strong> Em cada troca, veja se você não fica perdendo material depois.
          </li>
        </ul>
      </section>

      <section className="manual-secao">
        <h2>📈 Partidas ranqueadas e Elo</h2>
        <p>
          Partidas <strong>online</strong> entre dois jogadores logados são ranqueadas: você ganha ou perde pontos de
          <strong> Elo</strong> conforme o resultado (e conforme a força do adversário). Seu Elo define sua
          <strong> faixa de rank</strong>, que também libera novas <Link to="/skins">skins de peças</Link>:
        </p>
        <div className="rank-faixas">
          {RANKS.map((r) => (
            <div key={r.id} className="rank-faixa">
              <strong>{r.rotulo}</strong>
              <span>{r.eloMin === 0 ? 'a partir de 0' : `Elo ${r.eloMin}+`}</span>
            </div>
          ))}
        </div>
      </section>

      <div className="manual-cta">
        <Link to="/" className="primario grande manual-cta-botao">
          Começar a jogar →
        </Link>
      </div>
    </div>
  )
}

// A posição inicial como mapa notação -> peça (para o Diagrama).
function objetoInicial(): Record<string, string> {
  const pecas: Record<string, string> = {}
  for (let i = 0; i < 64; i++) {
    if (INICIAL[i] === '.') continue
    const casa = String.fromCharCode(97 + (i % 8)) + (Math.floor(i / 8) + 1)
    pecas[casa] = INICIAL[i]
  }
  return pecas
}

// Posição depois de uma sequência de lances [de, para] a partir do início. Só
// move a peça de uma casa para outra (as saídas aqui não têm capturas), o que
// basta para os diagramas das aberturas.
function posicaoApos(lances: string[][]): Record<string, string> {
  const pecas = objetoInicial()
  for (const [de, para] of lances) {
    pecas[para] = pecas[de]
    delete pecas[de]
  }
  return pecas
}
