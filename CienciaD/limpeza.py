"""Leitura, limpeza, padronização e validação da planilha de clientes.

Linha com erro sai dos dados tratados e vai para a lista de erros com o número
da linha no Excel. Aviso (correção aplicada, valor fora do dicionário) não
derruba a linha.
"""

import math
import re
from datetime import date, datetime
from pathlib import Path

import numpy as np
import pandas as pd

from esquema import (
    ESQUEMA, NIVEIS, SEGMENTOS, SEPARADOR_SERVICOS, SERVICOS, UFS,
    achatar, consultar, limpar, nome_proprio, sem_acento,
)

ABA_PRINCIPAL = "upload_clientes"
EMAIL = re.compile(r"^[^\s@]+@[^\s@.]+(\.[^\s@.]+)*\.[a-z]{2,}$")
DATA_BR = re.compile(r"^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$")


class PlanilhaInvalidaError(Exception):
    """O arquivo não pode ser tratado: ilegível, vazio ou em formato não suportado."""

    tipo = "EXCEL_INVALIDO"


class ColunaObrigatoriaError(PlanilhaInvalidaError):
    """O cabeçalho não tem uma ou mais colunas obrigatórias."""

    tipo = "COLUNA_OBRIGATORIA"

    def __init__(self, colunas):
        super().__init__(f"Cabeçalho sem a(s) coluna(s) obrigatória(s): {', '.join(colunas)}")
        self.colunas = colunas


# ---------------------------------------------------------------------------
# Leitura
# ---------------------------------------------------------------------------

def ler_matriz(caminho):
    """Devolve a planilha como lista de linhas cruas; a primeira é o cabeçalho."""
    caminho = Path(caminho)
    extensao = caminho.suffix.lower()
    try:
        if extensao in (".xlsx", ".xls"):
            # `with` fecha o arquivo: no Windows, aberto ele impede o Java de apagar o upload.
            with pd.ExcelFile(caminho) as livro:
                aba = ABA_PRINCIPAL if ABA_PRINCIPAL in livro.sheet_names else livro.sheet_names[0]
                tabela = livro.parse(aba, header=None, dtype=object)
        elif extensao == ".csv":
            tabela = _ler_csv(caminho)
        else:
            raise PlanilhaInvalidaError(f"Formato {extensao or '(sem extensão)'} não suportado; use .xlsx, .xls ou .csv")
    except PlanilhaInvalidaError:
        raise
    except Exception as erro:  # arquivo corrompido, protegido por senha etc.
        raise PlanilhaInvalidaError(f"Não foi possível ler o arquivo: {erro}") from erro

    return [[_celula(v) for v in linha] for linha in tabela.itertuples(index=False)]


def _ler_csv(caminho):
    # Excel no Windows exporta CSV em windows-1252, não em UTF-8.
    for codificacao in ("utf-8-sig", "cp1252"):
        try:
            return pd.read_csv(caminho, header=None, dtype=str, keep_default_na=False,
                               sep=None, engine="python", encoding=codificacao)
        except UnicodeDecodeError:
            continue
    raise PlanilhaInvalidaError("Codificação do CSV não reconhecida")


def _celula(valor):
    if valor is None or (isinstance(valor, float) and math.isnan(valor)):
        return None
    if isinstance(valor, pd.Timestamp):
        return valor.to_pydatetime()
    if isinstance(valor, np.generic):  # np.int64 não passa em isinstance(x, int)
        return valor.item()
    return valor


# ---------------------------------------------------------------------------
# Tratamento por coluna: devolve (valor, erro, aviso); erro e aviso = (tipo, mensagem)
# ---------------------------------------------------------------------------

def _texto(bruto):
    return limpar(bruto), None, None


def _codigo(bruto):
    return limpar(bruto).upper(), None, None


def _nome_proprio(bruto):
    return nome_proprio(bruto), None, None


def _email(bruto):
    valor = limpar(bruto).lower()
    if valor and not EMAIL.match(valor):
        return valor, ("email", f'e-mail "{valor}" não é válido'), None
    return valor, None, None


