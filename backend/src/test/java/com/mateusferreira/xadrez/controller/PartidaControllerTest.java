package com.mateusferreira.xadrez.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da camada web (MockMvc) com as rotas baseadas em id.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PartidaControllerTest {

    @Autowired
    MockMvc mvc;

    /** Cria uma partida via API e devolve o id gerado (lendo o JSON da resposta). */
    private long criarPartida() throws Exception {
        String corpo = mvc.perform(post("/partidas"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(corpo).get("id").asLong();
    }

    @Test
    void novaPartidaRetornaIdEVezDasBrancas() throws Exception {
        mvc.perform(post("/partidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.vez").value("BRANCO"));
    }

    @Test
    void jogadaValidaAlternaAVez() throws Exception {
        long id = criarPartida();

        mvc.perform(post("/partidas/" + id + "/jogadas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origem\":\"e2\",\"destino\":\"e4\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vez").value("PRETO"));
    }

    @Test
    void jogadaIlegalRetorna400() throws Exception {
        long id = criarPartida();

        mvc.perform(post("/partidas/" + id + "/jogadas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origem\":\"e2\",\"destino\":\"e5\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void partidaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/partidas/999999"))
                .andExpect(status().isNotFound());
    }
}
