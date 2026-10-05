"""Ponto de entrada: trata a planilha, calcula os indicadores e gera insights e gráficos.

Uso:
    py main.py <planilha.xlsx|.csv> [--saida output] [--sem-graficos]

Arquivos gerados em --saida:
    clientes_tratados.json  linhas válidas, já padronizadas (para o back-end gravar)
    problemas.json          linhas recusadas e avisos de correção
    indicadores.json        estatísticas, frequências, testes e séries para o dashboard
    insights.json           frases prontas para exibição
    graficos/*.png          linha, dispersão, histograma, boxplot e barras

A última linha do stdout é um JSON de resumo, lido pelo Spring Boot (ProcessBuilder).
Código de saída: 0 sucesso, 2 planilha inválida, 1 falha inesperada.
"""

import argparse
import json
import math
import sys
import time
from datetime import date, datetime
from pathlib import Path

import numpy as np
import pandas as pd

import analise
import graficos
import insights
from limpeza import PlanilhaInvalidaError, carregar


def processar(arquivo, saida, com_graficos=True):
    inicio = time.perf_counter()
    saida = Path(saida)
    saida.mkdir(parents=True, exist_ok=True)

    dados, erros, avisos, ignoradas = carregar(arquivo)
    if dados.empty:
        raise PlanilhaInvalidaError("Nenhuma linha válida para analisar: confira o relatório de problemas")

    indicadores = analise.analisar(dados, erros, avisos)
    lista_insights = insights.gerar(indicadores)

    _gravar(saida / "clientes_tratados.json", dados.drop(columns=["qtd_servicos"]).to_dict("records"))
    _gravar(saida / "problemas.json", {"erros": erros, "avisos": avisos, "colunas_ignoradas": ignoradas})
    _gravar(saida / "indicadores.json", indicadores)
    _gravar(saida / "insights.json", lista_insights)
    arquivos_graficos = graficos.gerar(dados, indicadores, saida / "graficos") if com_graficos else []

    return {
        "sucesso": True,
        "arquivo": Path(arquivo).name,
        "linhas_lidas": indicadores["qualidade"]["linhas_lidas"],
        "linhas_validas": len(dados),
        "linhas_rejeitadas": len(erros),
        "avisos": len(avisos),
        "insights": len(lista_insights),
        "graficos": arquivos_graficos,
        "saida": str(saida.resolve()),
        "duracao_ms": round((time.perf_counter() - inicio) * 1000),
    }


def _para_json(valor):
    """numpy, datas e NaN viram tipos que o JSON entende."""
    if isinstance(valor, dict):
        return {str(k): _para_json(v) for k, v in valor.items()}
    if isinstance(valor, (list, tuple, np.ndarray)):
        return [_para_json(v) for v in valor]
    if isinstance(valor, np.generic):  # np.float64, np.int64, np.bool_...
        valor = valor.item()
    if isinstance(valor, float) and not math.isfinite(valor):
        return None
    if isinstance(valor, (pd.Timestamp, datetime, date)):
        return valor.strftime("%Y-%m-%d")
    return valor


def _gravar(arquivo, conteudo):
    # Converte antes de abrir o arquivo: uma falha no meio não deixa JSON pela metade.
    texto = json.dumps(_para_json(conteudo), ensure_ascii=False, indent=2)
    Path(arquivo).write_text(texto, encoding="utf-8")


def main(argv=None):
    # Console do Windows usa cp1252; sem isso um "ç" no resumo derruba o script.
    sys.stdout.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(description="Trata a planilha da CTI e gera indicadores e insights.")
    parser.add_argument("arquivo", help="planilha .xlsx, .xls ou .csv")
    parser.add_argument("--saida", default=str(Path(__file__).parent / "output"), help="pasta dos resultados")
    parser.add_argument("--sem-graficos", action="store_true", help="não gera os PNGs (mais rápido)")
    args = parser.parse_args(argv)

    try:
        resumo = processar(args.arquivo, args.saida, com_graficos=not args.sem_graficos)
        codigo = 0
    except PlanilhaInvalidaError as erro:
        resumo = {"sucesso": False, "tipo": erro.tipo, "mensagem": str(erro),
                  "colunas": getattr(erro, "colunas", [])}
        codigo = 2
    except Exception as erro:  # o Java precisa de uma resposta legível mesmo no pior caso
        resumo = {"sucesso": False, "tipo": "ERRO_INTERNO", "mensagem": f"{type(erro).__name__}: {erro}"}
        codigo = 1

    print(json.dumps(resumo, ensure_ascii=False))
    return codigo


if __name__ == "__main__":
    sys.exit(main())
