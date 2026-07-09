"""Prepara os dados de treino do AvaliadorNeural a partir da base do Lichess.

Le o `lichess_db_eval.jsonl.zst` (https://database.lichess.org/#evals) — milhoes
de posicoes ja avaliadas pelo Stockfish, uma por linha:

    {"fen": "2bq1rk1/... b - -", "evals": [{"pvs": [{"cp": -52, ...}], "depth": 30, ...}, ...]}

e converte cada posicao para o contrato do AvaliadorNeural (ver ml/README.md):

- FEATURES relativas a quem esta na vez: 12 planos de 8x8; para as pretas o
  tabuleiro e espelhado na vertical e as cores trocadas.
  indice = (tipo*2 + lado)*64 + (linha*8 + coluna).
- ROTULO = probabilidade de vitoria de quem esta na vez:
  p = sigmoid(cp_da_vez / 173.7178). O cp do Lichess e na otica das BRANCAS,
  entao negamos quando a vez e das pretas. Mate anunciado vira p = 1.0 ou 0.0.

Para nao gastar disco, salvamos as features de forma ESPARSA: cada posicao tem
no maximo 32 pecas, entao guardamos so os INDICES ocupados (uint16, com 65535
de preenchimento) — ~70 bytes/posicao em vez dos 3 KB do vetor denso. O
treina.py densifica lote a lote.

O download e em STREAMING com parada antecipada: baixamos e descomprimimos so
ate juntar --limite posicoes (o arquivo completo tem dezenas de GB; nao
precisamos dele inteiro).

Uso:
    uv run --with zstandard --with numpy python ml/prepara_dados.py --limite 100000
    # ou, com o arquivo ja baixado:
    uv run --with zstandard --with numpy python ml/prepara_dados.py \
        --entrada ~/Downloads/lichess_db_eval.jsonl.zst --limite 1000000
"""

import argparse
import io
import json
import math
import urllib.request
from pathlib import Path

import numpy as np
import zstandard

URL_PADRAO = "https://database.lichess.org/lichess_db_eval.jsonl.zst"
ESCALA_CP = 173.7178          # mesma escala logistica do AvaliadorNeural.java
MAX_PECAS = 32                # limite fisico de pecas numa posicao legal
PREENCHIMENTO = 65535         # indice-sentinela para "sem peca" (uint16 max)

# char do FEN -> tipo 0..5 (peao, cavalo, bispo, torre, rainha, rei)
TIPO_DA_LETRA = {"p": 0, "n": 1, "b": 2, "r": 3, "q": 4, "k": 5}


def indices_da_fen(fen: str) -> tuple[list[int], bool] | None:
    """Converte o FEN nos indices esparsos das 768 features, relativos a vez.

    Devolve (indices, vez_das_brancas), ou None se o FEN for invalido.
    """
    campos = fen.split()
    if len(campos) < 2 or campos[1] not in ("w", "b"):
        return None
    vez_das_brancas = campos[1] == "w"

    indices: list[int] = []
    # O FEN descreve da fileira 8 para a 1; nossa convencao e linha 0 = fileira 1.
    for i, faixa in enumerate(campos[0].split("/")):
        if i > 7:
            return None
        linha = 7 - i
        coluna = 0
        for ch in faixa:
            if ch.isdigit():
                coluna += int(ch)
                continue
            tipo = TIPO_DA_LETRA.get(ch.lower())
            if tipo is None or coluna > 7:
                return None
            peca_branca = ch.isupper()
            # Otica de quem joga: espelha a fileira e troca "minha/dele" p/ pretas.
            linha_rel = linha if vez_das_brancas else 7 - linha
            lado = 0 if peca_branca == vez_das_brancas else 1
            indices.append((tipo * 2 + lado) * 64 + linha_rel * 8 + coluna)
            coluna += 1
    if not indices or len(indices) > MAX_PECAS:
        return None
    return indices, vez_das_brancas


def rotulo(avaliacoes: list[dict], vez_das_brancas: bool) -> float | None:
    """Probabilidade de vitoria de quem joga, a partir da avaliacao mais funda."""
    melhor = max(avaliacoes, key=lambda e: e.get("depth", 0), default=None)
    if not melhor or not melhor.get("pvs"):
        return None
    pv = melhor["pvs"][0]
    if "cp" in pv:
        cp_brancas = pv["cp"]
        cp_vez = cp_brancas if vez_das_brancas else -cp_brancas
        return 1.0 / (1.0 + math.exp(-cp_vez / ESCALA_CP))
    if "mate" in pv:
        brancas_dao_mate = pv["mate"] > 0
        return 1.0 if brancas_dao_mate == vez_das_brancas else 0.0
    return None


def abrir_jsonl(entrada: str):
    """Abre o .jsonl.zst (arquivo local ou URL) como texto, em streaming."""
    if entrada.startswith(("http://", "https://")):
        bruto = urllib.request.urlopen(entrada)  # noqa: S310 — URL do Lichess
    else:
        bruto = open(Path(entrada).expanduser(), "rb")
    fluxo = zstandard.ZstdDecompressor().stream_reader(bruto)
    return io.TextIOWrapper(fluxo, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--entrada", default=URL_PADRAO,
                        help="URL ou caminho do lichess_db_eval.jsonl.zst")
    parser.add_argument("--limite", type=int, default=100_000,
                        help="quantas posicoes coletar (parada antecipada)")
    parser.add_argument("--saida", default=str(Path(__file__).parent / "dados" / "posicoes.npz"))
    args = parser.parse_args()

    indices = np.full((args.limite, MAX_PECAS), PREENCHIMENTO, dtype=np.uint16)
    alvos = np.zeros(args.limite, dtype=np.float32)
    coletadas = puladas = 0

    with abrir_jsonl(args.entrada) as linhas:
        for linha in linhas:
            if coletadas >= args.limite:
                break
            try:
                registro = json.loads(linha)
                convertido = indices_da_fen(registro["fen"])
                if convertido is None:
                    puladas += 1
                    continue
                idx, vez_das_brancas = convertido
                p = rotulo(registro.get("evals", []), vez_das_brancas)
                if p is None:
                    puladas += 1
                    continue
            except (KeyError, ValueError, TypeError):
                puladas += 1
                continue
            indices[coletadas, : len(idx)] = idx
            alvos[coletadas] = p
            coletadas += 1
            if coletadas % 50_000 == 0:
                print(f"{coletadas} posicoes coletadas ({puladas} puladas)...")

    destino = Path(args.saida)
    destino.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(destino, indices=indices[:coletadas], alvos=alvos[:coletadas])
    tamanho_mb = destino.stat().st_size / 1_048_576
    print(f"{coletadas} posicoes salvas em {destino} ({tamanho_mb:.1f} MB; {puladas} puladas)")


if __name__ == "__main__":
    main()
