package com.mateusferreira.xadrez.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Camada de regras do jogo: tabuleiro + de quem é a vez + regras que conectam
 * tudo (movimentos legais, aplicar jogada, xeque, mate, e as regras especiais).
 *
 * A partir do roque, a Partida passa a ter ESTADO além do tabuleiro: os
 * "direitos de roque" (quem ainda pode rocar e de que lado). Esse estado nasce
 * do histórico do jogo, então é guardado aqui (e persistido no banco).
 */
public class Partida {

    private final Tabuleiro tabuleiro;
    private Cor jogadorAtual;

    // Direitos de roque: true = aquele lado ainda é permitido.
    private boolean roqueReiBranco;   // roque pequeno das brancas (torre h1)
    private boolean roqueDamaBranco;  // roque grande das brancas  (torre a1)
    private boolean roqueReiPreto;    // roque pequeno das pretas  (torre h8)
    private boolean roqueDamaPreto;   // roque grande das pretas   (torre a8)

    // Casa onde uma captura en passant é possível NESTE lance (null se não há).
    // É a casa "pulada" por um peão que acabou de avançar duas casas.
    private Posicao alvoEnPassant;

    /**
     * Construtor completo — usado ao RECONSTRUIR a partida (ex.: vinda do banco),
     * com todo o estado especial: direitos de roque e alvo de en passant.
     */
    public Partida(Tabuleiro tabuleiro, Cor jogadorInicial,
                   boolean roqueReiBranco, boolean roqueDamaBranco,
                   boolean roqueReiPreto, boolean roqueDamaPreto,
                   Posicao alvoEnPassant) {
        this.tabuleiro = tabuleiro;
        this.jogadorAtual = jogadorInicial;
        this.roqueReiBranco = roqueReiBranco;
        this.roqueDamaBranco = roqueDamaBranco;
        this.roqueReiPreto = roqueReiPreto;
        this.roqueDamaPreto = roqueDamaPreto;
        this.alvoEnPassant = alvoEnPassant;
    }

    /** Construtor sem alvo de en passant (direitos de roque informados). */
    public Partida(Tabuleiro tabuleiro, Cor jogadorInicial,
                   boolean roqueReiBranco, boolean roqueDamaBranco,
                   boolean roqueReiPreto, boolean roqueDamaPreto) {
        this(tabuleiro, jogadorInicial, roqueReiBranco, roqueDamaBranco, roqueReiPreto, roqueDamaPreto, null);
    }

    /** Construtor simples: posição "limpa", todos os direitos de roque ativos, sem en passant. */
    public Partida(Tabuleiro tabuleiro, Cor jogadorInicial) {
        this(tabuleiro, jogadorInicial, true, true, true, true, null);
    }

    /** Static factory: começa uma partida nova, na posição inicial, brancas jogam. */
    public static Partida nova() {
        return new Partida(Tabuleiro.posicaoInicial(), Cor.BRANCO);
    }

    public Cor getJogadorAtual() {
        return jogadorAtual;
    }

    public Tabuleiro getTabuleiro() {
        return tabuleiro;
    }

    /** Direitos de roque no formato "KQkq" (K/Q = brancas rei/dama; k/q = pretas), ou "-". */
    public String direitosDeRoque() {
        StringBuilder sb = new StringBuilder();
        if (roqueReiBranco) sb.append('K');
        if (roqueDamaBranco) sb.append('Q');
        if (roqueReiPreto) sb.append('k');
        if (roqueDamaPreto) sb.append('q');
        return sb.isEmpty() ? "-" : sb.toString();
    }

    /** Casa-alvo de en passant disponível neste lance, ou null. */
    public Posicao getAlvoEnPassant() {
        return alvoEnPassant;
    }

    public boolean estaEmXeque(Cor cor) {
        return tabuleiro.estaEmXeque(cor);
    }

    /**
     * Movimentos LEGAIS de uma peça: os pseudo-legais que não deixam o próprio
     * rei em xeque, MAIS os roques (quando a peça é o rei e as condições valem).
     */
    public List<Posicao> movimentosLegais(Posicao origem) {
        Peca peca = tabuleiro.pecaEm(origem);
        if (peca == null) {
            return List.of();
        }

        List<Posicao> legais = new ArrayList<>();
        for (Posicao destino : peca.movimentosPossiveis(tabuleiro, origem)) {
            Tabuleiro simulacao = tabuleiro.copia();
            simulacao.moverPeca(origem, destino);
            if (!simulacao.estaEmXeque(peca.getCor())) {
                legais.add(destino);
            }
        }

        if (peca instanceof Rei) {
            adicionarRoques(peca, origem, legais);
        }
        if (peca instanceof Peao) {
            adicionarEnPassant(peca, origem, legais);
        }
        return legais;
    }

