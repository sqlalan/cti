"""Testes estatísticos do lote (Shapiro-Wilk e qui-quadrado), com a interpretação em texto.

- Shapiro-Wilk: o faturamento segue uma distribuição normal? Decide se média ou mediana descreve melhor.
- Qui-quadrado: segmento e nível do cliente (A/B/C) estão associados?

Recebe a tabela já tratada por limpeza.tratar(); não grava nada.
"""
from __future__ import annotations

import pandas as pd
from scipy import stats as sp

ALFA = 0.05  # nível de significância: p abaixo disto rejeita a hipótese nula


def _virg(x, casas: int = 3) -> str:
    return f"{x:.{casas}f}".replace(".", ",")


def normalidade(df: pd.DataFrame) -> dict:
    """Shapiro-Wilk no faturamento anual. H0: os dados vêm de uma distribuição normal."""
    fat = df["faturamento_anual"].astype(float)
    if len(fat) < 3 or fat.nunique() < 2:
        return {"teste": "Shapiro-Wilk", "aplicado": False,
                "interpretacao": "Poucos clientes para testar a normalidade (mínimo de 3 valores diferentes)."}
    w, p = sp.shapiro(fat)
    normal = p >= ALFA
    conclusao = (
        f"O faturamento pode ser considerado normal (Shapiro-Wilk W = {_virg(w)}, p = {_virg(p, 4)}): "
        "média e desvio padrão descrevem bem a carteira."
        if normal else
        f"O faturamento não segue uma distribuição normal (Shapiro-Wilk W = {_virg(w)}, p = {_virg(p, 4)} < 0,05): "
        "a mediana descreve melhor o cliente típico do que a média."
    )
    return {"teste": "Shapiro-Wilk", "aplicado": True, "variavel": "faturamento_anual", "n": int(len(fat)),
            "estatistica": round(float(w), 4), "p": round(float(p), 6), "normal": bool(normal),
            "interpretacao": conclusao}


def associacao(df: pd.DataFrame) -> dict:
    """Qui-quadrado de independência entre segmento e nível. H0: as duas variáveis são independentes."""
    tabela = pd.crosstab(df["segmento"], df["nivel_cliente"])
    if tabela.shape[0] < 2 or tabela.shape[1] < 2:
        return {"teste": "Qui-quadrado", "aplicado": False,
                "interpretacao": "É preciso ao menos 2 segmentos e 2 níveis para testar a associação."}
    chi2, p, gl, esperado = sp.chi2_contingency(tabela)
    associadas = p < ALFA
    # Com frequência esperada abaixo de 5 em muitas células, o teste perde precisão.
    pouco_dado = (esperado < 5).mean() > 0.2
    conclusao = (
        f"Segmento e nível do cliente estão associados (qui-quadrado = {_virg(chi2, 2)}, gl = {gl}, "
        f"p = {_virg(p, 4)} < 0,05): a classificação A/B/C muda conforme o segmento."
        if associadas else
        f"Não há evidência de associação entre segmento e nível do cliente (qui-quadrado = {_virg(chi2, 2)}, "
        f"gl = {gl}, p = {_virg(p, 4)}): a proporção de clientes A, B e C é parecida entre os segmentos."
    )
    if pouco_dado:
        conclusao += " Atenção: há poucas observações por combinação, então o resultado é só indicativo."
    return {"teste": "Qui-quadrado", "aplicado": True, "variaveis": ["segmento", "nivel_cliente"],
            "estatistica": round(float(chi2), 4), "p": round(float(p), 6), "grausLiberdade": int(gl),
            "associadas": bool(associadas), "poucasObservacoes": bool(pouco_dado),
            "tabela": {str(seg): {str(niv): int(v) for niv, v in linha.items()} for seg, linha in tabela.iterrows()},
            "interpretacao": conclusao}


def calcular(df: pd.DataFrame) -> dict:
    """Os dois testes e as frases prontas para o relatório de insights."""
    resultado = {"normalidade": normalidade(df), "associacao": associacao(df)}
    resultado["frases"] = [t["interpretacao"] for t in (resultado["normalidade"], resultado["associacao"]) if t["aplicado"]]
    return resultado
