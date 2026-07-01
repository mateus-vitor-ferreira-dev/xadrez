/**
 * Crachá do jogador logado (barra superior): um "cartão" com avatar (inicial do
 * usuário num círculo dourado), o nome e a pontuação Elo num chip dourado em
 * destaque — bem mais visual que o texto simples anterior.
 */
export default function PerfilUsuario({ usuario, elo }: { usuario: string; elo: number }) {
  const inicial = usuario.charAt(0).toUpperCase()
  return (
    <div className="perfil" title={`${usuario} · Elo ${elo}`}>
      <span className="perfil-avatar" aria-hidden="true">
        {inicial}
      </span>
      <span className="perfil-nome">{usuario}</span>
      <span className="perfil-elo">
        <span className="perfil-elo-rotulo">ELO</span>
        <span className="perfil-elo-valor">{elo}</span>
      </span>
    </div>
  )
}
