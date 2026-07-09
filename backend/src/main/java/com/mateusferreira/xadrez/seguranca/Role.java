package com.mateusferreira.xadrez.seguranca;

/**
 * Papel do usuário na plataforma.
 *
 * <p>{@link #USER} é o padrão de toda conta. {@link #ADMIN} é uma conta de
 * demonstração/administração com acesso total — por exemplo, todas as skins
 * liberadas independentemente do rank. Contas viram ADMIN pela lista
 * {@code app.admin-usuarios} (ver {@code AdminSeeder}), não pelo cadastro.
 */
public enum Role {
    USER,
    ADMIN
}
