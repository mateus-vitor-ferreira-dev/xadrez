# ml/ — pipeline de aprendizado do AvaliadorNeural

Aqui vive o lado Python da fase neural da IA: geração de dados, treino e
exportação do modelo em ONNX consumido pelo `AvaliadorNeural` (Java).

## Contrato Java <-> modelo

Definido em `backend/.../dominio/AvaliadorNeural.java` e reproduzido pelo
`gera_modelo_dummy.py`:

- **Entrada** `features` `float[1][768]`: 12 planos de 8x8 achatados, na
  **ótica de quem avalia** — para as pretas o tabuleiro é espelhado na
  vertical e as cores trocadas (a rede sempre "olha do seu lado"). Índice =
  `(tipo*2 + lado)*64 + (linha*8 + coluna)`; tipo 0..5 = peão, cavalo, bispo,
  torre, rainha, rei; lado 0 = peça de quem avalia, 1 = do oponente.
  Casa ocupada = 1.0.
- **Saída** `float[1][1]`: probabilidade de vitória de **quem avalia** (0..1).
  O Java converte para centipeões (`cp = 173.7178 * ln(p/(1-p))`), já no
  ponto de vista que o negamax espera — sem negação.
- Por que relativa? A avaliação depende de quem está na vez (tempo); a
  codificação relativa dá isso à rede de graça, elimina o risco de esquecer a
  negação e corta o espaço de entrada pela metade por simetria.
- Limitações conhecidas da v1: roque e en passant não são codificados.

## Scripts (rodar da raiz do repositório)

1. **`prepara_dados.py`** — baixa em streaming (com parada antecipada) o
   `lichess_db_eval.jsonl.zst`, converte FEN -> features relativas à vez e
   rótulo cp -> probabilidade, e salva esparso em `ml/dados/posicoes.npz`
   (~70 bytes/posição):

   ```
   uv run --with zstandard --with numpy python ml/prepara_dados.py --limite 500000
   ```

2. **`treina.py`** — treina o MLP 768 -> 256 -> 32 -> 1 (MSE sobre a
   probabilidade), exporta `ml/saida/modelo.onnx` montando o grafo à mão com
   `onnx.helper` (sem depender do exportador do torch) e confere o export
   numericamente contra o torch via onnxruntime:

   ```
   uv run --with torch --with numpy --with onnx --with onnxruntime \
       python ml/treina.py --epocas 10
   ```

3. **`gera_modelo_dummy.py`** — gera o modelo de teste
   (`backend/src/test/resources/ia/modelo-dummy.onnx`), cujos pesos reproduzem
   a contagem de material; valida a integração Java sem depender de treino:

   ```
   uv run --with onnx --with numpy python ml/gera_modelo_dummy.py
   ```

`ml/dados/` e `ml/saida/` ficam fora do git (ver `.gitignore` da raiz).

## Próximos passos (fase 4)

- Torneio/ablation entre AvaliadorMaterial, AvaliadorPosicional e
  AvaliadorNeural (suíte de aberturas variadas — auto-jogo determinístico
  repete partidas; taxa de vitória com intervalo de confiança).
- Enriquecer features (roque/en passant) e dataset maior, se o torneio
  justificar.
