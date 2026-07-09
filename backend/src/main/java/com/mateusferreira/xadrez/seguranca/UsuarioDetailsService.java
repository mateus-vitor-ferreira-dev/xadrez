package com.mateusferreira.xadrez.seguranca;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ponte entre o nosso Usuario e o Spring Security: carrega o usuário pelo
 * identificador de login — que pode ser o APELIDO ou o E-MAIL — e o adapta para
 * o UserDetails que o Security entende (com o hash da senha).
 *
 * <p>O username do UserDetails é sempre o APELIDO (estável): assim o token JWT
 * carrega o apelido independentemente de o login ter sido por e-mail ou apelido.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String identificador) {
        Usuario u = repository.findByUsuario(identificador)
                .or(() -> repository.findByEmail(identificador))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + identificador));
        return User.withUsername(u.getUsuario())
                .password(u.getSenha())
                .authorities("ROLE_USER")
                .build();
    }
}