    /** Move com promoção padrão (Rainha), quando aplicável. */
    public void mover(Posicao origem, Posicao destino) {
        mover(origem, destino, TipoPromocao.RAINHA);
    }

    /** Move, promovendo um peão que chegue à última fileira para a peça escolhida. */
    public void mover(Posicao origem, Posicao destino, TipoPromocao promocao) {
        Peca peca = tabuleiro.pecaEm(origem);

        if (peca == null) {
            throw new MovimentoInvalidoException("Não há peça em " + origem + ".");
        }
        if (peca.getCor() != jogadorAtual) {
            throw new MovimentoInvalidoException("Não é a vez das peças " + peca.getCor() + ".");
        }
        if (!movimentosLegais(origem).contains(destino)) {
            throw new MovimentoInvalidoException("Movimento ilegal: " + origem + " -> " + destino + ".");
        }

        // Detecta os lances especiais ANTES de mover (usando a origem e o alvo atual).
        boolean ehRoque = (peca instanceof Rei) && Math.abs(destino.coluna() - origem.coluna()) == 2;
        boolean ehEnPassant = (peca instanceof Peao) && destino.equals(alvoEnPassant);

        tabuleiro.moverPeca(origem, destino);
        if (ehRoque) {
            moverTorreDoRoque(origem, destino); // a torre acompanha o rei
        }
        if (ehEnPassant) {
            // O peão capturado está AO LADO da origem: mesma linha da origem, coluna do destino.
            tabuleiro.removerPeca(new Posicao(origem.linha(), destino.coluna()));
        }
        promoverSeNecessario(destino, promocao);
        atualizarDireitosDeRoque(peca, origem);
        atualizarAlvoEnPassant(peca, origem, destino);

        jogadorAtual = jogadorAtual.oposta();
    }

    public boolean estaEmXequeMate(Cor cor) {
        if (!tabuleiro.estaEmXeque(cor)) {
            return false;
        }
        return !temAlgumMovimentoLegal(cor);
    }

    public boolean estaEmAfogamento(Cor cor) {
        if (tabuleiro.estaEmXeque(cor)) {
            return false;
        }
        return !temAlgumMovimentoLegal(cor);
    }

