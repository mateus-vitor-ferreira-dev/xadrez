package com.mateusferreira.xadrez.controller.dto;

/**
 * Corpo do {@code PUT /usuario/perfil}. Todos os campos são substituídos pelo
 * valor enviado: {@code email} é obrigatório e validado (formato + unicidade);
 * {@code telefone} é opcional — vazio/branco vira NULL (limpa o campo). O apelido
 * e a senha NÃO são editados por aqui.
 */
public record AtualizarPerfilRequest(String email, String telefone) {
}
