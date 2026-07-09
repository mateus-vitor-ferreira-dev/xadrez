package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.AtualizarPerfilRequest;
import com.mateusferreira.xadrez.controller.dto.EquiparTituloRequest;
import com.mateusferreira.xadrez.controller.dto.PerfilResponse;
import com.mateusferreira.xadrez.controller.dto.TokenResponse;
import com.mateusferreira.xadrez.dominio.Titulo;
import com.mateusferreira.xadrez.seguranca.JwtService;
import com.mateusferreira.xadrez.seguranca.Usuario;
import com.mateusferreira.xadrez.seguranca.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.regex.Pattern;

/**
 * Ações sobre o próprio perfil do usuário logado: equipar o TÍTULO exibido ao
 * lado do apelido (parte do "caminho de troféus") e editar as informações do
 * perfil (e-mail, telefone e foto) pela tela de perfil.
 */
@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    // Mesma validação leve de e-mail do cadastro (AuthController): algo@algo.dominio,
    // sem espaços. Não é a RFC inteira, mas barra o obviamente inválido.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UsuarioRepository repository;
    private final JwtService jwtService;

    public UsuarioController(UsuarioRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    /**
     * PUT /usuario/titulo — equipa (ou remove) o título mostrado ao lado do
     * apelido. Corpo {@code {"titulo":"CAVALEIRO"}} para equipar; {@code null}
     * (ou vazio) para tirar. Só aceita títulos DESBLOQUEADOS pelo Elo do usuário
     * (admin libera todos). Devolve a sessão atualizada, como no login.
     */
    @PutMapping("/titulo")
    public TokenResponse equiparTitulo(@RequestBody EquiparTituloRequest req, Principal principal) {
        Usuario u = usuarioLogado(principal);
        Titulo titulo = parse(req.titulo());
        if (titulo != null && !u.isAdmin() && !titulo.liberadoPara(u.getElo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esse título ainda não foi desbloqueado.");
        }
        u.setTituloEquipado(titulo);
        repository.save(u);
        String nome = titulo == null ? null : titulo.name();
        return new TokenResponse(jwtService.gerar(u.getUsuario()), u.getUsuario(), u.getEmail(), u.getElo(), u.isAdmin(), nome);
    }

    /**
     * GET /usuario/perfil — devolve os dados atuais do perfil para PREENCHER a
     * tela de edição (apelido, e-mail, telefone e foto). Exige login.
     */
    @GetMapping("/perfil")
    public PerfilResponse meuPerfil(Principal principal) {
        return perfilDe(usuarioLogado(principal));
    }

    /**
     * PUT /usuario/perfil — atualiza e-mail e telefone do usuário logado.
     * O e-mail é obrigatório e validado (formato + unicidade entre OUTRAS contas);
     * telefone é opcional (vazio limpa o campo). Não mexe em apelido nem senha.
     * Devolve o perfil já atualizado.
     */
    @PutMapping("/perfil")
    public PerfilResponse atualizarPerfil(@RequestBody AtualizarPerfilRequest req, Principal principal) {
        Usuario u = usuarioLogado(principal);

        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um e-mail válido.");
        }
        // Só checa colisão se o e-mail realmente mudou — e ignora a própria conta.
        if (!email.equals(u.getEmail()) && repository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse e-mail já está cadastrado.");
        }

        u.setEmail(email);
        u.setTelefone(normalizar(req.telefone()));
        repository.save(u);
        return perfilDe(u);
    }

    /** Monta o DTO de perfil a partir da entidade. */
    private PerfilResponse perfilDe(Usuario u) {
        return new PerfilResponse(u.getUsuario(), u.getEmail(), u.getTelefone());
    }

    /** Campo opcional: trim + vazio/branco vira NULL (limpa a coluna). */
    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    /** O usuário logado (pelo apelido no token); 401 se anônimo/sessão inválida. */
    private Usuario usuarioLogado(Principal principal) {
        if (principal == null || "anonymousUser".equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para equipar um título.");
        }
        return repository.findByUsuario(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    /** Converte o nome recebido no enum; null/vazio = sem título. */
    private Titulo parse(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        try {
            return Titulo.valueOf(nome);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título inválido.");
        }
    }
}
