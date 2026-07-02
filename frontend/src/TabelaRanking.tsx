import type { LinhaRanking } from './api'

// Tabela de pontuadores reutilizada nos DOIS lados do lobby: só mudam o título,
// as linhas e (opcionalmente) o subtítulo. Componente puramente apresentacional
// — quem busca os dados é a PaginaLobby; aqui só desenhamos.

interface Props {
  titulo: string
  subtitulo?: string
  linhas: LinhaRanking[]
  /** Apelido do usuário logado: destaca a própria linha, se ela aparecer. */
  destaque?: string
}

export default function TabelaRanking({ titulo, subtitulo, linhas, destaque }: Props) {
  return (
    <aside className="ranking-tabela">
      <div className="ranking-cabecalho">
        <h3>{titulo}</h3>
        {subtitulo && <span className="ranking-sub">{subtitulo}</span>}
      </div>

      {linhas.length === 0 ? (
        <p className="ranking-vazio">Ninguém por aqui ainda.</p>
      ) : (
        <ol className="ranking-lista">
          {linhas.map((l, i) => (
            <li
              key={l.usuario}
              className={l.usuario === destaque ? 'ranking-linha eu' : 'ranking-linha'}
            >
              <span className="ranking-pos">{i + 1}</span>
              <span className="ranking-nome">
                <strong>{l.usuario}</strong>
                <span className="ranking-rank">{l.rank}</span>
              </span>
              <span className="ranking-elo">{l.elo}</span>
            </li>
          ))}
        </ol>
      )}
    </aside>
  )
}
