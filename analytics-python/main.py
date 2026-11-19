"""Entrada do trabalho do Python: processar_lote (chamado pela API em api.py).

O Spring envia as linhas válidas; aqui o lote é criado, as linhas são limpas (limpeza.py),
calculadas (analise.py) e gravadas no banco (banco.py).
"""
from __future__ import annotations                                                  # recurso do Python para tipos mais flexíveis

import logging                                                                      # logging: escreve mensagens no terminal
import time                                                                         # time: mede quanto o processamento levou (telemetria)

import pandas as pd                                                                 # pandas: tabelas de dados

from analise import calcular                                                        # calcular: gera os indicadores e as frases (analise.py)
from limpeza import tratar                                                          # tratar: limpa e padroniza os dados (limpeza.py)

log = logging.getLogger("analytics")                                                # logger do programa


def processar_lote(engine, arquivo_nome: str, linhas: list[dict], recebidas: int,  # processar_lote: cria o lote, trata, calcula e GRAVA no banco
                   invalidas: int = 0, com_aviso: int = 0) -> int:
    """Devolve o id do lote. Se der erro, o lote fica com ERRO no banco e o erro sobe para a API."""
    import banco                                                                    # banco.py: único arquivo que fala com o banco
    inicio = time.perf_counter()                                                    # início, para a duração na telemetria
    ms = lambda: int((time.perf_counter() - inicio) * 1000)                         # ms: milissegundos desde o início
    lote_id = banco.criar_lote(engine, arquivo_nome, recebidas, len(linhas), invalidas, com_aviso)  # 1. cria o lote (PROCESSANDO)
    banco.registrar_telemetria(engine, "UPLOAD_PLANILHA", True, len(linhas), ms(),  # Plano PI: registra o envio da planilha
                               f"{arquivo_nome}: lote {lote_id} criado com {len(linhas)} linha(s) válida(s)")
    try:                                                                            # tenta processar
        if not linhas:                                                              # o backend não mandou nenhuma linha?
            raise ValueError("o backend não enviou linhas válidas")                 # erro explicando
        bruto = pd.DataFrame(linhas).astype(object)                                 # lista de dicionários vira tabela do pandas
        res = tratar(bruto)                                                         # 2. limpa os dados
        payload = calcular(res, recebidas)                                          # 3. calcula média, mediana, agrupamentos e frases
        banco.gravar_resultado(engine, lote_id, res.tratado, payload, len(res.rejeitadas))  # 4. GRAVA clientes, contratos e o JSON; lote PROCESSADO
        log.info("Lote %s processado.", lote_id)                                    # registra no terminal
        banco.registrar_telemetria(engine, "EXECUCAO_SCRIPT", True, len(res.tratado), ms(),  # Plano PI: execução que deu certo
                                   f"Lote {lote_id} processado pelo Python")
        return lote_id                                                              # deu certo: devolve o id
    except Exception as e:  # noqa: BLE001 - qualquer falha deve ficar registrada no lote
        log.exception("Falha no lote %s", lote_id)                                  # escreve o erro completo no terminal
        banco.marcar_erro(engine, lote_id, f"Falha no ETL: {e}")                    # grava ERRO no lote para a tela mostrar
        banco.registrar_telemetria(engine, "EXECUCAO_SCRIPT", False, None, ms(),    # Plano PI: execução que falhou
                                   f"Lote {lote_id}: Falha no ETL: {e}")
        raise                                                                       # repassa o erro para a API responder 500
