
package com.mateusferreira.xadrez.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * O tabuleiro 8x8. É a FONTE ÚNICA DA VERDADE sobre quem está em cada casa.
 *
 * Decisão de modelagem: representamos o tabuleiro como uma matriz Peca[8][8].
 *   - pecas[linha][coluna] guarda a peça naquela casa...
 *   - ...ou 'null' se a casa está vazia.
 *
 * Por que a matriz é a "fonte da verdade" e a peça NÃO guarda a própria posição?
 *   Se tanto a peça quanto o tabuleiro soubessem a posição, teríamos DUAS
 *   cópias da mesma informação — e o clássico bug de elas "desincronizarem"
 *   (mover a peça e esquecer de atualizar uma das cópias). Mantendo a posição
 *   só aqui, existe uma verdade só. Por isso movimentosPossiveis() recebe a
 *   'origem' de fora, em vez de a peça "se perguntar onde estou".
 */
public class Tabuleiro {

    private final Peca[][] pecas = new Peca[8][8];

    /**
     * Devolve a peça na casa indicada, ou null se a casa estiver vazia.
     * Guarda de segurança: se a posição estiver FORA do tabuleiro, devolve null
     * (tratamos "fora do tabuleiro" como "não há peça lá"). Isso deixa o código
     * de movimento mais simples: ele pode perguntar sem medo de estourar erro.
     */
    public Peca pecaEm(Posicao posicao) {
        if (!posicao.dentroDoTabuleiro()) {
            return null;
        }
        return pecas[posicao.linha()][posicao.coluna()];
    }

    /** A casa está vazia? */
    public boolean estaVazia(Posicao posicao) {
        return pecaEm(posicao) == null;
    }

    /**
     * Existe nessa casa uma peça da cor ADVERSÁRIA a 'minhaCor'?
     * É a pergunta que toda peça faz para saber se pode CAPTURAR ali.
     */
    public boolean temPecaAdversaria(Posicao posicao, Cor minhaCor) {
        Peca alvo = pecaEm(posicao);
        return alvo != null && alvo.getCor() == minhaCor.oposta();
    }

    /** Coloca (ou substitui) uma peça numa casa. Usado para montar o tabuleiro. */
    public void colocarPeca(Peca peca, Posicao posicao) {
        pecas[posicao.linha()][posicao.coluna()] = peca;
    }

    /** Esvazia uma casa (remove a peça que estiver nela). Usado na captura en passant. */
    public void removerPeca(Posicao posicao) {
        pecas[posicao.linha()][posicao.coluna()] = null;
    }

