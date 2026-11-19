"""Os 4 gráficos do relatório (linha, dispersão, histograma e boxplot), salvos em PNG.

- linha: contratações por mês
- dispersão: faturamento x quantidade de serviços
- histograma: distribuição do faturamento
- boxplot: faturamento por segmento

Os dados vêm da tabela tratada (limpeza.tratar) e do JSON da análise (analise.calcular).
No sistema, o Front-end desenha os mesmos números com Chart.js; estes PNGs são o relatório.
"""
from __future__ import annotations

from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # sem janela: só gera os arquivos
import matplotlib.pyplot as plt  # noqa: E402
import pandas as pd  # noqa: E402
from matplotlib.ticker import MaxNLocator  # noqa: E402

AZUL = "#0060a9"
VERMELHO = "#d71920"


def _reais(valor, _pos=None) -> str:
    """Eixo em milhões: 1850000 vira 'R$ 1,9 mi'."""
    return f"R$ {valor / 1_000_000:.1f} mi".replace(".", ",")


def _salvar(figura, arquivo: Path) -> str:
    figura.tight_layout()
    figura.savefig(arquivo, dpi=120)
    plt.close(figura)
    return arquivo.name


def linha_contratacoes(payload: dict, pasta: Path) -> str:
    meses = payload["porMes"]
    fig, ax = plt.subplots(figsize=(8, 4))
    ax.plot([m["mes"] for m in meses], [m["clientes"] for m in meses], marker="o", color=AZUL)
    ax.set_title("Contratações por mês")
    ax.set_xlabel("Mês")
    ax.set_ylabel("Clientes")
    ax.yaxis.set_major_locator(MaxNLocator(integer=True))  # contagem: só inteiros
    ax.tick_params(axis="x", rotation=45)
    ax.grid(alpha=0.3)
    return _salvar(fig, pasta / "linha_contratacoes.png")


def dispersao_faturamento_servicos(df: pd.DataFrame, payload: dict, pasta: Path) -> str:
    fig, ax = plt.subplots(figsize=(7, 4.5))
    ax.scatter(df["qtd_servicos"], df["faturamento_anual"], color=AZUL, alpha=0.7)
    r = payload["correlacao"]["pearson"]
    titulo = "Faturamento x nº de serviços"
    ax.set_title(titulo if r is None else f"{titulo} (r = {str(r).replace('.', ',')})")
    ax.set_xlabel("Serviços contratados")
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax.set_ylabel("Faturamento anual")
    ax.yaxis.set_major_formatter(_reais)
    ax.grid(alpha=0.3)
    return _salvar(fig, pasta / "dispersao_faturamento_servicos.png")


def histograma_faturamento(df: pd.DataFrame, payload: dict, pasta: Path) -> str:
    fig, ax = plt.subplots(figsize=(7, 4.5))
    ax.hist(df["faturamento_anual"], bins="auto", color=AZUL, edgecolor="white")
    resumo = payload["resumo"]
    ax.axvline(resumo["media"], color=VERMELHO, linestyle="--", label="Média")
    ax.axvline(resumo["mediana"], color="black", linestyle=":", label="Mediana")
    ax.set_title("Distribuição do faturamento anual")
    ax.set_xlabel("Faturamento anual")
    ax.set_ylabel("Clientes")
    ax.xaxis.set_major_formatter(_reais)
    ax.legend()
    return _salvar(fig, pasta / "histograma_faturamento.png")


def boxplot_faturamento_segmento(df: pd.DataFrame, pasta: Path) -> str:
    segmentos = sorted(df["segmento"].unique())
    fig, ax = plt.subplots(figsize=(8, 4.5))
    ax.boxplot([df.loc[df["segmento"] == s, "faturamento_anual"] for s in segmentos], tick_labels=segmentos)
    ax.set_title("Faturamento por segmento")
    ax.set_ylabel("Faturamento anual")
    ax.yaxis.set_major_formatter(_reais)
    ax.grid(axis="y", alpha=0.3)
    return _salvar(fig, pasta / "boxplot_faturamento_segmento.png")


def gerar(df: pd.DataFrame, payload: dict, pasta: Path) -> list[str]:
    """Gera os 4 gráficos em `pasta` e devolve os nomes dos arquivos."""
    pasta.mkdir(parents=True, exist_ok=True)
    return [
        linha_contratacoes(payload, pasta),
        dispersao_faturamento_servicos(df, payload, pasta),
        histograma_faturamento(df, payload, pasta),
        boxplot_faturamento_segmento(df, pasta),
    ]
