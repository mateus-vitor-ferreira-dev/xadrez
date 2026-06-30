package com.mateusferreira.xadrez.dominio;

import java.util.List;

/**
 * Classe ABSTRATA que representa qualquer peça do xadrez.
 *
 * É o "topo" da hierarquia de herança que você escolheu: Peao, Torre, Bispo,
 * Cavalo, Rainha e Rei vão ESTENDER esta classe.
 *
 * Por que abstrata?
 *   Porque não existe uma peça "genérica" jogável. Toda peça TEM uma cor
 *   (parte comum, fica aqui), mas CADA tipo se move de um jeito (parte
 *   específica, fica em cada subclasse). Como não dá para escrever o corpo de
 *   movimentosPossiveis() aqui, ele vira um método ABSTRATO — e isso obriga a
 *   classe inteira a ser abstrata. Resultado: 'new Peca(...)' é proibido pelo
 *   compilador. Só dá para criar 'new Torre(...)', 'new Bispo(...)' etc.
 */
public abstract class Peca {

    /**
     * A cor é 'final': definida no construtor e nunca mais muda (uma peça
     * branca não vira preta). É a tal COMPOSIÇÃO que conversamos — a cor é um
     * ATRIBUTO da peça, não um subtipo. 'private' = só a própria Peca acessa
     * diretamente; o resto do mundo usa getCor().
     */
    private final Cor cor;

    /**
     * Construtor 'protected' (não 'public'): ninguém de fora consegue chamar,
     * mas as subclasses conseguem via 'super(cor)'. É a forma de dizer
     * "essa classe só serve para ser herdada".
     */
    protected Peca(Cor cor) {
        this.cor = cor;
    }

    public Cor getCor() {
        return cor;
    }

    /**
     * O CORAÇÃO do polimorfismo. Cada subclasse é OBRIGADA a implementar este
     * método, dizendo para quais casas ELA pode ir, a partir de uma 'origem',
     * consultando o 'tabuleiro' para saber o que está pelo caminho.
     *
     * Quem usar as peças (a futura camada de regras) vai simplesmente chamar
     * 'peca.movimentosPossiveis(tabuleiro, origem)' SEM perguntar que peça é —
     * cada uma responde do seu jeito. Isso é polimorfismo na prática.
     *
     * Retorna a lista de casas de destino LEGAIS para o movimento desta peça.
     */
    public abstract List<Posicao> movimentosPossiveis(Tabuleiro tabuleiro, Posicao origem);

    /**
     * Um caractere para exibir a peça (ex.: 'T' de torre). Também abstrato:
     * cada subclasse devolve o seu. Vai nos ajudar a "desenhar" o tabuleiro no
     * console para testar.
     */
    public abstract char simbolo();
}
