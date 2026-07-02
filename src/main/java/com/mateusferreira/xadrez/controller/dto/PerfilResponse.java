package com.mateusferreira.xadrez.controller.dto;

/**
 * Dados do perfil do usuário logado, usados para PREENCHER a tela de edição e
 * devolvidos após salvar. Diferente do {@link TokenResponse} (a "sessão"), aqui
 * moram campos privados/editáveis como {@code telefone} — que não pertencem ao
 * token público. O {@code usuario} (apelido) vem junto só para exibir; a edição
 * dele fica fora deste escopo. {@code telefone} e {@code fotoUrl} são nullable.
 */
public record PerfilResponse(String usuario, String email, String telefone, String fotoUrl) {
}
