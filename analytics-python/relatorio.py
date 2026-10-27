"""Relatório de insights em arquivos (Plano PI: pasta output/), sem banco e sem Spring.

Uso (com o .venv ligado):
    python relatorio.py                          # usa amostras/lote_exemplo.json
    python relatorio.py amostras/clientes.json   # qualquer JSON no formato da aula 10

Gera em output/:
    indicadores.json   o JSON do dashboard (analise.calcular) + os testes estatísticos
    insights.json      as frases dos insights + a interpretação dos testes
    graficos/*.png     linha, dispersão, histograma e boxplot

No sistema, o mesmo cálculo roda na API (POST /etl) e vai para o banco; este script é o relatório.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import pandas as pd

import graficos
from analise import calcular
from limpeza import tratar

PASTA = Path(__file__).resolve().parent
PADRAO = PASTA / "amostras" / "lote_exemplo.json"


def gerar(arquivo: Path, saida: Path) -> dict:
    linhas = json.loads(arquivo.read_text(encoding="utf-8"))
    res = tratar(pd.DataFrame(linhas).astype(object))
    payload = calcular(res, len(linhas))  # já traz os testes estatísticos e as frases deles
    arquivos = graficos.gerar(res.tratado, payload, saida / "graficos")

    saida.mkdir(parents=True, exist_ok=True)
    (saida / "indicadores.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    (saida / "insights.json").write_text(json.dumps(payload["insights"], ensure_ascii=False, indent=2), encoding="utf-8")
    return {"clientes": len(res.tratado), "recusadas": len(res.rejeitadas), "insights": len(payload["insights"]),
            "graficos": arquivos}


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # console do Windows: acentos no print
    entrada = Path(sys.argv[1]) if len(sys.argv) > 1 else PADRAO
    resumo = gerar(entrada, PASTA / "output")
    print(f"{resumo['clientes']} clientes tratados ({resumo['recusadas']} recusados), "
          f"{resumo['insights']} insights e {len(resumo['graficos'])} gráficos em output/")
