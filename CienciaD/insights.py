"""Insights estratégicos em texto simples, gerados a partir dos indicadores.

Cada insight tem `tipo` (para o back-end gravar e o dashboard agrupar), `titulo`
e `texto`. Nada aqui é fixo: as frases mudam conforme a planilha enviada.
"""

from analise import formatar_p

MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"]


def gerar(ind):
    candidatos = [
        _concentracao_segmento(ind),
        _segmento_mais_valioso(ind),
        _concentracao_nivel(ind),
        _servico_mais_contratado(ind),
        _faturamento_e_servicos(ind),
        _clientes_extremos(ind),
        _evolucao(ind),
        _carteira_consultores(ind),
        _distribuicao_faturamento(ind),
        _segmento_x_nivel(ind),
        _qualidade(ind),
    ]
    return [insight for insight in candidatos if insight]


def _concentracao_segmento(ind):
    freq = ind["frequencias"]["segmento"]
    if not freq:
        return None
    topo = freq[0]
    dois = sum(f["percentual"] for f in freq[:2])
    return _insight(
        "SEGMENTO", "Segmento com a maior carteira",
        f"{topo['categoria']} é o segmento com mais clientes: {topo['quantidade']} de "
        f"{ind['qualidade']['linhas_validas']} ({_pct(topo['percentual'])}). Junto com "
        f"{freq[1]['categoria']}, soma {_pct(dois)} da carteira." if len(freq) > 1 else
        f"Todos os clientes estão no segmento {topo['categoria']}.",
    )


def _segmento_mais_valioso(ind):
    segmentos = ind["faturamento"]["por_segmento"]
    if len(segmentos) < 2:
        return None
    maior = max(segmentos, key=lambda s: s["mediana"])
    menor = min(segmentos, key=lambda s: s["mediana"])
    maior_qtd = ind["frequencias"]["segmento"][0]["categoria"]
    texto = (
        f"{maior['segmento']} tem o cliente típico de maior porte (mediana de {_reais(maior['mediana'])} "
        f"por ano), contra {_reais(menor['mediana'])} em {menor['segmento']}."
    )
    if maior["segmento"] != maior_qtd:
        texto += (
            f" {maior_qtd} tem mais clientes, mas {maior['segmento']} concentra mais valor por cliente: "
            "é onde vale priorizar a prospecção de contas grandes."
        )
    return _insight("SEGMENTO", "Segmento de maior valor por cliente", texto)


def _concentracao_nivel(ind):
    freq = {f["categoria"]: f for f in ind["frequencias"]["nivel"]}
    part = {p["nivel"]: p for p in ind["faturamento"]["participacao_por_nivel"]}
    if "A" not in freq or not freq["A"]["quantidade"]:
        return None
    texto = (
        f"Os clientes nível A são {_pct(freq['A']['percentual'])} da carteira e respondem por "
        f"{_pct(part['A']['percentual'])} do faturamento."
    )
    if part["A"]["percentual"] >= 60:
        texto += " A receita depende de poucas contas: perder um cliente A pesa muito, então retenção deles é prioridade."
    if freq.get("C") and freq["C"]["quantidade"]:
        texto += (
            f" Já o nível C reúne {_pct(freq['C']['percentual'])} dos clientes e só "
            f"{_pct(part['C']['percentual'])} do faturamento, um público para ofertas de upgrade."
        )
    return _insight("FATURAMENTO", "Concentração de receita por nível", texto)


def _servico_mais_contratado(ind):
    serv = ind["servicos"]
    if not serv["frequencia"]:
        return None
    topo = serv["frequencia"][0]
    texto = (
        f"{topo['categoria']} é o serviço mais contratado ({topo['quantidade']} de {serv['total_contratos']} "
        f"contratos, {_pct(topo['percentual'])}). Em média, cada cliente tem "
        f"{_num(serv['media_por_cliente'], 1)} serviços."
    )
    por_nivel = serv["por_nivel"]
    if "A" in por_nivel and "C" in por_nivel and por_nivel["A"] and por_nivel["C"]:
        a, c = por_nivel["A"][0]["categoria"], por_nivel["C"][0]["categoria"]
        if a != c:
            texto += f" Entre clientes A predomina {a}; entre clientes C, {c}."
    return _insight("SERVICO", "Serviço mais contratado", texto)


def _faturamento_e_servicos(ind):
    teste = ind["testes"]["correlacao_faturamento_servicos"]
    if not teste["rejeita_h0"] or teste["rho"] <= 0:
        return _insight(
            "SERVICO", "Faturamento e quantidade de serviços",
            f"Não há relação clara entre o porte do cliente e quantos serviços ele contrata "
            f"(Spearman ρ = {_num(teste['rho'])}, {formatar_p(teste['p_valor'])}).",
        )
    return _insight(
        "SERVICO", "Clientes maiores contratam mais serviços",
        f"Há correlação {teste['forca']} entre faturamento e número de serviços contratados "
        f"(Spearman ρ = {_num(teste['rho'])}, p < 0,05). Clientes de maior porte são o melhor alvo "
        "para venda cruzada de serviços complementares.",
    )


def _clientes_extremos(ind):
    fora = ind["faturamento"]["outliers"]["clientes"]
    if not fora:
        return None
    nomes = ", ".join(f"{c['nome']} ({_reais(c['faturamento'])})" for c in fora)
    quem = "1 cliente fatura" if len(fora) == 1 else f"{len(fora)} clientes faturam"
    return _insight(
        "FATURAMENTO", "Clientes fora da curva",
        f"{quem} muito acima do restante da carteira (acima de "
        f"{_reais(ind['faturamento']['outliers']['limite_superior'])}): {nomes}. "
        "Pedem atendimento dedicado e puxam a média para cima.",
    )


