# ml/ — pipeline de aprendizado do AvaliadorNeural

Aqui vive o lado Python da fase neural da IA: geração de dados, treino e
exportação do modelo em ONNX consumido pelo `AvaliadorNeural` (Java).

## Contrato Java <-> modelo

Definido em `backend/.../dominio/AvaliadorNeural.java` e reproduzido pelo
`gera_modelo_dummy.py`:

- **Entrada** `features` `float[1][768]`: 12 planos de 8x8 achatados,
  índice = `(tipo*2 + cor)*64 + (linha*8 + coluna)`; tipo 0..5 =
  peão, cavalo, bispo, torre, rainha, rei; cor 0 = branco, 1 = preto.
  Coordenadas absolutas (ótica das brancas), casa ocupada = 1.0.
- **Saída** `float[1][1]`: probabilidade de vitória das BRANCAS (0..1).
  O Java converte para centipeões (`cp = 173.7178 * ln(p/(1-p))`) e nega
  quando é a vez das pretas.

## Scripts

- `gera_modelo_dummy.py` — gera o modelo de teste
  (`backend/src/test/resources/ia/modelo-dummy.onnx`), cujos pesos reproduzem
  a contagem de material. Rode com:

  ```
  uv run --with onnx --with numpy python ml/gera_modelo_dummy.py
  ```

## Próximos passos (fase 3)

1. Baixar posições rotuladas da base aberta do Lichess (`lichess_db_eval.jsonl`).
2. Converter cada posição nas 768 features + rótulo cp -> probabilidade (sigmoid).
3. Treinar uma rede pequena (PyTorch) e exportar em ONNX seguindo o contrato.
4. Torneio/ablation entre AvaliadorMaterial, AvaliadorPosicional e AvaliadorNeural
   (suíte de aberturas variadas — auto-jogo determinístico repete partidas).
