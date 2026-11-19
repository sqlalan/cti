# ============================================================================
# api.py — a "porta de entrada" do Python para o DEPLOY: transforma o ETL em uma API (FastAPI).
# O Spring chama POST /etl enviando as linhas válidas; o Python cria o lote, limpa, calcula e GRAVA no banco.
# Como ligar:  uvicorn api:app --port 8000        (documentação automática em http://localhost:8000/docs)
# ============================================================================
import logging                                                                      # logging: escreve mensagens no terminal
import os                                                                           # os: lê variáveis de ambiente (ETL_API_KEY)
from typing import Any                                                              # Any: "qualquer tipo" (valores das linhas)

from fastapi import FastAPI, Header, HTTPException                                  # FastAPI: cria a API; Header: lê cabeçalhos; HTTPException: devolve erro HTTP
import pandas as pd                                                                 # pandas: tabelas de dados
from pydantic import BaseModel                                                      # BaseModel: descreve o formato do JSON que o Spring envia
from sqlalchemy import text                                                         # text: escreve SQL dentro do Python (rota /lotes)

import config                                                                       # config.py: carrega o .env (senha do banco, ETL_API_KEY)
from analise import calcular                                                        # calcular: indicadores e frases (analise.py)
from limpeza import tratar                                                          # tratar: limpeza (limpeza.py)
from main import processar_lote                                                     # a MESMA função que o modo de estudo usa: trata, calcula e grava no Azure

log = logging.getLogger("analytics")                                                # logger desta API
app = FastAPI(title="CTI Insights — ETL em Python")                                 # app: a aplicação web
_engine = None                                                                      # conexão com o Azure (criada na primeira vez que for usada)


class Pedido(BaseModel):                                                            # Pedido: o "molde" do JSON (nomes iguais aos do pedido.put do Java)
    arquivoNome: str = "planilha"                                                   # nome da planilha; se não vier, vale "planilha"
    recebidas: int                                                                  # linhas lidas na planilha (obrigatório)
    invalidas: int = 0                                                              # linhas com erro, que ficaram no Java
    comAviso: int = 0                                                               # linhas boas com algum aviso
    linhas: list[dict[str, Any]]                                                    # a lista com as linhas válidas


def _engine_azure():                                                                # devolve a conexão com o Azure (cria só uma vez)
    global _engine                                                                  # usa a variável de fora da função
    if _engine is None:                                                             # ainda não existe?
        import banco                                                                # banco.py: único arquivo que fala com o Azure
        _engine = banco.criar_engine()                                              # abre a conexão
    return _engine                                                                  # devolve


@app.get("/saude")                                                                  # GET /saude: o Spring (ou você) confere se a API está de pé
def saude() -> dict:                                                                # função chamada nesse endereço
    return {"status": "ok"}                                                         # resposta simples


@app.post("/etl")                                                                   # POST /etl: recebe as linhas, cria o lote, processa e grava no banco
def etl(pedido: Pedido, x_etl_key: str | None = Header(default=None)) -> dict:      # x_etl_key: cabeçalho X-ETL-Key
    chave = os.getenv("ETL_API_KEY", "").strip()                                    # chave combinada com o Spring (vazia = sem chave, só para estudo)
    if chave and x_etl_key != chave:                                                # há chave configurada e a enviada não confere?
        raise HTTPException(status_code=401, detail="Chave do ETL inválida.")       # recusa: 401 não autorizado
    if not pedido.linhas:                                                           # veio vazio?
        raise HTTPException(status_code=422, detail="Nenhuma linha válida foi enviada.")  # 422: dados inválidos
    try:                                                                            # tente fazer isto...
        lote_id = processar_lote(_engine_azure(), pedido.arquivoNome, pedido.linhas,  # main.py: cria o lote, limpa, calcula e grava
                                 pedido.recebidas, pedido.invalidas, pedido.comAviso)
    except Exception as e:  # noqa: BLE001 - ...se der qualquer erro, guarde em e
        raise HTTPException(status_code=500, detail=f"Falha no ETL: {e}")           # 500 = erro no servidor, com o motivo
    return {"ok": True, "loteId": lote_id}                                          # deu tudo certo (loteId: para a tela acompanhar o lote)


@app.post("/testar")                                                                # POST /testar: teste sem banco (não grava nada no Azure)
def testar(linhas: list[dict[str, Any]]) -> dict:                                   # recebe só a lista de clientes (como amostras/clientes.json)
    res = tratar(pd.DataFrame(linhas).astype(object))                               # limpa
    return calcular(res, len(linhas))                                               # calcula e devolve os números e as frases


@app.get("/lotes")                                                                  # desafio da aula 12: GET /lotes lista os lotes gravados no banco
def listar_lotes():                                                                 # função que será executada
    with _engine_azure().connect() as conn:                                         # conecta no banco do .env (Neon ou Azure)
        sql = text("SELECT id, arquivo_nome, status FROM upload_lote")              # a consulta: quais lotes existem
        linhas = conn.execute(sql).mappings().all()                                 # executa e pega todas as linhas
    return [dict(l) for l in linhas]                                                # devolve a lista como JSON
