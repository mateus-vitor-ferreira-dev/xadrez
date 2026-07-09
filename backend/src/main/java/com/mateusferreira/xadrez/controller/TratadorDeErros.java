package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.ErroResponse;
import com.mateusferreira.xadrez.dominio.MovimentoInvalidoException;
import com.mateusferreira.xadrez.service.PartidaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratador GLOBAL de erros da API. O @RestControllerAdvice intercepta exceções
 * lançadas por qualquer @RestController e as converte numa resposta HTTP limpa.
 *
 * Cada método @ExceptionHandler cuida de um tipo de exceção, e o @ResponseStatus
 * define o código HTTP correto — assim a API "diz a verdade" sobre o que houve.
 */
@RestControllerAdvice
public class TratadorDeErros {

    /**
     * Jogada ilegal = erro do CLIENTE (ele mandou uma jogada que não vale).
     * Logo, HTTP 400 (Bad Request) — e não mais o 500 ("erro meu, servidor").
     */
    @ExceptionHandler(MovimentoInvalidoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse movimentoInvalido(MovimentoInvalidoException e) {
        return new ErroResponse(e.getMessage());
    }

    /**
     * Pedir uma partida (por id) que não existe no banco -> HTTP 404 (Not Found).
     */
    @ExceptionHandler(PartidaNaoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResponse partidaInexistente(PartidaNaoEncontradaException e) {
        return new ErroResponse(e.getMessage());
    }
}