    private boolean temAlgumMovimentoLegal(Cor cor) {
        for (Posicao origem : tabuleiro.posicoesDe(cor)) {
            if (!movimentosLegais(origem).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ----- Regras especiais -----

    /** Promoção: peão que chega à última fileira vira a peça escolhida (padrão Rainha). */
    private void promoverSeNecessario(Posicao destino, TipoPromocao promocao) {
        Peca peca = tabuleiro.pecaEm(destino);
        if (peca instanceof Peao) {
            int ultimaFileira = (peca.getCor() == Cor.BRANCO) ? 7 : 0;
            if (destino.linha() == ultimaFileira) {
                tabuleiro.colocarPeca(promocao.cria(peca.getCor()), destino);
            }
        }
    }

    /**
     * Acrescenta os roques aos movimentos legais do rei, se permitidos.
     * Condições: direito ativo, torre no canto, casas entre eles vazias, rei
     * não está em xeque e não passa por (nem chega a) casa atacada.
     */
    private void adicionarRoques(Peca rei, Posicao origem, List<Posicao> legais) {
        Cor cor = rei.getCor();
        Cor adversaria = cor.oposta();
        int linha = (cor == Cor.BRANCO) ? 0 : 7;
        Posicao casaRei = new Posicao(linha, 4);

        // O rei precisa estar na casa inicial e NÃO pode estar em xeque para rocar.
        if (!origem.equals(casaRei) || tabuleiro.estaSobAtaque(casaRei, adversaria)) {
            return;
        }

        // Roque pequeno (lado do rei): torre em (linha,7); f e g vazias; rei passa por f e g.
        boolean direitoRei = (cor == Cor.BRANCO) ? roqueReiBranco : roqueReiPreto;
        if (direitoRei
                && ehTorreDaCor(new Posicao(linha, 7), cor)
                && tabuleiro.estaVazia(new Posicao(linha, 5))
                && tabuleiro.estaVazia(new Posicao(linha, 6))
                && !tabuleiro.estaSobAtaque(new Posicao(linha, 5), adversaria)
                && !tabuleiro.estaSobAtaque(new Posicao(linha, 6), adversaria)) {
            legais.add(new Posicao(linha, 6));
        }

        // Roque grande (lado da dama): torre em (linha,0); b, c e d vazias; rei passa por d e c.
        boolean direitoDama = (cor == Cor.BRANCO) ? roqueDamaBranco : roqueDamaPreto;
        if (direitoDama
                && ehTorreDaCor(new Posicao(linha, 0), cor)
                && tabuleiro.estaVazia(new Posicao(linha, 1))
                && tabuleiro.estaVazia(new Posicao(linha, 2))
                && tabuleiro.estaVazia(new Posicao(linha, 3))
                && !tabuleiro.estaSobAtaque(new Posicao(linha, 3), adversaria)
                && !tabuleiro.estaSobAtaque(new Posicao(linha, 2), adversaria)) {
            legais.add(new Posicao(linha, 2));
        }
    }

    private boolean ehTorreDaCor(Posicao casa, Cor cor) {
        Peca peca = tabuleiro.pecaEm(casa);
        return peca instanceof Torre && peca.getCor() == cor;
    }

    /** Move a torre para o outro lado do rei, completando o roque. */
    private void moverTorreDoRoque(Posicao origemRei, Posicao destinoRei) {
        int linha = origemRei.linha();
        if (destinoRei.coluna() == 6) {        // roque pequeno: torre h -> f
            tabuleiro.moverPeca(new Posicao(linha, 7), new Posicao(linha, 5));
        } else if (destinoRei.coluna() == 2) { // roque grande: torre a -> d
            tabuleiro.moverPeca(new Posicao(linha, 0), new Posicao(linha, 3));
        }
    }

    /** Atualiza os direitos de roque após uma jogada (rei ou torre que se moveu). */
    private void atualizarDireitosDeRoque(Peca peca, Posicao origem) {
        if (peca instanceof Rei) {
            // Rei se moveu -> perde os dois roques da sua cor.
            if (peca.getCor() == Cor.BRANCO) {
                roqueReiBranco = false;
                roqueDamaBranco = false;
            } else {
                roqueReiPreto = false;
                roqueDamaPreto = false;
            }
        } else if (peca instanceof Torre) {
            // Torre saiu de um canto -> perde o direito daquele lado.
            if (origem.equals(new Posicao(0, 7))) roqueReiBranco = false;
            if (origem.equals(new Posicao(0, 0))) roqueDamaBranco = false;
            if (origem.equals(new Posicao(7, 7))) roqueReiPreto = false;
            if (origem.equals(new Posicao(7, 0))) roqueDamaPreto = false;
        }
    }

    /**
     * Acrescenta a captura en passant aos movimentos legais do peão, se o
     * alvo atual estiver numa de suas diagonais da frente. Como o peão
     * capturado fica AO LADO (não no destino), a simulação remove esse peão
     * antes de checar se o próprio rei ficaria em xeque.
     */
    private void adicionarEnPassant(Peca peao, Posicao origem, List<Posicao> legais) {
        if (alvoEnPassant == null) {
            return;
        }
        int direcao = (peao.getCor() == Cor.BRANCO) ? 1 : -1;
        Posicao diagonalEsquerda = origem.deslocar(direcao, -1);
        Posicao diagonalDireita = origem.deslocar(direcao, 1);
        if (!alvoEnPassant.equals(diagonalEsquerda) && !alvoEnPassant.equals(diagonalDireita)) {
            return; // o alvo não está ao alcance deste peão
        }

        Tabuleiro simulacao = tabuleiro.copia();
        simulacao.moverPeca(origem, alvoEnPassant);
        simulacao.removerPeca(new Posicao(origem.linha(), alvoEnPassant.coluna())); // remove o peão capturado
        if (!simulacao.estaEmXeque(peao.getCor())) {
            legais.add(alvoEnPassant);
        }
    }

    /**
     * Define o alvo de en passant para o PRÓXIMO lance: se um peão acabou de
     * avançar duas casas, o alvo é a casa intermediária (a que ele "pulou").
     * Qualquer outra jogada limpa o alvo (en passant só vale por um lance).
     */
    private void atualizarAlvoEnPassant(Peca peca, Posicao origem, Posicao destino) {
        boolean peaoAvancouDuas = (peca instanceof Peao)
                && Math.abs(destino.linha() - origem.linha()) == 2;
        if (peaoAvancouDuas) {
            int linhaIntermediaria = (origem.linha() + destino.linha()) / 2;
            alvoEnPassant = new Posicao(linhaIntermediaria, origem.coluna());
        } else {
            alvoEnPassant = null;
        }
    }
}
