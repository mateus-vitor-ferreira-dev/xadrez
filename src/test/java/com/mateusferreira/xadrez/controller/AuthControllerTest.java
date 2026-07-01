package com.mateusferreira.xadrez.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    private org.springframework.test.web.servlet.ResultActions registrar(String usuario, String senha) throws Exception {
        return mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"" + usuario + "\",\"senha\":\"" + senha + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions logar(String usuario, String senha) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"" + usuario + "\",\"senha\":\"" + senha + "\"}"));
    }

    @Test
    void cadastroRetornaTokenEElo1200() throws Exception {
        registrar("jogador_a", "senha123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.usuario").value("jogador_a"))
                .andExpect(jsonPath("$.elo").value(1200));
    }

    @Test
    void naoPermiteUsuarioDuplicado() throws Exception {
        registrar("jogador_b", "senha123").andExpect(status().isOk());
        registrar("jogador_b", "outra123").andExpect(status().isConflict());
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        registrar("jogador_c", "senha123").andExpect(status().isOk());
        logar("jogador_c", "errada").andExpect(status().isUnauthorized());
    }

    @Test
    void loginCorretoRetornaToken() throws Exception {
        registrar("jogador_d", "senha123").andExpect(status().isOk());
        logar("jogador_d", "senha123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}
