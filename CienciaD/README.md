# CTI Insights — Ciência de Dados

Scripts em Python + Pandas que recebem a planilha de clientes da CTI e devolvem:

- os dados limpos;
- as estatísticas;
- os testes de hipótese;
- os gráficos;
- os insights em texto.

O Spring Boot chama estes scripts pelo `ProcessBuilder` depois do upload. Também dá para rodar direto no terminal.

## Como rodar

```bash
cd CienciaD
py -m pip install -r requirements.txt        # no Linux/Mac: python3 -m pip ...
py main.py ../FrontEnd/planilhas-exemplo/CTI_Insights_modelo_upload_aula.xlsx
```

Opções:
- `--saida <pasta>` escolhe onde os resultados são gravados. O padrão é `CienciaD/output`.
- `--sem-graficos` pula os PNGs, o que deixa o processamento mais rápido.

Testes: `py -m unittest discover -s tests -t .`

## Arquivos

| Arquivo | O que faz |
| --- | --- |
| `esquema.py` | Colunas esperadas, apelidos aceitos no cabeçalho e dicionários de segmento e serviço. **Espelha o `FrontEnd/src/dados/esquemaPlanilha.js`**: se mudar uma regra lá, mude aqui também. |
| `limpeza.py` | Leitura (.xlsx, .xls e .csv), limpeza, padronização e validação linha a linha. |
| `analise.py` | Estatística descritiva, cruzamentos, série temporal e testes de hipótese. |
| `graficos.py` | Gráficos de linha, dispersão, histograma, boxplot e barras, em PNG. |
| `insights.py` | Frases de insight geradas a partir dos números. Nenhum texto é fixo. |
| `main.py` | Ponto de entrada. Junta tudo e grava os resultados. |

## O que é gerado

| Arquivo | Conteúdo |
| --- | --- |
| `clientes_tratados.json` | Linhas válidas e padronizadas, prontas para o back-end gravar. |
| `problemas.json` | Linhas recusadas (com número da linha no Excel e motivo) e avisos de correção. |
| `indicadores.json` | Qualidade da planilha, frequências, medidas de faturamento, cruzamentos, contratações por mês e por ano, e os testes. Alimenta o dashboard. |
| `insights.json` | Lista de `{ tipo, titulo, texto }` para exibir no dashboard. |
| `graficos/*.png` | `linha_contratacoes`, `dispersao_faturamento_servicos`, `histograma_faturamento`, `boxplot_faturamento_segmento`, `barras_segmento` e `barras_nivel`. |

A pasta `output/` já traz o resultado da planilha modelo, que tem só dados fictícios. Planilhas reais da CTI são confidenciais e **não devem ser commitadas**.

### Contrato com o back-end

A **última linha do stdout** é um JSON de resumo.

Quando dá certo:

```json
{"sucesso": true, "linhas_lidas": 39, "linhas_validas": 31, "linhas_rejeitadas": 8, "insights": 11, ...}
```

Quando a planilha é inválida:

```json
{"sucesso": false, "tipo": "COLUNA_OBRIGATORIA", "mensagem": "...", "colunas": ["Segmento"]}
```

| Código de saída | Significado | Exceção no Java |
| --- | --- | --- |
| 0 | Processado | — |
| 2, `tipo = EXCEL_INVALIDO` | Arquivo ilegível, vazio ou de formato não suportado | `ExcelInvalidoException` |
| 2, `tipo = COLUNA_OBRIGATORIA` | Falta coluna obrigatória no cabeçalho | `ColunaObrigatoriaException` |
| 1, `tipo = ERRO_INTERNO` | Falha inesperada no script | erro 500 |

## Tratamento dos dados

As regras seguem a aba `dicionario_dados` da planilha modelo.

**Limpeza e padronização** (a linha continua válida, e a correção é registrada como aviso):
- Remove espaços sobrando, inclusive o espaço não separável que vem de páginas web.
- Segmentos: "IND.", "Industria", "INDUSTRIA" e " indústria " viram **Indústria**. O mesmo vale para Comércio, Serviços, Saúde, Tecnologia e Educação.
- Nomes: "ANA SOUZA" vira **Ana Souza**, para não virar um consultor diferente no agrupamento.
- Código e nível passam para maiúscula ("cti004" vira CTI004, "b" vira B).
- Faturamento aceita `1850000`, `1850000.00` e `R$ 1.850.000,00`.
- Serviços podem vir separados por `;`, `,` ou `/`.

