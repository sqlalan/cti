"""Estatística descritiva, testes de hipótese e série temporal sobre os dados tratados.

Escalas (aba ciencia_de_dados da planilha modelo): segmento e consultor são
nominais, nível A/B/C é ordinal, faturamento é de razão, data é temporal.
"""

from collections import Counter

import numpy as np
import pandas as pd
from scipy import stats

from esquema import NIVEIS

ALFA = 0.05

ROTULOS = {"segmento": "segmento", "nivel": "nível", "consultor": "consultor"}


# ---------------------------------------------------------------------------
# Descritiva
# ---------------------------------------------------------------------------

def frequencia(serie, ordem=None):
    """Contagem e percentual de cada categoria, da maior para a menor (ou na `ordem`)."""
    contagem = serie.value_counts()
    if ordem:
        contagem = contagem.reindex(ordem, fill_value=0)
    total = int(contagem.sum())
    return [
        {"categoria": str(categoria), "quantidade": int(qtd), "percentual": _pct(qtd, total)}
        for categoria, qtd in contagem.items()
    ]


def medidas(valores):
    """Tendência central e dispersão de uma série numérica."""
    valores = pd.Series(valores, dtype=float).dropna()
    n = len(valores)
    if n == 0:
        return {"n": 0}
    q1, q3 = valores.quantile([0.25, 0.75])
    media = valores.mean()
    desvio = valores.std(ddof=1) if n > 1 else None  # amostral
    return {
        "n": n,
        "soma": valores.sum(),
        "media": media,
        "mediana": valores.median(),
        "minimo": valores.min(),
        "maximo": valores.max(),
        "amplitude": valores.max() - valores.min(),
        "variancia": valores.var(ddof=1) if n > 1 else None,
        "desvio_padrao": desvio,
        "coeficiente_variacao": desvio / media * 100 if desvio is not None and media else None,
        "q1": q1,
        "q3": q3,
        "iqr": q3 - q1,
        "p90": valores.quantile(0.9),
    }


def medidas_por_grupo(dados, grupo, ordem=None):
    grupos = ordem or sorted(dados[grupo].unique())
    return [
        {grupo: chave, **medidas(dados.loc[dados[grupo] == chave, "faturamento"])}
        for chave in grupos
        if (dados[grupo] == chave).any()
    ]


def outliers(dados):
    """Clientes fora de Q1 - 1,5·IQR e Q3 + 1,5·IQR (critério do boxplot)."""
    q1, q3 = dados["faturamento"].quantile([0.25, 0.75])
    limite = q3 + 1.5 * (q3 - q1)
    piso = q1 - 1.5 * (q3 - q1)
    fora = dados[(dados["faturamento"] > limite) | (dados["faturamento"] < piso)]
    return {
        "limite_inferior": piso,
        "limite_superior": limite,
        "clientes": [
            {"codigo": r.codigo, "nome": r.nome, "segmento": r.segmento, "faturamento": r.faturamento}
            for r in fora.sort_values("faturamento", ascending=False).itertuples()
        ],
    }


def servicos(dados):
    """Serviços mais contratados (célula multivalorada separada em linhas)."""
    explodido = dados[["nivel", "servicos"]].explode("servicos")
    geral = frequencia(explodido["servicos"])
    por_nivel = {
        nivel: frequencia(explodido.loc[explodido["nivel"] == nivel, "servicos"])
        for nivel in NIVEIS
        if (explodido["nivel"] == nivel).any()
    }
    return {
        "total_contratos": len(explodido),
        "media_por_cliente": dados["qtd_servicos"].mean(),
        "frequencia": geral,
        "por_nivel": por_nivel,
    }


def cruzamento(dados, linhas, colunas, ordem_colunas=None):
    """Tabela de contingência em formato de lista, pronta para JSON."""
    tabela = pd.crosstab(dados[linhas], dados[colunas])
    if ordem_colunas:
        tabela = tabela.reindex(columns=ordem_colunas, fill_value=0)
    return [{linhas: indice, **{str(c): int(v) for c, v in linha.items()}} for indice, linha in tabela.iterrows()]


def contratacoes_por_mes(dados):
    """Série mensal de novos clientes e de contratos, com os meses vazios em zero."""
    mensal = dados.set_index("contratacao").resample("MS").agg({"codigo": "count", "qtd_servicos": "sum"})
    acumulado = mensal["codigo"].cumsum()
    return [
        {
            "mes": mes.strftime("%Y-%m"),
            "clientes": int(linha.codigo),
            "contratos": int(linha.qtd_servicos),
            "clientes_acumulados": int(acumulado[mes]),
        }
        for mes, linha in mensal.iterrows()
    ]


