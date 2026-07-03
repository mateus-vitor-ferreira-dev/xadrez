import { useEffect, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { atualizarPerfil, buscarPerfil, type Perfil } from '../lib/api'
import { useAuth } from '../contexts/auth'

/**
 * Tela de PERFIL (rota /perfil): editar e-mail, telefone e foto do usuário logado.
 * O apelido (público) aparece só de leitura; a senha não muda por aqui.
 *
 * Fluxo: carrega os dados atuais (GET /usuario/perfil) para preencher o form;
 * salvar dispara o PUT e, ao dar certo, espelha o e-mail na sessão (auth) para a
 * barra de topo e o ranking ficarem coerentes sem exigir novo login.
 */
export default function PaginaPerfil() {
  const { auth, definir } = useAuth()
  const navigate = useNavigate()

  // Editar perfil exige conta: sem login, manda para o /login.
  useEffect(() => {
    if (!auth) navigate('/login')
  }, [auth, navigate])

  // Dados atuais para preencher o formulário. Depende do usuário na queryKey para
  // recarregar ao trocar de conta.
  const perfil = useQuery({
    queryKey: ['perfil', auth?.usuario],
    queryFn: buscarPerfil,
    enabled: !!auth,
  })

  // Estado dos campos editáveis. Só existe depois que o perfil carrega (ver effect).
  const [email, setEmail] = useState('')
  const [telefone, setTelefone] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [salvo, setSalvo] = useState(false)

  // Assim que o perfil chega, preenche os campos (uma vez por carga).
  useEffect(() => {
    if (perfil.data) {
      setEmail(perfil.data.email)
      setTelefone(perfil.data.telefone ?? '')
    }
  }, [perfil.data])

  const salvar = useMutation({
    mutationFn: (): Promise<Perfil> =>
      atualizarPerfil({
        email: email.trim(),
        // Telefone é opcional: vazio vira null (limpa no back).
        telefone: telefone.trim() || null,
      }),
    onSuccess: (p) => {
      setErro(null)
      setSalvo(true)
      // Espelha o e-mail (que pode ter mudado) na sessão guardada.
      if (auth) definir({ ...auth, email: p.email })
    },
    onError: (e) => {
      setSalvo(false)
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar o perfil.')
    },
  })

  function enviar() {
    // Validação leve no cliente; o back revalida formato e unicidade.
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setSalvo(false)
      setErro('Informe um e-mail válido.')
      return
    }
    setErro(null)
    salvar.mutate()
  }

  // Enquanto não há sessão, o effect acima já está redirecionando.
  if (!auth) return null

  const inicial = auth.usuario.charAt(0).toUpperCase()

  return (
    <div className="auth-page">
      <div className="auth-box perfil-box">
        <Link to="/" className="voltar-auth">
          ← Voltar ao jogo
        </Link>

        <h2>Meu perfil</h2>
        <p className="auth-sub">Atualize suas informações. O apelido e a senha não mudam por aqui.</p>

        {perfil.isLoading ? (
          <p className="status">Carregando…</p>
        ) : perfil.isError ? (
          <p className="status alerta">Não foi possível carregar seu perfil. Recarregue a página.</p>
        ) : (
          <>
            {/* Avatar-inicial dourado (decorativo), igual ao da barra de topo. */}
            <div className="perfil-foto-previa">
              <span className="perfil-avatar-grande" aria-hidden="true">
                {inicial}
              </span>
            </div>

            {/* Apelido: só leitura (troca de apelido está fora deste escopo). */}
            <label className="perfil-campo">
              <span>Apelido</span>
              <input value={auth.usuario} disabled readOnly />
            </label>

            <label className="perfil-campo">
              <span>E-mail</span>
              <input
                type="email"
                placeholder="voce@exemplo.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>

            <label className="perfil-campo">
              <span>Telefone (opcional)</span>
              <input
                type="tel"
                placeholder="(11) 90000-0000"
                value={telefone}
                onChange={(e) => setTelefone(e.target.value)}
              />
            </label>

            <button className="primario" onClick={enviar} disabled={salvar.isPending}>
              {salvar.isPending ? 'Salvando…' : 'Salvar alterações'}
            </button>

            {salvo && !salvar.isPending && <p className="status ok">✓ Perfil atualizado.</p>}
            {erro && <p className="status alerta">{erro}</p>}
          </>
        )}
      </div>
    </div>
  )
}