    /**
     * Encontra a casa onde está o Rei da cor informada.
     *
     * Varre as 64 casas procurando uma peça que SEJA um Rei (instanceof) E da
     * cor pedida. Lança erro se não houver rei — num jogo válido isso nunca
     * acontece, então um rei ausente indica um bug em quem montou o tabuleiro.
     */
    public Posicao posicaoDoRei(Cor cor) {
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = pecas[linha][coluna];
                if (peca instanceof Rei && peca.getCor() == cor) {
                    return new Posicao(linha, coluna);
                }
            }
        }
        throw new IllegalStateException("Não há rei " + cor + " no tabuleiro.");
    }

    /**
     * A casa 'alvo' está sendo atacada por ALGUMA peça da cor 'corAtacante'?
     *
     * Estratégia: para cada peça da cor atacante no tabuleiro, perguntamos se
     * os movimentos possíveis dela alcançam 'alvo'. Se qualquer uma alcança,
     * a casa está sob ataque.
     *
     * Por que isso funciona para todas as peças automaticamente:
     *   - Torre/Bispo/Rainha já param ao esbarrar -> não "atacam através" de peças.
     *   - Peão só inclui a diagonal quando há inimigo lá (e o alvo, o rei, É o
     *     inimigo) -> ataque diagonal correto; e NÃO ataca de frente.
     *   - Cavalo ignora o que está no caminho -> ataque por salto correto.
     */
    public boolean estaSobAtaque(Posicao alvo, Cor corAtacante) {
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = pecas[linha][coluna];
                if (peca != null && peca.getCor() == corAtacante) {
                    Posicao origem = new Posicao(linha, coluna);
                    if (peca.movimentosPossiveis(this, origem).contains(alvo)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * O rei da cor informada está em xeque?
     * Traduzindo: a casa do rei está sob ataque das peças adversárias.
     */
    public boolean estaEmXeque(Cor cor) {
        Posicao casaDoRei = posicaoDoRei(cor);
        return estaSobAtaque(casaDoRei, cor.oposta());
    }

    /**
     * Move uma peça de 'origem' para 'destino' (nível BAIXO, sem validar regras).
     * Se houver peça no destino, ela é sobrescrita — é assim que a captura
     * acontece: a peça capturada simplesmente some da grade.
     *
     * Quem valida se o movimento é legal é a camada de regras (Partida); aqui
     * só executamos mecanicamente.
     */
    public void moverPeca(Posicao origem, Posicao destino) {
        Peca peca = pecas[origem.linha()][origem.coluna()];
        pecas[origem.linha()][origem.coluna()] = null;       // esvazia a origem
        pecas[destino.linha()][destino.coluna()] = peca;     // ocupa o destino (captura se havia algo)
    }

    /**
     * Cria uma CÓPIA do tabuleiro, para simular jogadas sem alterar o original.
     *
     * Detalhe importante: copiamos a GRADE (a matriz), mas reutilizamos os
     * mesmos objetos Peca. Isso é SEGURO porque as peças não têm estado mutável
     * (só a cor, que é final) — elas não guardam posição. Foi por isso que, lá
     * no começo, decidimos manter a posição só no tabuleiro: agora copiar fica
     * barato e à prova de bug.
     */
    public Tabuleiro copia() {
        Tabuleiro nova = new Tabuleiro();
        for (int linha = 0; linha < 8; linha++) {
            System.arraycopy(this.pecas[linha], 0, nova.pecas[linha], 0, 8);
        }
        return nova;
    }

    /** Lista as casas ocupadas por peças da cor informada (para varrer em busca de mate). */
    public List<Posicao> posicoesDe(Cor cor) {
        List<Posicao> posicoes = new ArrayList<>();
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = pecas[linha][coluna];
                if (peca != null && peca.getCor() == cor) {
                    posicoes.add(new Posicao(linha, coluna));
                }
            }
        }
        return posicoes;
    }

    /**
     * STATIC FACTORY METHOD: cria um tabuleiro já montado na posição inicial
     * padrão do xadrez. Damos um NOME ("posição inicial") em vez de um
     * construtor anônimo — comunica melhor a intenção.
     */
    public static Tabuleiro posicaoInicial() {
        Tabuleiro t = new Tabuleiro();
        Cor[] cores = {Cor.BRANCO, Cor.PRETO};
        int[] linhaDasPecas = {0, 7}; // fileira das peças "grandes": branco linha 0, preto linha 7
        int[] linhaDosPeoes = {1, 6}; // fileira dos peões:           branco linha 1, preto linha 6

        for (int i = 0; i < 2; i++) {
            Cor cor = cores[i];
            int base = linhaDasPecas[i];

            // A ordem clássica da primeira fileira: T C B D R B C T
            t.colocarPeca(new Torre(cor),  new Posicao(base, 0));
            t.colocarPeca(new Cavalo(cor), new Posicao(base, 1));
            t.colocarPeca(new Bispo(cor),  new Posicao(base, 2));
            t.colocarPeca(new Rainha(cor), new Posicao(base, 3));
            t.colocarPeca(new Rei(cor),    new Posicao(base, 4));
            t.colocarPeca(new Bispo(cor),  new Posicao(base, 5));
            t.colocarPeca(new Cavalo(cor), new Posicao(base, 6));
            t.colocarPeca(new Torre(cor),  new Posicao(base, 7));

            // Os 8 peões da cor.
            for (int coluna = 0; coluna < 8; coluna++) {
                t.colocarPeca(new Peao(cor), new Posicao(linhaDosPeoes[i], coluna));
            }
        }
        return t;
    }

    /**
     * Devolve uma representação em TEXTO do tabuleiro (não imprime nada — só
     * monta a String; quem imprime é a camada de console).
     * Convenção: MAIÚSCULAS = brancas, minúsculas = pretas, '.' = casa vazia.
     */
    public String desenhar() {
        StringBuilder sb = new StringBuilder();
        // Desenhamos da fileira 8 (topo) para a 1 (base), como num tabuleiro real.
        for (int linha = 7; linha >= 0; linha--) {
            sb.append(linha + 1).append("  ");
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = pecas[linha][coluna];
                char c;
                if (peca == null) {
                    c = '.';
                } else {
                    c = peca.simbolo();
                    if (peca.getCor() == Cor.PRETO) {
                        c = Character.toLowerCase(c); // pretas em minúsculo
                    }
                }
                sb.append(c).append(' ');
            }
            sb.append('\n');
        }
        sb.append("\n   a b c d e f g h\n");
        return sb.toString();
    }

    /**
     * Serializa o tabuleiro numa String COMPACTA de 64 caracteres (uma por casa,
     * da linha 0 à 7, coluna 0 à 7). É o formato que vai pro banco.
     * Convenção: MAIÚSCULA = branca, minúscula = preta, '.' = vazia.
     * (Diferente de desenhar(), que tem espaços/rótulos e é só para exibição.)
     */
    public String serializar() {
        StringBuilder sb = new StringBuilder(64);
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = pecas[linha][coluna];
                if (peca == null) {
                    sb.append('.');
                } else {
                    char c = peca.simbolo();
                    sb.append(peca.getCor() == Cor.BRANCO ? c : Character.toLowerCase(c));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Reconstrói um Tabuleiro a partir da String de 64 caracteres gerada por
     * serializar(). É o caminho de VOLTA (banco -> objeto).
     */
    public static Tabuleiro deTexto(String texto) {
        Tabuleiro t = new Tabuleiro();
        for (int i = 0; i < 64; i++) {
            char c = texto.charAt(i);
            if (c == '.') {
                continue; // casa vazia
            }
            int linha = i / 8;   // a cada 8 caracteres, sobe uma linha
            int coluna = i % 8;  // posição dentro da linha
            Cor cor = Character.isUpperCase(c) ? Cor.BRANCO : Cor.PRETO;
            Peca peca = criarPeca(Character.toUpperCase(c), cor);
            t.colocarPeca(peca, new Posicao(linha, coluna));
        }
        return t;
    }

    /**
     * Mapeia um símbolo de peça (em maiúsculo) + cor para o objeto Peca correto.
     * Usa um 'switch expression' (Java moderno): cada caso já "devolve" o valor.
     */
    private static Peca criarPeca(char simbolo, Cor cor) {
        return switch (simbolo) {
            case 'T' -> new Torre(cor);
            case 'C' -> new Cavalo(cor);
            case 'B' -> new Bispo(cor);
            case 'D' -> new Rainha(cor);
            case 'R' -> new Rei(cor);
            case 'P' -> new Peao(cor);
            default -> throw new IllegalArgumentException("Símbolo de peça desconhecido: " + simbolo);
        };
    }
}
