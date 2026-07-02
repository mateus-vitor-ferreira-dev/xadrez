package com.mateusferreira.xadrez.controller;

import com.mateusferreira.xadrez.controller.dto.EquiparTituloRequest;
import com.mateusferreira.xadrez.controller.dto.TokenResponse;
import com.mateusferreira.xadrez.dominio.Titulo;
import com.mateusferreira.xadrez.seguranca.JwtService;
import com.mateusferreira.xadrez.seguranca.Usuario;
import com.mateusferreira.xadrez.seguranca.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Ações sobre o próprio perfil do usuário logado. Por ora, equipar o TÍTULO
 * exibido ao lado do apelido (parte do "caminho de troféus").
 */
@RestController
@RequestMapping("/usuario")
public class UsuarioController {

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