def contratacoes_por_ano(dados):
    # A média mensal divide pelos meses que a planilha cobre naquele ano, não por 12:
    # o ano corrente está incompleto, e um mês sem venda continua contando.
    periodo = dados["contratacao"].dt.to_period("M")
    primeiro, ultimo = periodo.min(), periodo.max()
    resultado = []
    for ano, grupo in dados.groupby(dados["contratacao"].dt.year):
        inicio = max(pd.Period(f"{ano}-01", "M"), primeiro)
        fim = min(pd.Period(f"{ano}-12", "M"), ultimo)
        meses_cobertos = (fim - inicio).n + 1
        resultado.append({
            "ano": int(ano),
            "clientes": len(grupo),
            "contratos": int(grupo["qtd_servicos"].sum()),
            "meses_cobertos": meses_cobertos,
            "media_clientes_mes": len(grupo) / meses_cobertos,
            "faturamento_novos": grupo["faturamento"].sum(),
        })
    return resultado


# ---------------------------------------------------------------------------
# Testes de hipótese (α = 5%)
# ---------------------------------------------------------------------------

def teste_normalidade(valores, nome="faturamento"):
    """Shapiro-Wilk. H0: os dados vêm de uma distribuição normal."""
    valores = pd.Series(valores, dtype=float).dropna()
    if len(valores) < 3:
        return {"teste": "Shapiro-Wilk", "aplicavel": False, "motivo": "menos de 3 observações"}
    w, p = stats.shapiro(valores)
    normal = p >= ALFA
    return {
        "teste": "Shapiro-Wilk",
        "variavel": nome,
        "aplicavel": True,
        "n": len(valores),
        "estatistica": w,
        "p_valor": p,
        "rejeita_h0": not normal,
        "conclusao": (
            f"O {nome} segue distribuição normal ({formatar_p(p)} ≥ 0,05); média e desvio padrão o descrevem bem."
            if normal else
            f"O {nome} não segue distribuição normal ({formatar_p(p)} < 0,05); a mediana representa "
            "melhor o cliente típico do que a média, e testes não paramétricos são os indicados."
        ),
    }


def teste_associacao(dados, linhas="segmento", colunas="nivel"):
    """Qui-quadrado de independência. H0: segmento e nível não estão associados."""
    tabela = pd.crosstab(dados[linhas], dados[colunas])
    if tabela.shape[0] < 2 or tabela.shape[1] < 2:
        return {"teste": "Qui-quadrado", "aplicavel": False, "motivo": "a tabela precisa de pelo menos 2×2"}
    qui2, p, gl, esperados = stats.chi2_contingency(tabela)
    n = int(tabela.values.sum())
    cramer = np.sqrt(qui2 / (n * (min(tabela.shape) - 1)))
    baixas = float((esperados < 5).mean() * 100)
    # Regra de Cochran: com mais de 20% das células esperadas abaixo de 5 a aproximação fica frágil.
    confiavel = baixas <= 20
    associado = p < ALFA
    a, b = ROTULOS.get(linhas, linhas), ROTULOS.get(colunas, colunas)
    conclusao = (
        f"Há associação entre {a} e {b} ({formatar_p(p)} < 0,05; V de Cramér = {_br(cramer)})."
        if associado else
        f"Não há evidência de associação entre {a} e {b} ({formatar_p(p)} ≥ 0,05; "
        f"V de Cramér = {_br(cramer)})."
    )
    if not confiavel:
        conclusao += (
            f" Atenção: {baixas:.0f}% das células têm frequência esperada abaixo de 5, então o "
            "resultado é indicativo; com mais clientes na base o teste ganha força."
        )
    return {
        "teste": "Qui-quadrado de independência",
        "variaveis": [linhas, colunas],
        "aplicavel": True,
        "n": n,
        "estatistica": qui2,
        "graus_liberdade": int(gl),
        "p_valor": p,
        "v_cramer": cramer,
        "celulas_esperadas_abaixo_5_pct": baixas,
        "pressuposto_atendido": confiavel,
        "rejeita_h0": associado,
        "conclusao": conclusao,
    }


def teste_correlacao(dados, x="faturamento", y="qtd_servicos"):
    """Spearman (por postos): não exige normalidade e serve para contagens pequenas."""
    rho, p = stats.spearmanr(dados[x], dados[y])
    r, p_pearson = stats.pearsonr(dados[x], dados[y])
    forca = _forca(rho)
    sentido = "positiva" if rho > 0 else "negativa"
    significativa = p < ALFA
    return {
        "teste": "Correlação de Spearman",
        "variaveis": [x, y],
        "n": len(dados),
        "rho": rho,
        "p_valor": p,
        "pearson_r": r,
        "pearson_p_valor": p_pearson,
        "forca": forca,
        "rejeita_h0": significativa,
        "conclusao": (
            f"Correlação {sentido} {forca} e significativa (ρ = {_br(rho)}, {formatar_p(p)}): "
            "clientes que faturam mais tendem a contratar mais serviços."
            if significativa and rho > 0 else
            f"Correlação {sentido} {forca} (ρ = {_br(rho)}, {formatar_p(p)})"
            + ("." if significativa else ", sem significância estatística.")
        ),
    }


