package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.CredenciaisRequest;
import com.mateusferreira.xadrez.controller.dto.TokenResponse;
import com.mateusferreira.xadrez.seguranca.JwtService;
import com.mateusferreira.xadrez.seguranca.Usuario;
import com.mateusferreira.xadrez.seguranca.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Autenticação: cadastro e login. Ambos devolvem um token JWT que o front
 * guarda e envia (Authorization: Bearer ...) nas próximas requisições.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository repository, PasswordEncoder encoder,
                          AuthenticationManager authManager, JwtService jwtService) {
        this.repository = repository;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    /** POST /auth/register — cria a conta (senha guardada como hash) e já devolve o token. */
    @PostMapping("/register")
    public TokenResponse register(@RequestBody CredenciaisRequest req) {
        String usuario = req.usuario() == null ? "" : req.usuario().trim();
        String senha = req.senha() == null ? "" : req.senha();
        if (usuario.length() < 3 || usuario.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário deve ter de 3 a 30 caracteres.");
        }
        if (senha.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter ao menos 4 caracteres.");
        }
        if (repository.existsByUsuario(usuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário já existe.");
        }
        Usuario novo = repository.save(new Usuario(usuario, encoder.encode(senha)));
        return new TokenResponse(jwtService.gerar(novo.getUsuario()), novo.getUsuario(), novo.getElo());
    }

    /** POST /auth/login — valida usuário/senha e devolve o token. */
    @PostMapping("/login")
    public TokenResponse login(@RequestBody CredenciaisRequest req) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.usuario(), req.senha()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }
        Usuario u = repository.findByUsuario(req.usuario()).orElseThrow();
        return new TokenResponse(jwtService.gerar(u.getUsuario()), u.getUsuario(), u.getElo());
    }
}
