package com.mateusferreira.xadrez.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que roda a cada requisição: se houver um "Authorization: Bearer <token>"
 * válido, autentica o usuário no contexto do Security. Não bloqueia nada por si
 * só — apenas identifica quem está logado (as regras de acesso ficam no config).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthFilter(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String usuario = jwtService.usuarioDoToken(header.substring(7));
            if (usuario != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails ud = usuarioDetailsService.loadUserByUsername(usuario);
                    var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (RuntimeException ignore) {
                    // usuário do token não existe mais -> segue sem autenticar
                }
            }
        }
        chain.doFilter(req, res);
    }
}
