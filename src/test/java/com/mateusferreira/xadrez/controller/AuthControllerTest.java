package com.mateusferreira.xadrez.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    private ResultActions registrar(String usuario, String email, String senha) throws Exception {
        return mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"" + usuario + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }

    private ResultActions logar(String identificador, String senha) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificador\":\"" + identificador + "\",\"senha\":\"" + senha + "\"}"));
    }

    @Test
    void cadastroRetornaTokenEEloInicial() throws Exception {
        // Todo novo jogador nasce na faixa Iniciante (Elo 800).
        registrar("jogador_a", "a@teste.com", "senha123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.usuario").value("jogador_a"))
                .andExpect(jsonPath("$.email").value("a@teste.com"))
                .andExpect(jsonPath("$.elo").value(800));
    }

    @Test
    void cadastroComEmailInvalidoRetorna400() throws Exception {
        registrar("jogador_x", "email-invalido", "senha123").andExpect(status().isBadRequest());
    }

    @Test
    void naoPermiteApelidoDuplicado() throws Exception {
        registrar("jogador_b", "b1@teste.com", "senha123").andExpect(status().isOk());
        registrar("jogador_b", "b2@teste.com", "outra123").andExpect(status().isConflict());
    }

    @Test
    void naoPermiteEmailDuplicado() throws Exception {
        registrar("jogador_e1", "mesmo@teste.com", "senha123").andExpect(status().isOk());
        registrar("jogador_e2", "mesmo@teste.com", "outra123").andExpect(status().isConflict());
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        registrar("jogador_c", "c@teste.com", "senha123").andExpect(status().isOk());
        logar("jogador_c", "errada").andExpect(status().isUnauthorized());
    }

    @Test
    void loginPorApelidoFunciona() throws Exception {
        registrar("jogador_d", "d@teste.com", "senha123").andExpect(status().isOk());
        logar("jogador_d", "senha123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginPorEmailFunciona() throws Exception {
        registrar("jogador_f", "f@teste.com", "senha123").andExpect(status().isOk());
        logar("f@teste.com", "senha123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("jogador_f"));
    }
}
