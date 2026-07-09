package com.mateusferreira.xadrez.seguranca;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Valida o "ID token" que o botão do Google (GIS) devolve no frontend. A lib
 * oficial confere a assinatura (contra as chaves públicas do Google), o emissor
 * e a <b>audiência</b> (tem que ser o NOSSO Client ID) e a validade.
 *
 * <p>Se o token é válido, extraímos o e-mail (já verificado pelo Google) e o
 * nome. Não guardamos nada do Google além disso — a sessão continua sendo o
 * nosso próprio JWT.
 */
@Service
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(List.of(clientId))
                .build();
    }

    /** Valida o credential e devolve (e-mail, nome), ou 401 se inválido. */
    public GoogleUser verificar(String credential) {
        GoogleIdToken token;
        try {
            token = verifier.verify(credential);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falha ao validar o login do Google.");
        }
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login do Google inválido ou expirado.");
        }
        GoogleIdToken.Payload p = token.getPayload();
        if (!Boolean.TRUE.equals(p.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail do Google não verificado.");
        }
        String nome = (String) p.get("name");
        return new GoogleUser(p.getEmail(), nome == null ? "" : nome);
    }

    /** Dados mínimos que aproveitamos do Google. */
    public record GoogleUser(String email, String nome) {
    }
}
