"""Esquema da planilha de clientes e dicionários de padronização.

Espelha FrontEnd/src/dados/esquemaPlanilha.js: o navegador e o Python precisam
aceitar e recusar as mesmas linhas. Mudou uma regra lá, mude aqui também.
"""

import re
import unicodedata

NIVEIS = ["A", "B", "C"]

UFS = {
    "AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO", "MA", "MG", "MS", "MT",
    "PA", "PB", "PE", "PI", "PR", "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO",
}

# Sinônimos ficam na forma achatada (sem acento, caixa ou pontuação).
SEGMENTOS = {
    "Indústria": ["industria", "ind", "industrial", "fabrica"],
    "Comércio": ["comercio", "com", "comercial", "varejo"],
    "Serviços": ["servicos", "servico", "serv"],
    "Saúde": ["saude", "hospitalar"],
    "Tecnologia": ["tecnologia", "tec", "ti"],
    "Educação": ["educacao", "ensino", "educacional"],
}

SERVICOS = {
    "Internet Dedicada": ["internet dedicada", "internet"],
    "Link Empresarial": ["link empresarial", "link"],
    "MPLS": ["mpls"],
    "Firewall": ["firewall"],
    "Backup": ["backup"],
    "Cloud": ["cloud", "nuvem"],
}

SEPARADOR_SERVICOS = re.compile(r"[;,/]")

# chave, rótulo, apelidos do cabeçalho, obrigatória, tratamento
ESQUEMA = [
    ("codigo", "Código", ["codigo cliente", "codigo", "cod cliente", "cod"], True, "codigo"),
    ("nome", "Cliente", ["nome cliente", "cliente", "nome", "razao social"], True, "nome_proprio"),
    ("consultor", "Consultor", ["consultor", "vendedor", "responsavel"], True, "nome_proprio"),
    ("segmento", "Segmento", ["segmento", "setor", "ramo"], True, "segmento"),
    ("nivel", "Nível", ["nivel cliente", "nivel", "classificacao", "curva"], True, "nivel"),
    ("faturamento", "Faturamento anual", ["faturamento anual", "faturamento", "receita", "valor"], True, "moeda"),
    ("servicos", "Serviços", ["servicos contratados", "servicos", "produtos"], True, "servicos"),
    ("contratacao", "Contratação", ["data contratacao", "contratacao", "data"], True, "data"),
    ("cidade", "Cidade", ["cidade", "municipio"], False, "nome_proprio"),
    ("uf", "UF", ["uf", "estado", "sigla"], False, "uf"),
    ("email", "E-mail", ["email", "e mail", "email cliente", "email contato", "contato"], False, "email"),
]


def limpar(bruto):
    """Texto sem espaços sobrando, incluindo o espaço não separável."""
    if bruto is None or (isinstance(bruto, float) and bruto != bruto):
        return ""
    return re.sub(r"[\s ]+", " ", str(bruto)).strip()


def sem_acento(texto):
    return "".join(c for c in unicodedata.normalize("NFD", texto) if not unicodedata.combining(c))


def achatar(bruto):
    """Só para comparar ("Código Cliente" == "codigo_cliente"), nunca para exibir."""
    texto = sem_acento(limpar(bruto)).lower()
    texto = re.sub(r"[._-]+", " ", texto)
    return re.sub(r"\s+", " ", texto).strip()


PARTICULAS = {"de", "da", "do", "das", "dos", "e", "em", "a", "o"}
SIGLA = re.compile(r"^[A-ZÀ-Ý]+$")


def nome_proprio(bruto):
    """"ANA SOUZA" -> "Ana Souza"; mantém sigla curta só em texto de caixa mista."""
    limpo = limpar(bruto)
    if not limpo:
        return ""
    gritado = limpo == limpo.upper()
    palavras = []
    for indice, palavra in enumerate(limpo.split(" ")):
        if indice > 0 and achatar(palavra) in PARTICULAS:
            palavras.append(achatar(palavra))
        elif not gritado and len(palavra) <= 3 and SIGLA.match(palavra):
            palavras.append(palavra)
        else:
            palavras.append(palavra[:1].upper() + palavra[1:].lower())
    return " ".join(palavras)


def consultar(dicionario, bruto):
    chave = achatar(bruto)
    for canonico, sinonimos in dicionario.items():
        if chave in sinonimos:
            return canonico
    return None
