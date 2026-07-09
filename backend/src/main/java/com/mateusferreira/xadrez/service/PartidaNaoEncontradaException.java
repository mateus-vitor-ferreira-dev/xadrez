package com.mateusferreira.xadrez.service;

/**
 * Lançada quando se pede uma partida (por id) que não existe no banco.
 * Será traduzida para HTTP 404 (Not Found) no tratador de erros.
 */
public class PartidaNaoEncontradaException extends RuntimeException {

    public PartidaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
