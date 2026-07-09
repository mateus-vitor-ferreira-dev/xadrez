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

## Fase 4 — torneio entre avaliadores (ablation study)

O harness vive no backend (`com.mateusferreira.xadrez.torneio`): os três
avaliadores jogam entre si com a mesma busca e o mesmo tempo por lance, sobre
uma suíte de 20 aberturas jogadas com as duas cores. Rodar (da pasta
`backend/`):

```
mvn -q compile exec:java \
    -Dexec.args="--tempo 100 --aberturas 20 --modelo ../ml/saida/modelo.onnx"
```

Saída: placar por pareamento com IC 95% e diferença de Elo.

### Primeiros resultados (2026-07-09; 500k posições, 10 épocas, 100 ms/lance)

| Pareamento             | Placar (1º) | Score        | Elo (IC 95%)        |
|------------------------|-------------|--------------|---------------------|
| Material x Posicional  | +2 =31 -7   | 43,8% ± 7,1  | -44 [-95, +6]       |
| Material x Neural      | +14 =25 -1  | 66,3% ± 8,0  | +117 [+58, +184]    |
| Posicional x Neural    | +32 =6 -2   | 87,5% ± 8,3  | +338 [+232, +544]   |

Leitura: as piece-square tables valem ~+44 Elo sobre material puro (IC ainda
cruza o zero; mais jogos apertariam a barra). A rede v1 PERDE para os dois,
com significância. Diagnóstico: (1) 500k posições/10 épocas dão MAE ~275 cp na
validação — avaliação mais grosseira que uma PST calibrada; (2) a inferência
ONNX custa ordens de magnitude mais por nó que a avaliação artesanal, e com o
MESMO tempo por lance a rede busca bem mais raso. Resultado negativo, medido e
explicado — é o que o ablation existe para mostrar.

### Rede v2 (2026-07-09; 2M posições, 30 épocas, checkpoint do melhor epoch)

O treino longo SOBREAJUSTOU a partir da época 10 (val MSE 0,02922 na ép. 10 ->
0,03016 na ép. 30, com o MSE de treino sempre caindo); o checkpoint por
validação exportou a época 10. Torneio com a mesma configuração da v1:

| Pareamento             | Placar (1º) | Score         | Elo (IC 95%)      | v1 (referência)  |
|------------------------|-------------|---------------|-------------------|------------------|
| Material x Posicional  | +3 =31 -6   | 46,3% ± 7,3   | -26 [-78, +24]    | -44 [-95, +6]    |
| Material x Neural      | +14 =21 -5  | 61,3% ± 10,1  | +80 [+8, +158]    | +117 [+58, +184] |
| Posicional x Neural    | +23 =15 -2  | 76,3% ± 9,2   | +203 [+124, +307] | +338 [+232, +544]|

Leitura: 4x mais dados valeram **~+135 Elo** para a rede contra o Posicional
(os ICs de v1 e v2 quase não se sobrepõem) e ~+37 contra o Material — mas ela
ainda perde para os dois. Com o ganho por dados desacelerando e o sobreajuste
chegando cedo, o gargalo aparente passa a ser a CAPACIDADE do modelo (MLP
pequeno sobre planos binários) e o CUSTO por nó, não a quantidade de dados.
De bônus, Material x Posicional replicou dentro do IC da primeira rodada.

## Melhorias futuras

- Rede maior e/ou features mais ricas (roque/en passant, planos de ataque):
  a v2 mostrou que mais dados sozinhos não fecham o gap — o gargalo agora é
  capacidade do modelo.
- Reduzir o custo por nó: lote de inferências, ou usar a rede só na folha da
  quiescência (as buscas internas com avaliação barata).
- Regularização/lr-schedule para adiar o sobreajuste (chegou na época 10).
- Mais aberturas na suíte para ICs mais apertados.
