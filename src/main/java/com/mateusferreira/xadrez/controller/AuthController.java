package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.GoogleAuthResponse;
import com.mateusferreira.xadrez.controller.dto.GoogleFinalizarRequest;
import com.mateusferreira.xadrez.controller.dto.GoogleLoginRequest;
import com.mateusferreira.xadrez.controller.dto.LoginRequest;
import com.mateusferreira.xadrez.controller.dto.RegistroRequest;
import com.mateusferreira.xadrez.controller.dto.TokenResponse;
import com.mateusferreira.xadrez.seguranca.GoogleTokenVerifier;
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

import java.util.UUID;
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
    private final GoogleTokenVerifier googleVerifier;

    public AuthController(UsuarioRepository repository, PasswordEncoder encoder,
                          AuthenticationManager authManager, JwtService jwtService,
                          GoogleTokenVerifier googleVerifier) {
        this.repository = repository;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.googleVerifier = googleVerifier;
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

    /**
     * POST /auth/google — valida o ID token do Google. Se já existe conta com o
     * e-mail, faz login; se é o 1º acesso, devolve {@code novo=true} + uma
     * sugestão de apelido, e o front chama /auth/google/finalizar.
     */
    @PostMapping("/google")
    public GoogleAuthResponse google(@RequestBody GoogleLoginRequest req) {
        GoogleTokenVerifier.GoogleUser gu = googleVerifier.verificar(req.credential());
        String email = gu.email().toLowerCase();
        return repository.findByEmail(email)
                .map(u -> new GoogleAuthResponse(false, tokenPara(u), null, null))
                .orElseGet(() -> new GoogleAuthResponse(true, null, email, sugerirApelido(gu)));
    }

    /**
     * POST /auth/google/finalizar — cria a conta do 1º acesso via Google, com o
     * apelido escolhido. Revalida o credential (não confia só no front).
     */
    @PostMapping("/google/finalizar")
    public TokenResponse googleFinalizar(@RequestBody GoogleFinalizarRequest req) {
        GoogleTokenVerifier.GoogleUser gu = googleVerifier.verificar(req.credential());
        String email = gu.email().toLowerCase();
        // Se a conta já existe (ex.: finalizou noutra aba), apenas devolve o token.
        var existente = repository.findByEmail(email);
        if (existente.isPresent()) {
            return tokenPara(existente.get());
        }
        String apelido = req.apelido() == null ? "" : req.apelido().trim();
        if (apelido.length() < 3 || apelido.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O apelido deve ter de 3 a 30 caracteres.");
        }
        if (repository.existsByUsuario(apelido)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse apelido já está em uso.");
        }
        // Conta via Google não tem senha utilizável: guardamos um hash aleatório.
        Usuario novo = repository.save(new Usuario(apelido, email, encoder.encode(UUID.randomUUID().toString())));
        return tokenPara(novo);
    }

    /** Sugestão de apelido a partir do e-mail (só sugestão; o usuário confirma). */
    private String sugerirApelido(GoogleTokenVerifier.GoogleUser gu) {
        String base = gu.email().split("@")[0].toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (base.length() < 3) {
            base = "jogador";
        }
        return base.length() > 30 ? base.substring(0, 30) : base;
    }

    /** Monta o token + dados públicos do usuário. */
    private TokenResponse tokenPara(Usuario u) {
        String titulo = u.getTituloEquipado() == null ? null : u.getTituloEquipado().name();
        return new TokenResponse(jwtService.gerar(u.getUsuario()), u.getUsuario(), u.getEmail(), u.getElo(), u.isAdmin(), titulo);
    }
}
