package com.mateusferreira.xadrez.dominio;

/**
 * A cor de uma peça (e do jogador da vez).
 *
 * Por que um ENUM e não, por exemplo, uma String "branco"/"preto" ou um boolean?
 *
 *  - Só existem DOIS valores possíveis e eles são FIXOS. Enum garante isso em
 *    tempo de compilação: é impossível criar um Cor.AZUL por engano. Com String,
 *    "Branco", "branco", "BRANCO" e "banco" (typo!) seriam todos "válidos" e
 *    causariam bugs silenciosos.
 *  - Um boolean (isBranco) funcionaria, mas 'true'/'false' não se leem como
 *    "branco"/"preto" no código. Enum deixa a intenção explícita.
 *
 * Lição: sempre que um atributo só pode ser um conjunto pequeno e fixo de
 * valores, pense em enum.
 */
public enum Cor {

    BRANCO,
    PRETO;

    /**
     * Devolve a cor adversária. Útil em várias regras ("é a vez do oponente?",
     * "essa casa está atacada por uma peça da cor oposta?").
     *
     * Colocar este método AQUI dentro (em vez de espalhar 'if cor == BRANCO ...'
     * pelo código) é um exemplo de manter o comportamento junto do dado a que
     * ele pertence — coração da Orientação a Objetos.
     */
    public Cor oposta() {
        return this == BRANCO ? PRETO : BRANCO;
    }
}
