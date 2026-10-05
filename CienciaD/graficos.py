"""Gráficos do relatório de insights, salvos como PNG.

Os 4 tipos da ementa (linha, dispersão, histograma, boxplot) e as barras de
distribuição por segmento e por nível. O dashboard redesenha os mesmos dados no
navegador a partir do indicadores.json; os PNGs servem ao relatório e à apresentação.
"""

from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # sem janela: roda em servidor e chamado pelo Java

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.ticker import FuncFormatter, MaxNLocator

from analise import formatar_p
from esquema import NIVEIS

AZUL = "#2563eb"
CINZA = "#64748b"
CORES_NIVEL = {"A": "#1d4ed8", "B": "#60a5fa", "C": "#bfdbfe"}

plt.rcParams.update({
    "figure.figsize": (9, 5),
    "figure.dpi": 110,
    "axes.spines.top": False,
    "axes.spines.right": False,
    "axes.titleweight": "bold",
    "axes.titlesize": 13,
    "axes.grid": True,
    "grid.alpha": 0.25,
})

milhoes = FuncFormatter(lambda v, _: f"R$ {v / 1e6:.1f} mi".replace(".", ","))


def gerar(dados, indicadores, pasta):
    """Gera todos os gráficos em `pasta` e devolve os nomes dos arquivos."""
    pasta = Path(pasta)
    pasta.mkdir(parents=True, exist_ok=True)
    feitos = [
        _salvar(linha_contratacoes(indicadores), pasta / "linha_contratacoes.png"),
        _salvar(dispersao_faturamento_servicos(dados, indicadores), pasta / "dispersao_faturamento_servicos.png"),
        _salvar(histograma_faturamento(dados, indicadores), pasta / "histograma_faturamento.png"),
        _salvar(boxplot_faturamento_segmento(dados), pasta / "boxplot_faturamento_segmento.png"),
        _salvar(barras_segmento(indicadores), pasta / "barras_segmento.png"),
        _salvar(barras_nivel(indicadores), pasta / "barras_nivel.png"),
    ]
    return [arquivo.name for arquivo in feitos]


def linha_contratacoes(indicadores):
    serie = indicadores["contratacoes"]["por_mes"]
    meses = [p["mes"] for p in serie]
    fig, ax = plt.subplots()
    ax.plot(meses, [p["clientes"] for p in serie], marker="o", color=AZUL, label="Novos clientes")
    ax.plot(meses, [p["contratos"] for p in serie], marker="s", color=CINZA, linestyle="--", label="Serviços contratados")
    ax.set_title("Evolução das contratações por mês")
    ax.set_ylabel("Quantidade")
    ax.yaxis.set_major_locator(MaxNLocator(integer=True))
    ax.set_xticks(range(len(meses)), [m[5:] + "/" + m[2:4] for m in meses], rotation=45, ha="right")
    ax.legend(frameon=False)
    return fig


def dispersao_faturamento_servicos(dados, indicadores):
    teste = indicadores["testes"]["correlacao_faturamento_servicos"]
    fig, ax = plt.subplots()
    for nivel in NIVEIS:
        grupo = dados[dados["nivel"] == nivel]
        # Deslocamento pequeno no eixo x: vários clientes têm a mesma quantidade de serviços.
        deslocamento = np.linspace(-0.12, 0.12, len(grupo)) if len(grupo) > 1 else 0
        ax.scatter(grupo["qtd_servicos"] + deslocamento, grupo["faturamento"], s=60,
                   color=CORES_NIVEL[nivel], edgecolor="#1e3a8a", label=f"Nível {nivel}")
    ax.set_title("Faturamento × quantidade de serviços contratados")
    ax.set_xlabel("Serviços contratados")
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax.yaxis.set_major_formatter(milhoes)
    ax.text(0.02, 0.95, f"Spearman ρ = {teste['rho']:.2f}".replace(".", ",") + f"  ({formatar_p(teste['p_valor'])})",
            transform=ax.transAxes, va="top", color=CINZA)
    ax.legend(frameon=False, loc="lower right")
    return fig


def histograma_faturamento(dados, indicadores):
    geral = indicadores["faturamento"]["geral"]
    fig, ax = plt.subplots()
    ax.hist(dados["faturamento"], bins="sturges", color=AZUL, alpha=0.85, edgecolor="white")
    ax.axvline(geral["media"], color="#dc2626", linestyle="--", label="Média")
    ax.axvline(geral["mediana"], color="#16a34a", linestyle="-", label="Mediana")
    ax.set_title("Distribuição do faturamento anual dos clientes")
    ax.set_ylabel("Clientes")
    ax.xaxis.set_major_formatter(milhoes)
    ax.yaxis.set_major_locator(MaxNLocator(integer=True))
    ax.legend(frameon=False)
    return fig


def boxplot_faturamento_segmento(dados):
    # Ordenado pela mediana: a leitura do gráfico fica de cima para baixo.
    ordem = dados.groupby("segmento")["faturamento"].median().sort_values().index
    fig, ax = plt.subplots()
    ax.boxplot([dados.loc[dados["segmento"] == s, "faturamento"] for s in ordem],
               tick_labels=list(ordem), vert=False, patch_artist=True,
               boxprops={"facecolor": "#dbeafe", "edgecolor": AZUL},
               medianprops={"color": "#1e3a8a", "linewidth": 2})
    ax.set_title("Faturamento por segmento")
    ax.xaxis.set_major_formatter(milhoes)
    ax.grid(axis="y", visible=False)
    return fig


def barras_segmento(indicadores):
    freq = indicadores["frequencias"]["segmento"][::-1]
    fig, ax = plt.subplots()
    barras = ax.barh([f["categoria"] for f in freq], [f["quantidade"] for f in freq], color=AZUL)
    ax.bar_label(barras, labels=[f"{f['quantidade']} ({f['percentual']:.0f}%)" for f in freq], padding=4)
    ax.set_title("Clientes por segmento")
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    ax.grid(axis="y", visible=False)
    return fig


def barras_nivel(indicadores):
    freq = indicadores["frequencias"]["nivel"]
    participacao = {p["nivel"]: p["percentual"] for p in indicadores["faturamento"]["participacao_por_nivel"]}
    fig, ax = plt.subplots()
    barras = ax.bar([f"Nível {f['categoria']}" for f in freq], [f["quantidade"] for f in freq],
                    color=[CORES_NIVEL[f["categoria"]] for f in freq], edgecolor="#1e3a8a")
    ax.bar_label(barras, labels=[
        f"{f['quantidade']} clientes\n{participacao[f['categoria']]:.0f}% do faturamento" for f in freq
    ], padding=4)
    ax.set_title("Distribuição de clientes por nível A/B/C")
    ax.set_ylim(0, max(f["quantidade"] for f in freq) * 1.3)
    ax.yaxis.set_major_locator(MaxNLocator(integer=True))
    ax.grid(axis="x", visible=False)
    return fig


def _salvar(figura, arquivo):
    figura.tight_layout()
    figura.savefig(arquivo)
    plt.close(figura)
    return arquivo