**Validação** (a linha é recusada):
- campo obrigatório vazio;
- nível diferente de A, B ou C;
- faturamento negativo ou que não é número;
- data inexistente ou fora de dd/mm/aaaa;
- UF inválida;
- código de cliente repetido.

## Métodos estatísticos

| Tópico da ementa | Onde aparece |
| --- | --- |
| Escalas de medição | Segmento e consultor são nominais. Nível A/B/C é ordinal. Faturamento é de razão. Data de contratação é temporal. |
| Tendência central | Média, mediana e moda do faturamento (geral, por segmento, por nível e por consultor), e moda de segmento, nível e serviço. |
| Dispersão | Amplitude, variância, desvio padrão amostral, coeficiente de variação, quartis, IQR e percentil 90. |
| Valores extremos | Regra do boxplot: acima de Q3 + 1,5·IQR. |
| Teste de normalidade | **Shapiro-Wilk** no faturamento e no log do faturamento. |
| Associação | **Qui-quadrado** entre segmento e nível, com V de Cramér e checagem da regra de Cochran. |
| Correlação | **Spearman** (e Pearson, para comparar) entre faturamento e número de serviços. |
| Comparação de grupos | **Kruskal-Wallis**: o faturamento difere entre os níveis A, B e C? |
| Série temporal | Contratações por mês (meses sem venda contam como zero) e média mensal por ano. |
| Gráficos | Linha, dispersão, histograma e boxplot, mais barras por segmento e por nível. |

Nível de significância usado em todos os testes: **α = 0,05**.

## Resultados com a planilha modelo

**Qualidade da planilha:** 31 de 39 linhas foram aproveitadas (79,5%). As 8 recusadas foram:
- 4 com nível fora de A/B/C;
- 3 com campo obrigatório vazio;
- 1 com código repetido.

Além disso, 34 valores foram padronizados automaticamente.

**Faturamento por nível:**

| Nível | Clientes | Média | Mediana | Desvio padrão |
| --- | --- | --- | --- | --- |
| A | 12 | R$ 4,31 mi | R$ 4,03 mi | R$ 1,60 mi |
| B | 13 | R$ 1,21 mi | R$ 1,17 mi | R$ 0,37 mi |
| C | 6 | R$ 0,52 mi | R$ 0,51 mi | R$ 0,11 mi |

**Interpretação dos testes:**

- **Shapiro-Wilk** (W = 0,844, p = 0,0004): o faturamento **não é normal**. Ele é assimétrico, com poucos clientes muito grandes puxando a média (R$ 2,27 mi) para longe da mediana (R$ 1,42 mi). Por isso usamos a mediana e testes não paramétricos. O log do faturamento passa no teste (p = 0,297).
- **Kruskal-Wallis** (H = 25,9, p < 0,0001): o faturamento difere entre os níveis. A classificação A/B/C acompanha o porte do cliente.
- **Spearman** (ρ = 0,83, p < 0,0001): correlação positiva forte. Quem fatura mais contrata mais serviços, o que aponta para venda cruzada.
- **Qui-quadrado** (χ² = 15,9, gl = 10, p = 0,103): não há evidência de associação entre segmento e nível. Mas todas as células têm frequência esperada abaixo de 5, o que viola a regra de Cochran. Com 31 clientes divididos em 18 combinações de segmento e nível, o resultado é só indicativo.

**Principais insights:**
- O nível A tem 39% dos clientes e gera 73% do faturamento.
- Indústria tem a maior mediana de faturamento.
- Comércio tem mais clientes, mas com o menor valor por cliente.
- Internet Dedicada é o serviço mais contratado.

A lista completa está em `output/insights.json`.

## Limitações

Como prevê o plano de ensino, o alcance das análises depende da quantidade e da qualidade dos dados:

- **A amostra é pequena.** Com 31 clientes, os testes por subgrupo (por exemplo, segmentos com 3 clientes) têm pouca força.
- **A série temporal é curta e sem histórico.** Cada cliente aparece uma vez, na data de contratação, e não há cancelamentos nem renovações. Por isso a análise é de entrada de clientes, e não de faturamento ao longo do tempo.
- **O resultado depende do dicionário.** Segmento ou serviço fora do dicionário passa adiante como aviso. É preciso cadastrar o sinônimo nos dois esquemas (Python e front) para que ele seja agrupado corretamente.
