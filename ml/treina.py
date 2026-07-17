"""Treina o AvaliadorNeural e exporta o modelo em ONNX.

MLP 780 -> 512 -> 64 -> 1 com sigmoid na saida, treinado para prever a
PROBABILIDADE DE VITORIA de quem esta na vez, sobre as posicoes preparadas
pelo prepara_dados.py (12 planos de pecas + roque + en passant). A perda e
MSE sobre a probabilidade — o mesmo alvo suave (cp -> sigmoid) usado por
redes de avaliacao classicas.

A exportacao ONNX e feita A MAO com onnx.helper a partir dos pesos treinados
(MatMul/Add/Relu/Sigmoid), em vez de torch.onnx.export: o grafo resultante e
identico ao contrato do AvaliadorNeural.java (mesma abordagem do
gera_modelo_dummy.py) e nao depende da API de exportacao do torch, que muda
entre versoes. No final o modelo exportado e CONFERIDO numericamente contra o
torch com o onnxruntime.

Uso:
    uv run --with torch --with numpy --with onnx --with onnxruntime \
        python ml/treina.py --epocas 10
"""

import argparse
import math
from pathlib import Path

import numpy as np
import onnx
import onnxruntime
import torch
from onnx import TensorProto, helper
from torch import nn

ESCALA_CP = 173.7178  # mesma escala logistica do AvaliadorNeural.java
FEATURES = 12 * 64 + 4 + 8  # planos + roque + en passant = 780
SEMENTE = 20260709


