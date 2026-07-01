package com.mateusferreira.xadrez.seguranca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false, unique = true, length = 30)
    private String usuario;

    /** Hash BCrypt da senha (nunca a senha em texto puro). */
    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private int elo = 1200;

    protected Usuario() {
    }

    public Usuario(String usuario, String senhaHash) {
        this.usuario = usuario;
        this.senha = senhaHash;
        this.elo = 1200;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
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
}
