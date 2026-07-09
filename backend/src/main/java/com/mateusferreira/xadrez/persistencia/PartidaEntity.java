package com.mateusferreira.xadrez.persistencia;

import com.mateusferreira.xadrez.dominio.Cor;
import com.mateusferreira.xadrez.dominio.Resultado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * MODELO DE PERSISTÊNCIA: representa uma partida COMO ELA É GUARDADA NO BANCO.
 * É propositalmente separada do domínio (Partida/Tabuleiro), que continua puro.
 *
 * Note que esta classe é "burra": só carrega dados para ir/voltar do banco. As
 * regras do xadrez NÃO moram aqui. Na próxima etapa faremos a tradução
 * entity <-> domínio.
 *
 * Anotações JPA:
 *  @Entity        -> "esta classe vira uma tabela no banco".
 *  @Table(name)   -> nome da tabela (sem isso, usaria o nome da classe).
 *  @Id            -> a chave primária.
 *  @GeneratedValue-> o banco gera o id automaticamente (auto-incremento).
 *  @Column        -> ajustes da coluna (tamanho, obrigatoriedade...).
 *  @Enumerated(STRING) -> guarda o enum como TEXTO ("BRANCO"), não como número
 *                         (número quebraria se mudássemos a ordem do enum).
 */
@Entity
@Table(name = "partidas")
public class PartidaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** O tabuleiro serializado (faremos a serialização compacta no próximo bloco). */
    @Column(nullable = false, length = 64)
    private String tabuleiro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Cor jogadorAtual;

    /** Direitos de roque no formato "KQkq" (ou "-"). Estado especial persistido. */
    @Column(nullable = false, length = 4)
    private String roque;

    /** Casa-alvo de en passant em notação ("e3"), ou null se não houver. */
    @Column(length = 2)
    private String enPassant;

    // ----- Campos de PARTIDA ONLINE / ELO (Fase 4) -----

    /** true se é uma partida online (única que pontua Elo). */
    @Column(nullable = false)
    private boolean online = false;

    /** Username de quem joga de brancas (null se anônimo/local). */
    @Column(length = 30)
    private String brancoUsuario;

    /** Username de quem joga de pretas (null enquanto ninguém entrou). */
    @Column(length = 30)
    private String pretoUsuario;

    /** Desfecho da partida. Começa EM_ANDAMENTO. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Resultado resultado = Resultado.EM_ANDAMENTO;

    /** Trava para aplicar o Elo UMA única vez, mesmo se a partida recarregar. */
    @Column(nullable = false)
    private boolean eloAplicado = false;

    /** Variação de Elo das brancas nesta partida (null enquanto não aplicado). */
    private Integer deltaBranco;

    /** Variação de Elo das pretas nesta partida (null enquanto não aplicado). */
    private Integer deltaPreto;

    /**
     * Construtor SEM argumentos exigido pela JPA: o Hibernate cria o objeto
     * "vazio" via reflexão e depois preenche os campos. Fica 'protected' para
     * desencorajar uso direto no nosso código.
     */
    protected PartidaEntity() {
    }

    public PartidaEntity(String tabuleiro, Cor jogadorAtual, String roque, String enPassant) {
        this.tabuleiro = tabuleiro;
        this.jogadorAtual = jogadorAtual;
        this.roque = roque;
        this.enPassant = enPassant;
    }

    public Long getId() {
        return id;
    }

    public String getTabuleiro() {
        return tabuleiro;
    }

    public Cor getJogadorAtual() {
        return jogadorAtual;
    }

    public String getRoque() {
        return roque;
    }

    public String getEnPassant() {
        return enPassant;
    }

    public void setTabuleiro(String tabuleiro) {
        this.tabuleiro = tabuleiro;
    }

    public void setJogadorAtual(Cor jogadorAtual) {
        this.jogadorAtual = jogadorAtual;
    }

    public void setRoque(String roque) {
        this.roque = roque;
    }

    public void setEnPassant(String enPassant) {
        this.enPassant = enPassant;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getBrancoUsuario() {
        return brancoUsuario;
    }

    public void setBrancoUsuario(String brancoUsuario) {
        this.brancoUsuario = brancoUsuario;
    }

    public String getPretoUsuario() {
        return pretoUsuario;
    }

    public void setPretoUsuario(String pretoUsuario) {
        this.pretoUsuario = pretoUsuario;
    }

    public Resultado getResultado() {
        return resultado;
    }

    public void setResultado(Resultado resultado) {
        this.resultado = resultado;
    }

    public boolean isEloAplicado() {
        return eloAplicado;
    }

    public void setEloAplicado(boolean eloAplicado) {
        this.eloAplicado = eloAplicado;
    }

    public Integer getDeltaBranco() {
        return deltaBranco;
    }

    public void setDeltaBranco(Integer deltaBranco) {
        this.deltaBranco = deltaBranco;
    }

    public Integer getDeltaPreto() {
        return deltaPreto;
    }

    public void setDeltaPreto(Integer deltaPreto) {
        this.deltaPreto = deltaPreto;
    }
}
