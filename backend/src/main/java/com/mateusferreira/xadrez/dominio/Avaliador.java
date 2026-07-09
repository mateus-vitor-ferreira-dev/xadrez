package com.mateusferreira.xadrez.dominio;

/**
 * Função de avaliação da IA: dá uma NOTA à posição do ponto de vista de 'cor'
 * (positivo = bom para 'cor'), em centipeões. É o ponto de extensão que permite
 * trocar a heurística — material, posicional, neural... — sem tocar na busca do
 * {@link MotorIA}.
 *
 * <p>Contrato importante para o negamax: a nota é SEMPRE relativa a 'cor'. Uma
 * implementação neural treinada na ótica das brancas, por exemplo, precisa negar
 * o resultado quando 'cor' for {@link Cor#PRETO}.
 */
public interface Avaliador {

    int avaliar(Partida partida, Cor cor);
}