class RedeAvaliadora(nn.Module):
    """MLP 780 -> 512 -> 64 -> 1; sigmoid na saida (probabilidade de vitoria)."""

    def __init__(self) -> None:
        super().__init__()
        self.corpo = nn.Sequential(
            nn.Linear(FEATURES, 512), nn.ReLU(),
            nn.Linear(512, 64), nn.ReLU(),
            nn.Linear(64, 1), nn.Sigmoid(),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.corpo(x)


def densificar(indices: np.ndarray) -> np.ndarray:
    """Lote esparso (B, 32) de indices uint16 -> lote denso (B, 768) float32."""
    lote = np.zeros((indices.shape[0], FEATURES), dtype=np.float32)
    linhas, colunas = np.nonzero(indices != 65535)
    lote[linhas, indices[linhas, colunas]] = 1.0
    return lote


def mae_em_centipeoes(previsto: np.ndarray, alvo: np.ndarray) -> float:
    """Erro medio absoluto em centipeoes (logit dos dois lados, com trava).

    A trava em [1e-4, 1-1e-4] (~ +-1600 cp) evita que mates (p = 0/1) explodam
    o logit para infinito e dominem a media.
    """
    p = np.clip(previsto, 1e-4, 1 - 1e-4)
    a = np.clip(alvo, 1e-4, 1 - 1e-4)
    cp_p = ESCALA_CP * np.log(p / (1 - p))
    cp_a = ESCALA_CP * np.log(a / (1 - a))
    return float(np.abs(cp_p - cp_a).mean())


def exportar_onnx(modelo: RedeAvaliadora, destino: Path) -> None:
    """Monta o grafo ONNX a mao a partir dos pesos treinados (contrato fixo)."""
    pesos = {nome: p.detach().numpy() for nome, p in modelo.corpo.named_parameters()}
    # nn.Linear guarda W como [saida, entrada]; MatMul espera [entrada, saida].
    iniciais = []
    nos = []
    anterior = "features"
    camadas = [("0", "Relu"), ("2", "Relu"), ("4", "Sigmoid")]
    for i, (idx, ativacao) in enumerate(camadas):
        w, b = f"w{i}", f"b{i}"
        iniciais.append(helper.make_tensor(
            w, TensorProto.FLOAT, list(pesos[f"{idx}.weight"].T.shape),
            pesos[f"{idx}.weight"].T.flatten().tolist()))
        iniciais.append(helper.make_tensor(
            b, TensorProto.FLOAT, list(pesos[f"{idx}.bias"].shape),
            pesos[f"{idx}.bias"].flatten().tolist()))
        ultima = i == len(camadas) - 1
        pos_ativacao = "prob_vitoria" if ultima else f"ativ{i}"
        nos.append(helper.make_node("MatMul", [anterior, w], [f"mat{i}"]))
        nos.append(helper.make_node("Add", [f"mat{i}", b], [f"soma{i}"]))
        nos.append(helper.make_node(ativacao, [f"soma{i}"], [pos_ativacao]))
        anterior = pos_ativacao

    grafo = helper.make_graph(
        nodes=nos,
        name="avaliador-neural",
        inputs=[helper.make_tensor_value_info("features", TensorProto.FLOAT, ["lote", FEATURES])],
        outputs=[helper.make_tensor_value_info("prob_vitoria", TensorProto.FLOAT, ["lote", 1])],
        initializer=iniciais,
    )
    modelo_onnx = helper.make_model(
        grafo, opset_imports=[helper.make_opsetid("", 17)], ir_version=8
    )
    onnx.checker.check_model(modelo_onnx)
    destino.parent.mkdir(parents=True, exist_ok=True)
    onnx.save(modelo_onnx, str(destino))


def conferir_export(modelo: RedeAvaliadora, caminho: Path, amostra: np.ndarray) -> float:
    """Confere o ONNX exportado contra o torch; devolve a maior divergencia."""
    sessao = onnxruntime.InferenceSession(str(caminho))
    saida_onnx = sessao.run(None, {"features": amostra})[0].reshape(-1)
    with torch.no_grad():
        saida_torch = modelo(torch.from_numpy(amostra)).numpy().reshape(-1)
    return float(np.abs(saida_onnx - saida_torch).max())


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dados", default=str(Path(__file__).parent / "dados" / "posicoes.npz"))
    parser.add_argument("--saida", default=str(Path(__file__).parent / "saida" / "modelo.onnx"))
    parser.add_argument("--epocas", type=int, default=10)
    parser.add_argument("--lote", type=int, default=1024)
    parser.add_argument("--taxa", type=float, default=1e-3)
    args = parser.parse_args()

    torch.manual_seed(SEMENTE)
    rng = np.random.default_rng(SEMENTE)

    d = np.load(args.dados)
    indices, alvos = d["indices"], d["alvos"]
    n = len(alvos)
    embaralhado = rng.permutation(n)
    corte = max(1, int(n * 0.05))  # 5% para validacao
    val_ids, treino_ids = embaralhado[:corte], embaralhado[corte:]
    print(f"{len(treino_ids)} posicoes de treino, {len(val_ids)} de validacao")

    modelo = RedeAvaliadora()
    otimizador = torch.optim.Adam(modelo.parameters(), lr=args.taxa)
    perda_mse = nn.MSELoss()

    x_val = torch.from_numpy(densificar(indices[val_ids]))
    y_val = torch.from_numpy(alvos[val_ids]).unsqueeze(1)

    # Guardamos (e exportamos) o MELHOR epoch pela validacao, nao o ultimo:
    # em treinos longos o final pode sobreajustar; e exportar a cada melhora
    # deixa o melhor-ate-agora no disco mesmo se a rodada for interrompida.
    destino = Path(args.saida)
    melhor_val = float("inf")
    melhor_epoca = 0

    for epoca in range(1, args.epocas + 1):
        modelo.train()
        ordem = rng.permutation(treino_ids)
        soma_perda = passos = 0
        for inicio in range(0, len(ordem), args.lote):
            ids = ordem[inicio : inicio + args.lote]
            x = torch.from_numpy(densificar(indices[ids]))
            y = torch.from_numpy(alvos[ids]).unsqueeze(1)
            otimizador.zero_grad()
            perda = perda_mse(modelo(x), y)
            perda.backward()
            otimizador.step()
            soma_perda += perda.item()
            passos += 1

        modelo.eval()
        with torch.no_grad():
            prev_val = modelo(x_val)
            perda_val = perda_mse(prev_val, y_val).item()
            mae = mae_em_centipeoes(prev_val.numpy().reshape(-1), alvos[val_ids])
        novo_melhor = perda_val < melhor_val
        if novo_melhor:
            melhor_val, melhor_epoca = perda_val, epoca
            melhor_estado = {k: v.clone() for k, v in modelo.state_dict().items()}
            exportar_onnx(modelo, destino)  # melhor-ate-agora sempre no disco
        print(f"epoca {epoca:2d}: treino MSE {soma_perda / passos:.5f} | "
              f"val MSE {perda_val:.5f} | val MAE ~{mae:.0f} cp"
              + (" | * exportado" if novo_melhor else ""))

    # Recarrega o melhor epoch e exporta/confere a partir dele.
    modelo.load_state_dict(melhor_estado)
    exportar_onnx(modelo, destino)
    divergencia = conferir_export(modelo, destino, densificar(indices[val_ids[:64]]))
    tamanho_kb = destino.stat().st_size / 1024
    print(f"melhor epoch: {melhor_epoca} (val MSE {melhor_val:.5f})")
    print(f"modelo salvo em {destino} ({tamanho_kb:.0f} KB); "
          f"divergencia onnx vs torch: {divergencia:.2e}")
    assert divergencia < 1e-5, "export ONNX divergiu do torch!"


if __name__ == "__main__":
    main()
