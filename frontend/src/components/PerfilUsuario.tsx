/**
 * Crachá do jogador logado (barra superior): um "cartão" com avatar (inicial do
 * usuário num círculo dourado), o nome e a pontuação Elo num chip dourado em
 * destaque — bem mais visual que o texto simples anterior.
 */
export default function PerfilUsuario({
  usuario,
  elo,
  titulo,
}: {
  usuario: string
  elo: number
  /** Rótulo do título equipado (ex.: 'Cavaleiro'); ausente = sem título. */
  titulo?: string | null
}) {
  const inicial = usuario.charAt(0).toUpperCase()
  return (
    <div className="perfil" title={`${usuario}${titulo ? ` · ${titulo}` : ''} · Elo ${elo}`}>
      <span className="perfil-avatar" aria-hidden="true">
        {inicial}
      </span>
      <span className="perfil-nome">
        <span className="perfil-usuario">{usuario}</span>
        {titulo && <span className="perfil-titulo">{titulo}</span>}
      </span>
      <span className="perfil-elo">
        <span className="perfil-elo-rotulo">ELO</span>
        <span className="perfil-elo-valor">{elo}</span>
      </span>
    </div>
  )
}
