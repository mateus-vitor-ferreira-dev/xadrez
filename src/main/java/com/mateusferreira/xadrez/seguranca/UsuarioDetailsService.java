package com.mateusferreira.xadrez.seguranca;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ponte entre o nosso Usuario e o Spring Security: carrega o usuário pelo nome
 * e o adapta para o UserDetails que o Security entende (com o hash da senha).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) {
        Usuario u = repository.findByUsuario(usuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + usuario));
        return User.withUsername(u.getUsuario())
                .password(u.getSenha())
                .authorities("ROLE_USER")
                .build();
    }
}