def _segmento(bruto):
    limpo = limpar(bruto)
    if not limpo:
        return "", None, None
    canonico = consultar(SEGMENTOS, limpo)
    if canonico:
        return canonico, None, None
    # Pode ser segmento novo: passa, mas o relatório cobra o cadastro.
    return nome_proprio(limpo), None, ("dicionario", f'segmento "{limpo}" não está no dicionário')


def _nivel(bruto):
    valor = limpar(bruto).upper()
    if valor and valor not in NIVEIS:
        return valor, ("formato", f'nível "{valor}" fora de A, B ou C'), None
    return valor, None, None


def _moeda(bruto):
    """Aceita 1850000, 1850000.0 e "R$ 1.850.000,00"."""
    if isinstance(bruto, bool):
        return None, ("formato", "faturamento inválido"), None
    if isinstance(bruto, (int, float)):
        if not math.isfinite(bruto):
            return None, ("formato", "faturamento inválido"), None
        if bruto < 0:
            return float(bruto), ("formato", "faturamento negativo"), None
        return float(bruto), None, None

    texto = limpar(bruto)
    if not texto:
        return None, None, None
    sem_moeda = re.sub(r"[\s ]", "", re.sub(r"R\$", "", texto, flags=re.I))
    brasileiro = re.match(r"^-?\d{1,3}(\.\d{3})*(,\d+)?$", sem_moeda) or re.match(r"^-?\d+,\d+$", sem_moeda)
    normalizado = sem_moeda.replace(".", "").replace(",", ".") if brasileiro else sem_moeda
    try:
        numero = float(normalizado)
    except ValueError:
        return None, ("formato", f'faturamento "{texto}" não é numérico'), None
    if not math.isfinite(numero):
        return None, ("formato", f'faturamento "{texto}" não é numérico'), None
    if numero < 0:
        return numero, ("formato", "faturamento negativo"), None
    return numero, None, None


def _servicos(bruto):
    texto = limpar(bruto)
    itens, desconhecidos = [], []
    for pedaco in SEPARADOR_SERVICOS.split(texto) if texto else []:
        limpo = limpar(pedaco)
        if not limpo:
            continue
        canonico = consultar(SERVICOS, limpo)
        if not canonico:
            canonico = nome_proprio(limpo)
            desconhecidos.append(limpo)
        if canonico not in itens:
            itens.append(canonico)
    if desconhecidos:
        return itens, None, ("dicionario", f"serviço não catalogado: {', '.join(desconhecidos)}")
    return itens, None, None


def _data(bruto):
    """dd/mm/aaaa à mão: o parser genérico lê 03/02 como 2 de março sem avisar."""
    if isinstance(bruto, datetime):
        return bruto.date(), None, None
    if isinstance(bruto, date):
        return bruto, None, None
    texto = limpar(bruto)
    if not texto:
        return None, None, None
    partes = DATA_BR.match(texto)
    if not partes:
        return None, ("formato", f'data "{texto}" fora do formato dd/mm/aaaa'), None
    dia, mes, ano = map(int, partes.groups())
    try:
        return date(ano, mes, dia), None, None
    except ValueError:
        return None, ("formato", f'data "{texto}" não existe no calendário'), None


def _uf(bruto):
    valor = sem_acento(limpar(bruto)).upper()
    if valor and valor not in UFS:
        return valor, ("formato", f'UF "{valor}" não é uma sigla válida'), None
    return valor, None, None


TRATAMENTOS = {
    "texto": _texto, "codigo": _codigo, "nome_proprio": _nome_proprio, "email": _email,
    "segmento": _segmento, "nivel": _nivel, "moeda": _moeda, "servicos": _servicos,
    "data": _data, "uf": _uf,
}


# ---------------------------------------------------------------------------
# Cabeçalho e linhas
# ---------------------------------------------------------------------------

