package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.LoginRequest;
import com.mateusferreira.xadrez.controller.dto.RegistroRequest;
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

import java.util.regex.Pattern;

/**
 * Autenticação: cadastro e login. Ambos devolvem um token JWT que o front
 * guarda e envia (Authorization: Bearer ...) nas próximas requisições.
 *
 * <p>Cadastro pede apelido (único), e-mail (único) e senha. O login aceita
 * <b>e-mail OU apelido</b> como identificador (ver {@code UsuarioDetailsService}).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    // Validação simples de e-mail: algo@algo.dominio (sem espaços). Não é a RFC
    // completa, mas cobre os casos reais e evita entradas claramente inválidas.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
    public TokenResponse register(@RequestBody RegistroRequest req) {
        String usuario = req.usuario() == null ? "" : req.usuario().trim();
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        String senha = req.senha() == null ? "" : req.senha();
        if (usuario.length() < 3 || usuario.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O usuário deve ter de 3 a 30 caracteres.");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail válido.");
        }
        if (senha.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter ao menos 4 caracteres.");
        }
        if (repository.existsByUsuario(usuario)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse apelido já está em uso.");
        }
        if (repository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse e-mail já está cadastrado.");
        }
        Usuario novo = repository.save(new Usuario(usuario, email, encoder.encode(senha)));
        return tokenPara(novo);
    }

    /** POST /auth/login — valida identificador (e-mail ou apelido) + senha e devolve o token. */
    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest req) {
        String identificador = req.identificador() == null ? "" : req.identificador().trim();
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(identificador, req.senha()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }
        // Login pode vir por e-mail; buscamos por apelido e, se não achar, por e-mail.
        Usuario u = repository.findByUsuario(identificador)
                .or(() -> repository.findByEmail(identificador.toLowerCase()))
                .orElseThrow();
        return tokenPara(u);
    }

    /** Monta o token + dados públicos do usuário. */
    private TokenResponse tokenPara(Usuario u) {
        return new TokenResponse(jwtService.gerar(u.getUsuario()), u.getUsuario(), u.getEmail(), u.getElo());
    }
}