def teste_grupos(dados, grupo="nivel", ordem=NIVEIS):
    """Kruskal-Wallis. H0: o faturamento tem a mesma distribuição em todos os níveis."""
    amostras = [dados.loc[dados[grupo] == g, "faturamento"] for g in ordem if (dados[grupo] == g).any()]
    if len(amostras) < 2:
        return {"teste": "Kruskal-Wallis", "aplicavel": False, "motivo": "menos de 2 grupos"}
    h, p = stats.kruskal(*amostras)
    difere = p < ALFA
    return {
        "teste": "Kruskal-Wallis",
        "variaveis": ["faturamento", grupo],
        "aplicavel": True,
        "estatistica": h,
        "p_valor": p,
        "rejeita_h0": difere,
        "conclusao": (
            f"O faturamento difere entre os níveis ({formatar_p(p)} < 0,05): a classificação A/B/C "
            "acompanha o porte financeiro do cliente."
            if difere else
            f"Não há diferença significativa de faturamento entre os níveis ({formatar_p(p)}): "
            "a classificação A/B/C não está refletindo o porte do cliente."
        ),
    }


# ---------------------------------------------------------------------------
# Tudo junto
# ---------------------------------------------------------------------------

def analisar(dados, erros=(), avisos=()):
    """Monta o dicionário de indicadores que vai para indicadores.json."""
    lidas = len(dados) + len(erros)
    fat_por_nivel = dados.groupby("nivel")["faturamento"].sum().reindex(NIVEIS, fill_value=0)
    total_fat = dados["faturamento"].sum()
    return {
        "qualidade": {
            "linhas_lidas": lidas,
            "linhas_validas": len(dados),
            "linhas_rejeitadas": len(erros),
            "percentual_aproveitado": _pct(len(dados), lidas),
            "avisos": len(avisos),
            "erros_por_tipo": _contar_tipos(p["tipo"] for e in erros for p in e["problemas"]),
            "avisos_por_tipo": _contar_tipos(a["tipo"] for a in avisos),
        },
        "frequencias": {
            "segmento": frequencia(dados["segmento"]),
            "nivel": frequencia(dados["nivel"], ordem=NIVEIS),
            "consultor": frequencia(dados["consultor"]),
            "cidade": frequencia(dados["cidade"].replace("", "Não informada")),
            "uf": frequencia(dados["uf"].replace("", "Não informada")),
        },
        "moda": {
            "segmento": _moda(dados["segmento"]),
            "nivel": _moda(dados["nivel"]),
            "servico": _moda(dados["servicos"].explode()),
        },
        "faturamento": {
            "geral": medidas(dados["faturamento"]),
            "por_segmento": medidas_por_grupo(dados, "segmento"),
            "por_nivel": medidas_por_grupo(dados, "nivel", ordem=NIVEIS),
            "por_consultor": medidas_por_grupo(dados, "consultor"),
            "participacao_por_nivel": [
                {"nivel": n, "faturamento": v, "percentual": _pct(v, total_fat)} for n, v in fat_por_nivel.items()
            ],
            "outliers": outliers(dados),
        },
        "servicos": servicos(dados),
        "cruzamentos": {
            "segmento_x_nivel": cruzamento(dados, "segmento", "nivel", ordem_colunas=NIVEIS),
            "nivel_x_qtd_servicos": cruzamento(dados, "nivel", "qtd_servicos"),
        },
        "contratacoes": {
            "por_mes": contratacoes_por_mes(dados),
            "por_ano": contratacoes_por_ano(dados),
        },
        "testes": {
            "normalidade_faturamento": teste_normalidade(dados["faturamento"]),
            "normalidade_log_faturamento": teste_normalidade(
                np.log(dados.loc[dados["faturamento"] > 0, "faturamento"]), "log do faturamento"
            ),
            "associacao_segmento_nivel": teste_associacao(dados),
            "correlacao_faturamento_servicos": teste_correlacao(dados),
            "faturamento_entre_niveis": teste_grupos(dados),
        },
    }


def _br(valor, casas=2):
    return f"{valor:.{casas}f}".replace(".", ",")


def formatar_p(p):
    """p-valor como se escreve em relatório: muito pequeno vira "p < 0,0001"."""
    return "p < 0,0001" if p < 0.0001 else f"p = {_br(p, 4 if p < 0.01 else 3)}"


def _pct(parte, total):
    return float(parte) / float(total) * 100 if total else 0.0


def _moda(serie):
    modas = serie.mode()
    return sorted(str(m) for m in modas) if len(modas) else []


def _contar_tipos(tipos):
    return dict(Counter(tipos).most_common())


def _forca(coeficiente):
    a = abs(coeficiente)
    if a >= 0.7:
        return "forte"
    if a >= 0.4:
        return "moderada"
    if a >= 0.2:
        return "fraca"
    return "desprezível"
