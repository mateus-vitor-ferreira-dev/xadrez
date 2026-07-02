package com.mateusferreira.xadrez.seguranca;

import com.mateusferreira.xadrez.dominio.Titulo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuário da plataforma. A senha é guardada como HASH (BCrypt), nunca em texto.
 * O campo 'elo' já nasce aqui (padrão 1200) para o sistema de pontuação (Fase 4).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Apelido público e único (aparece nas partidas / ranking). */
    @Column(nullable = false, unique = true, length = 30)
    private String usuario;

    /** E-mail único — serve para login (junto com o apelido) e contato. */
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Hash BCrypt da senha (nunca a senha em texto puro). */
    @Column(nullable = false)
    private String senha;

    /** Elo inicial: todo mundo começa na faixa Iniciante e sobe jogando. */
    public static final int ELO_INICIAL = 800;

    @Column(nullable = false)
    private int elo = ELO_INICIAL;

    /**
     * Quantas partidas ranqueadas (online, entre 2 logados) o jogador já
     * disputou. Enquanto for baixo, o Elo usa um K maior ("provisório"), para o
     * jogador chegar rápido ao seu nível real. Ver {@code CalculadoraElo}.
     */
    @Column(nullable = false)
    private int jogosRanqueados = 0;

    /**
     * Vitórias consecutivas em partidas ranqueadas. Emendar vitórias dá um bônus
     * modesto de Elo (ver {@code CalculadoraElo.bonusStreak}). Zera ao empatar ou perder.
     */
    @Column(nullable = false)
    private int vitoriasSeguidas = 0;

    /**
     * Papel da conta. A coluna é NULLABLE de propósito: com {@code ddl-auto=update}
     * numa base que já tem usuários, adicionar uma coluna NOT NULL sem default
     * quebraria a migração. Linhas antigas ficam NULL e {@link #getRole()} as trata
     * como {@link Role#USER}. Contas novas nascem USER (default do campo abaixo).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role = Role.USER;

    /**
     * Título exibido ao lado do apelido (ranking/perfil). NULL = nenhum. Só pode
     * ser um dos títulos desbloqueados pelo Elo — a checagem é feita ao equipar
     * (ver {@code UsuarioController}). Coluna nullable pelo mesmo motivo do role.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Titulo tituloEquipado;

    protected Usuario() {
    }

    public Usuario(String usuario, String email, String senhaHash) {
        this.usuario = usuario;
        this.email = email;
        this.senha = senhaHash;
        this.elo = ELO_INICIAL;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getJogosRanqueados() {
        return jogosRanqueados;
    }

    public void setJogosRanqueados(int jogosRanqueados) {
        this.jogosRanqueados = jogosRanqueados;
    }

    public int getVitoriasSeguidas() {
        return vitoriasSeguidas;
    }

    public void setVitoriasSeguidas(int vitoriasSeguidas) {
        this.vitoriasSeguidas = vitoriasSeguidas;
    }

    /** Papel da conta; NULL (linhas antigas) conta como {@link Role#USER}. */
    public Role getRole() {
        return role == null ? Role.USER : role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    /** Atalho: {@code true} se a conta é administradora (acesso total). */
    public boolean isAdmin() {
        return getRole() == Role.ADMIN;
    }

    /** Título equipado, ou {@code null} se nenhum. */
    public Titulo getTituloEquipado() {
        return tituloEquipado;
    }

    public void setTituloEquipado(Titulo tituloEquipado) {
        this.tituloEquipado = tituloEquipado;
    }
}