def _evolucao(ind):
    anos = ind["contratacoes"]["por_ano"]
    meses = ind["contratacoes"]["por_mes"]
    if not meses:
        return None
    maximo = max(m["clientes"] for m in meses)
    picos = [m for m in meses if m["clientes"] == maximo]
    vazios = sum(1 for m in meses if m["clientes"] == 0)
    if len(picos) == 1:
        texto = f"O mês com mais novos clientes foi {_mes(picos[0]['mes'])} ({maximo})."
    else:
        texto = f"As entradas são regulares: nenhum mês passou de {maximo} novos clientes."
    if vazios:
        texto += f" Em {vazios} {'mês' if vazios == 1 else 'meses'} não houve nenhuma contratação."
    if len(anos) >= 2:
        anterior, atual = anos[-2], anos[-1]
        variacao = (atual["media_clientes_mes"] / anterior["media_clientes_mes"] - 1) * 100
        sentido = "acima" if variacao > 0 else "abaixo"
        texto = (
            f"Em {atual['ano']} entraram em média {_num(atual['media_clientes_mes'])} clientes por mês "
            f"({atual['meses_cobertos']} meses), {_pct(abs(variacao))} {sentido} de {anterior['ano']} "
            f"({_num(anterior['media_clientes_mes'])} por mês). " + texto
        )
    return _insight("CONTRATACOES", "Ritmo de novas contratações", texto)


def _carteira_consultores(ind):
    consultores = ind["faturamento"]["por_consultor"]
    if len(consultores) < 2:
        return None
    maior = max(consultores, key=lambda c: c["soma"])
    total = sum(c["soma"] for c in consultores)
    menor = min(consultores, key=lambda c: c["soma"])
    return _insight(
        "CONSULTOR", "Carteira por consultor",
        f"{maior['consultor']} administra a carteira de maior faturamento ({_reais(maior['soma'])}, "
        f"{_pct(maior['soma'] / total * 100)} do total, em {maior['n']} clientes). A menor é a de "
        f"{menor['consultor']} ({_reais(menor['soma'])}, {menor['n']} clientes).",
    )


def _distribuicao_faturamento(ind):
    geral = ind["faturamento"]["geral"]
    normal = ind["testes"]["normalidade_faturamento"]
    if not normal.get("aplicavel"):
        return None
    texto = (
        f"Faturamento médio de {_reais(geral['media'])}, mas a mediana é {_reais(geral['mediana'])}: "
        f"metade dos clientes fatura até esse valor. O desvio padrão de {_reais(geral['desvio_padrao'])} "
        f"(coeficiente de variação de {_pct(geral['coeficiente_variacao'])}) mostra uma carteira bem heterogênea."
    )
    if normal["rejeita_h0"]:
        texto += (
            f" O teste de Shapiro-Wilk rejeita a normalidade ({formatar_p(normal['p_valor'])}), "
            "por isso as comparações usam mediana e testes não paramétricos."
        )
    return _insight("FATURAMENTO", "Como o faturamento se distribui", texto)


def _segmento_x_nivel(ind):
    teste = ind["testes"]["associacao_segmento_nivel"]
    if not teste.get("aplicavel"):
        return None
    return _insight("ESTATISTICA", "Segmento e nível do cliente (Qui-quadrado)", teste["conclusao"])


def _qualidade(ind):
    q = ind["qualidade"]
    if not q["linhas_rejeitadas"] and not q["avisos"]:
        return _insight("QUALIDADE", "Qualidade da planilha", "Todas as linhas passaram na validação sem correções.")
    rotulos = {"vazio": "campo obrigatório vazio", "formato": "valor fora do padrão",
               "duplicado": "código repetido", "email": "e-mail inválido"}
    motivos = ", ".join(f"{qtd} por {rotulos.get(tipo, tipo)}" for tipo, qtd in q["erros_por_tipo"].items())
    texto = (
        f"{q['linhas_validas']} de {q['linhas_lidas']} linhas foram aproveitadas ({_pct(q['percentual_aproveitado'])})."
    )
    if motivos:
        texto += f" Ficaram de fora: {motivos}."
    if q["avisos"]:
        texto += f" Outros {q['avisos']} valores foram padronizados automaticamente (ex.: \"IND.\" → \"Indústria\")."
    texto += " Corrigir esses pontos na origem deixa os próximos relatórios mais completos."
    return _insight("QUALIDADE", "Qualidade dos dados enviados", texto)


# ---------------------------------------------------------------------------
# Formatação pt-BR
# ---------------------------------------------------------------------------

def _insight(tipo, titulo, texto):
    return {"tipo": tipo, "titulo": titulo, "texto": texto}


def _num(valor, casas=2):
    return f"{valor:,.{casas}f}".replace(",", "§").replace(".", ",").replace("§", ".")


def _pct(valor):
    return f"{_num(valor, 0 if abs(valor - round(valor)) < 0.05 else 1)}%"


def _reais(valor):
    if abs(valor) >= 1e6:
        return f"R$ {_num(valor / 1e6, 2)} mi"
    return f"R$ {_num(valor, 0)}"


def _mes(ano_mes):
    ano, mes = ano_mes.split("-")
    return f"{MESES[int(mes) - 1]}/{ano}"
