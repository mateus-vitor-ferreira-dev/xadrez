"""Gera o modelo ONNX DUMMY usado nos testes do AvaliadorNeural (Java).

O dummy segue o MESMO contrato que o modelo treinado de verdade seguirá
(ver AvaliadorNeural.java):

  entrada: "features" float[1][768] — 12 planos de 8x8 achatados.
           indice = (tipo*2 + cor)*64 + (linha*8 + coluna)
           tipo 0..5 = peao, cavalo, bispo, torre, rainha, rei
           cor 0 = branco, 1 = preto; casa ocupada = 1.0
  saida:   "prob_vitoria_brancas" float[1][1] — probabilidade 0..1

Em vez de pesos treinados, o dummy usa o VALOR DE MATERIAL de cada peca:
peso = +valor_cp/ESCALA para pecas brancas e -valor_cp/ESCALA para pretas,
com uma sigmoid na saida. Como o Java converte de volta com
cp = ESCALA * ln(p/(1-p)), o resultado e EXATAMENTE a diferenca de material
em centipeoes — ou seja, o dummy se comporta como o AvaliadorMaterial.
Isso permite testar toda a integracao (features -> ONNX -> conversao -> sinal)
com um comportamento conhecido, antes de existir um modelo treinado.

Uso (gera backend/src/test/resources/ia/modelo-dummy.onnx):
    uv run --with onnx --with numpy python ml/gera_modelo_dummy.py
"""

from pathlib import Path

import numpy as np
import onnx
from onnx import TensorProto, helper

# Mesma escala logistica do AvaliadorNeural.java (400/ln 10).
ESCALA_CP = 173.7178

# Valores classicos em centipeoes, na ordem dos planos (rei = 0: nunca capturado).
VALORES_CP = [100, 320, 330, 500, 900, 0]  # peao, cavalo, bispo, torre, rainha, rei

FEATURES = 12 * 64


def pesos_material() -> np.ndarray:
    """Vetor [768, 1]: +valor/ESCALA nos planos brancos, -valor/ESCALA nos pretos."""
    w = np.zeros((FEATURES, 1), dtype=np.float32)
    for tipo, valor_cp in enumerate(VALORES_CP):
        peso = valor_cp / ESCALA_CP
        w[(tipo * 2 + 0) * 64 : (tipo * 2 + 1) * 64, 0] = peso   # brancas
        w[(tipo * 2 + 1) * 64 : (tipo * 2 + 2) * 64, 0] = -peso  # pretas
    return w


def main() -> None:
    entrada = helper.make_tensor_value_info("features", TensorProto.FLOAT, [1, FEATURES])
    saida = helper.make_tensor_value_info("prob_vitoria_brancas", TensorProto.FLOAT, [1, 1])

    pesos = helper.make_tensor(
        "pesos", TensorProto.FLOAT, [FEATURES, 1], pesos_material().flatten().tolist()
    )

    grafo = helper.make_graph(
        nodes=[
            helper.make_node("MatMul", ["features", "pesos"], ["logito"]),
            helper.make_node("Sigmoid", ["logito"], ["prob_vitoria_brancas"]),
        ],
        name="avaliador-dummy-material",
        inputs=[entrada],
        outputs=[saida],
        initializer=[pesos],
    )

    modelo = helper.make_model(
        grafo, opset_imports=[helper.make_opsetid("", 17)], ir_version=8
    )
    onnx.checker.check_model(modelo)

    destino = (
        Path(__file__).resolve().parent.parent
        / "backend" / "src" / "test" / "resources" / "ia" / "modelo-dummy.onnx"
    )
    destino.parent.mkdir(parents=True, exist_ok=True)
    onnx.save(modelo, str(destino))
    print(f"modelo dummy salvo em {destino} ({destino.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