def casar_cabecalho(cabecalho):
    """Mapeia cada coluna do esquema para a posição dela no arquivo."""
    achatado = [achatar(nome) for nome in cabecalho]
    mapa, usadas = {}, set()
    for chave, _, apelidos, _, _ in ESQUEMA:
        for indice, nome in enumerate(achatado):
            if indice not in usadas and nome in apelidos:
                mapa[chave] = indice
                usadas.add(indice)
                break
    faltando = [rotulo for chave, rotulo, _, obrigatoria, _ in ESQUEMA if obrigatoria and chave not in mapa]
    ignoradas = [limpar(nome) for i, nome in enumerate(cabecalho) if limpar(nome) and i not in usadas]
    return mapa, faltando, ignoradas


def _correcoes(bruto, valor, rotulo):
    """O que o tratamento corrigiu em silêncio, para a planilha ser corrigida na origem."""
    if not isinstance(bruto, str):
        return []
    achados = []
    aparado = limpar(bruto)
    if aparado != bruto:
        achados.append(("espacos", f'{rotulo}: espaço sobrando removido de "{bruto}"'))
    if isinstance(valor, str) and aparado and valor != aparado:
        achados.append(("padronizacao", f'{rotulo}: "{aparado}" padronizado para "{valor}"'))
    return achados


def tratar(matriz):
    """Devolve (dados, erros, avisos, colunas_ignoradas). `dados` é um DataFrame."""
    if not matriz:
        raise PlanilhaInvalidaError("A planilha está vazia")

    cabecalho, *corpo = matriz
    mapa, faltando, ignoradas = casar_cabecalho(cabecalho)
    if faltando:
        raise ColunaObrigatoriaError(faltando)

    registros, erros, avisos = [], [], []
    vistos = {}

    for indice, celulas in enumerate(corpo):
        linha = indice + 2  # cabeçalho + Excel conta a partir de 1
        if all(limpar(c) == "" for c in celulas):
            continue

        registro, problemas = {}, []
        for chave, rotulo, _, obrigatoria, tratamento in ESQUEMA:
            bruto = celulas[mapa[chave]] if chave in mapa else None
            valor, erro, aviso = TRATAMENTOS[tratamento](bruto)
            registro[chave] = valor

            if erro:
                problemas.append({"tipo": erro[0], "coluna": rotulo, "mensagem": _maiuscula(erro[1])})
            if aviso:
                avisos.append({"linha": linha, "coluna": rotulo, "tipo": aviso[0], "motivo": _maiuscula(aviso[1])})
            if not erro:
                for tipo, mensagem in _correcoes(bruto, valor, rotulo):
                    avisos.append({"linha": linha, "coluna": rotulo, "tipo": tipo, "motivo": mensagem})

            vazio = valor in ("", None) or (isinstance(valor, list) and not valor)
            if not erro and obrigatoria and vazio:
                problemas.append({"tipo": "vazio", "coluna": rotulo, "mensagem": f"{rotulo} em branco"})

        codigo = registro["codigo"]
        if codigo:
            if codigo in vistos:
                problemas.append({
                    "tipo": "duplicado", "coluna": "Código",
                    "mensagem": f"Código {codigo} repetido (já apareceu na linha {vistos[codigo]})",
                })
            else:
                vistos[codigo] = linha

        if problemas:
            erros.append({
                "linha": linha,
                "codigo": codigo or "—",
                "nome": registro["nome"] or "(sem nome)",
                "problemas": problemas,
            })
            continue

        registro["linha"] = linha
        registros.append(registro)

    return _como_tabela(registros), erros, avisos, ignoradas


def _maiuscula(texto):
    return texto[:1].upper() + texto[1:]


def _como_tabela(registros):
    colunas = [chave for chave, *_ in ESQUEMA] + ["linha"]
    dados = pd.DataFrame(registros, columns=colunas)
    dados["faturamento"] = dados["faturamento"].astype(float)
    dados["contratacao"] = pd.to_datetime(dados["contratacao"])
    dados["qtd_servicos"] = dados["servicos"].map(len).astype(int)
    return dados


def carregar(caminho):
    """Atalho: lê o arquivo e devolve o resultado de `tratar`."""
    return tratar(ler_matriz(caminho))
