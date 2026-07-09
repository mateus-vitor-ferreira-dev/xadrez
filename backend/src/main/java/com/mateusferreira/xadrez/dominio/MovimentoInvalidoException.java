package com.mateusferreira.xadrez.dominio;

/**
 * Lançada quando se tenta executar uma jogada que viola as regras
 * (não é a vez do jogador, não há peça na origem, ou o movimento é ilegal).
 *
 * Estende RuntimeException (exceção "não checada"): não obrigamos quem chama a
 * envolver tudo em try/catch. Mais adiante, na API REST, vamos capturá-la num
 * ponto central e transformá-la numa resposta HTTP 400 (Bad Request).
 */
public class MovimentoInvalidoException extends RuntimeException {

    public MovimentoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
