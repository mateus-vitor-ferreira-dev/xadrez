import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { buscarRanking } from './api'
import { useAuth } from './auth'

// Chamariz na tela inicial: um mini-pódio com o Top 3 do site + um botão que leva
// ao jogo online. Some do caminho de quem só quer jogar local/IA (é um card
// pequeno), mas planta a ideia "existe ranking, vale competir". Reaproveita o
// mesmo /ranking das tabelas do lobby — visível até sem login (estímulo).

const MEDALHAS = ['🥇', '🥈', '🥉']

export default function TeaserRanking() {
  const { auth } = useAuth()
  const navigate = useNavigate()

  const ranking = useQuery({ queryKey: ['ranking', auth?.usuario], queryFn: buscarRanking })

  // Só os 3 primeiros do site. Sem ninguém pontuando ainda, não mostramos nada
  // (um pódio vazio desestimularia em vez de estimular).
  const top3 = (ranking.data?.topSite ?? []).slice(0, 3)
  if (top3.length === 0) return null

  return (
    <div className="teaser-ranking">
      <span className="teaser-titulo">🏆 Top do site</span>
      <ol className="teaser-lista">
        {top3.map((l, i) => (
          <li key={l.usuario}>
            <span className="teaser-medalha">{MEDALHAS[i]}</span>
            <strong className="teaser-nome">{l.usuario}</strong>
            <span className="teaser-elo">{l.elo}</span>
          </li>
        ))}
      </ol>
      <button className="teaser-cta" onClick={() => navigate(auth ? '/lobby' : '/login')}>
        Jogar online e entrar no ranking →
      </button>
    </div>
  )
}
