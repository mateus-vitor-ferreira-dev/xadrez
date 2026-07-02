import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { useAuth } from '../contexts/auth'
import { useSkin } from '../contexts/skin'
import { PecaSvg } from '../contexts/skin'
import { TEMAS, desbloqueado } from '../themes/skins'
import { TITULOS, tituloLiberado } from '../themes/titulos'
import { equiparTitulo } from '../lib/api'

// Hub de RECOMPENSAS (rota /skins): duas abas — "Peças" (skins do tabuleiro,
// client-side) e "Títulos" (o brinde público do caminho de troféus, salvo no
// servidor). Tudo é desbloqueado pelo rank; a conta admin libera tudo.

// Peças mostradas na prévia da skin: rei e peão de cada lado (contraste claro/escuro).
const PREVIA = ['wr', 'wp', 'bp', 'br']

export default function PaginaSkins() {
  const { auth, definir } = useAuth()
  const { tema: equipado, equipar } = useSkin()
  const meuElo = auth?.elo ?? 0
  const admin = auth?.admin ?? false
  const [aba, setAba] = useState<'pecas' | 'titulos'>('pecas')

  // Equipar título vai ao servidor (é público). Sucesso -> regrava a sessão.
  const equiparTit = useMutation({
    mutationFn: (id: string | null) => equiparTitulo(id),
    onSuccess: (novo) => definir(novo),
  })

  return (
    <div className="skins-page">
      <Link to="/" className="voltar-auth">
        ← Voltar ao jogo
      </Link>

      <div className="skins-cabecalho">
        <h2>🎁 Recompensas</h2>
        <p className="auth-sub">
          {admin
            ? '👑 Conta admin: skins e títulos todos liberados.'
            : auth
              ? `Skins e títulos são liberados conforme você sobe de rank (seu Elo: ${meuElo}).`
              : 'Skins e títulos são liberados conforme você sobe de rank. Faça login para desbloquear pelas suas vitórias.'}
        </p>
      </div>

      <div className="recompensas-abas">
        <button className={aba === 'pecas' ? 'ativo' : ''} onClick={() => setAba('pecas')}>
          🎨 Peças
        </button>
        <button className={aba === 'titulos' ? 'ativo' : ''} onClick={() => setAba('titulos')}>
          🏅 Títulos
        </button>
      </div>

      {aba === 'pecas' ? (
        <div className="skins-grid">
          {TEMAS.map((t) => {
            const liberado = desbloqueado(t, meuElo, admin)
            const ativo = t.id === equipado.id
            return (
              <div key={t.id} className={`skin-card${ativo ? ' equipado' : ''}${liberado ? '' : ' bloqueado'}`}>
                <div className="skin-previa">
                  {PREVIA.map((code) => (
                    <PecaSvg key={code} code={code} tema={t} className="skin-previa-peca" />
                  ))}
                </div>
                <div className="skin-info">
                  <strong>{t.nome}</strong>
                  <span className="skin-req">{t.eloMin === 0 ? 'Padrão' : `${t.rankNome} · Elo ${t.eloMin}`}</span>
                </div>
                {liberado ? (
                  <button
                    className={ativo ? 'skin-btn ativo' : 'skin-btn primario'}
                    disabled={ativo}
                    onClick={() => equipar(t.id)}
                  >
                    {ativo ? '✓ Equipado' : 'Equipar'}
                  </button>
                ) : (
                  <span className="skin-cadeado">🔒 {t.rankNome}</span>
                )}
              </div>
            )
          })}
        </div>
      ) : (
        <div className="titulos-grid">
          {!auth && <p className="titulos-aviso">Entre para desbloquear e equipar títulos.</p>}
          {equiparTit.isError && <p className="status alerta">Não foi possível equipar o título. Tente de novo.</p>}

          {/* Opção de não exibir título nenhum. */}
          <div className={`titulo-card${auth && !auth.titulo ? ' equipado' : ''}`}>
            <div className="titulo-nome">Sem título</div>
            <span className="skin-req">Padrão</span>
            <button
              className={auth && !auth.titulo ? 'skin-btn ativo' : 'skin-btn primario'}
              disabled={!auth || !auth.titulo || equiparTit.isPending}
              onClick={() => equiparTit.mutate(null)}
            >
              {auth && !auth.titulo ? '✓ Equipado' : 'Remover'}
            </button>
          </div>

          {TITULOS.map((t) => {
            const liberado = !!auth && tituloLiberado(t, meuElo, admin)
            const ativo = auth?.titulo === t.id
            return (
              <div key={t.id} className={`titulo-card${ativo ? ' equipado' : ''}${liberado ? '' : ' bloqueado'}`}>
                <div className="titulo-nome">🏅 {t.rotulo}</div>
                <span className="skin-req">{t.eloMin === 0 ? 'Padrão' : `${t.rank} · Elo ${t.eloMin}`}</span>
                {liberado ? (
                  <button
                    className={ativo ? 'skin-btn ativo' : 'skin-btn primario'}
                    disabled={ativo || equiparTit.isPending}
                    onClick={() => equiparTit.mutate(t.id)}
                  >
                    {ativo ? '✓ Equipado' : 'Equipar'}
                  </button>
                ) : (
                  <span className="skin-cadeado">🔒 {t.rank}</span>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
