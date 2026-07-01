package com.mateusferreira.xadrez.controller.dto;

/**
 * Resposta do /auth/google. Dois casos:
 * <ul>
 *   <li><b>novo=false</b>: já existe conta com esse e-mail → {@code sessao} traz
 *       o token (login concluído).</li>
 *   <li><b>novo=true</b>: primeiro acesso → {@code sessao} é null; o front usa
 *       {@code email}/{@code sugestaoApelido} para pedir o apelido e finalizar.</li>
 * </ul>
 */
public record GoogleAuthResponse(boolean novo, TokenResponse sessao, String email, String sugestaoApelido) {
}
