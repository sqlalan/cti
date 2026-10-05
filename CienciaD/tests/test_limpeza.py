import unittest
from datetime import date, datetime

from esquema import achatar, nome_proprio
from limpeza import ColunaObrigatoriaError, PlanilhaInvalidaError, tratar

CABECALHO = ["codigo_cliente", "nome_cliente", "consultor", "segmento", "nivel_cliente",
             "faturamento_anual", "servicos_contratados", "data_contratacao", "cidade", "uf"]


def linha(codigo="CTI001", nome="Cliente", consultor="Ana Souza", segmento="Industria", nivel="A",
          faturamento=1000000, servicos="Internet Dedicada", data=datetime(2025, 1, 15),
          cidade="Campinas", uf="SP"):
    return [codigo, nome, consultor, segmento, nivel, faturamento, servicos, data, cidade, uf]


class Padronizacao(unittest.TestCase):

    def test_achatar_ignora_acento_caixa_e_pontuacao(self):
        self.assertEqual(achatar("  Código_Cliente "), "codigo cliente")

    def test_nome_gritado_vira_nome_proprio(self):
        self.assertEqual(nome_proprio("ANA SOUZA"), "Ana Souza")

    def test_particula_fica_minuscula_e_sigla_e_mantida(self):
        self.assertEqual(nome_proprio("joão DA silva"), "João da Silva")
        self.assertEqual(nome_proprio("Consultoria TI"), "Consultoria TI")

    def test_variantes_de_segmento_viram_uma_so(self):
        variantes = ["IND.", "Industria", "INDUSTRIA", " indústria ", "INDÚSTRIA"]
        dados, erros, _, _ = tratar([CABECALHO] + [linha(codigo=f"C{i}", segmento=s) for i, s in enumerate(variantes)])
        self.assertEqual(erros, [])
        self.assertEqual(set(dados["segmento"]), {"Indústria"})

    def test_servicos_separados_por_varios_separadores(self):
        dados, _, _, _ = tratar([CABECALHO, linha(servicos="internet dedicada, FIREWALL / nuvem;backup")])
        self.assertEqual(dados.loc[0, "servicos"], ["Internet Dedicada", "Firewall", "Cloud", "Backup"])

    def test_faturamento_em_texto_brasileiro(self):
        dados, _, _, _ = tratar([CABECALHO, linha(faturamento="R$ 1.850.000,50")])
        self.assertEqual(dados.loc[0, "faturamento"], 1850000.50)

    def test_data_em_texto_dd_mm_aaaa(self):
        dados, _, _, _ = tratar([CABECALHO, linha(data="03/02/2025")])
        self.assertEqual(dados.loc[0, "contratacao"].date(), date(2025, 2, 3))

    def test_codigo_e_nivel_em_maiuscula(self):
        dados, _, _, _ = tratar([CABECALHO, linha(codigo="cti004", nivel="b")])
        self.assertEqual((dados.loc[0, "codigo"], dados.loc[0, "nivel"]), ("CTI004", "B"))

    def test_padronizacao_gera_aviso_sem_derrubar_a_linha(self):
        dados, erros, avisos, _ = tratar([CABECALHO, linha(segmento="IND.")])
        self.assertEqual((len(dados), len(erros)), (1, 0))
        self.assertTrue(any(a["tipo"] == "padronizacao" for a in avisos))


class Validacao(unittest.TestCase):

    def problemas(self, **campos):
        _, erros, _, _ = tratar([CABECALHO, linha(**campos)])
        return [p["tipo"] for e in erros for p in e["problemas"]]

    def test_nivel_fora_de_abc(self):
        self.assertEqual(self.problemas(nivel="F"), ["formato"])

    def test_obrigatorio_vazio(self):
        self.assertEqual(self.problemas(consultor=None), ["vazio"])
        self.assertEqual(self.problemas(servicos=""), ["vazio"])

    def test_faturamento_negativo_ou_nao_numerico(self):
        self.assertEqual(self.problemas(faturamento=-10), ["formato"])
        self.assertEqual(self.problemas(faturamento="muito"), ["formato"])

    def test_data_inexistente(self):
        self.assertEqual(self.problemas(data="31/02/2025"), ["formato"])

    def test_uf_invalida(self):
        self.assertEqual(self.problemas(uf="XX"), ["formato"])

    def test_cidade_e_uf_sao_opcionais(self):
        self.assertEqual(self.problemas(cidade=None, uf=None), [])

    def test_codigo_repetido_recusa_a_segunda_linha(self):
        dados, erros, _, _ = tratar([CABECALHO, linha(), linha(nome="Filial")])
        self.assertEqual(len(dados), 1)
        self.assertEqual(erros[0]["linha"], 3)
        self.assertEqual(erros[0]["problemas"][0]["tipo"], "duplicado")

    def test_linha_totalmente_vazia_e_ignorada(self):
        dados, erros, _, _ = tratar([CABECALHO, linha(), [None] * len(CABECALHO)])
        self.assertEqual((len(dados), len(erros)), (1, 0))

    def test_cabecalho_com_outros_nomes(self):
        cabecalho = ["Código", "Razão Social", "Vendedor", "Setor", "Curva", "Receita", "Produtos", "Data", "Município", "Estado"]
        dados, erros, _, _ = tratar([cabecalho, linha()])
        self.assertEqual((len(dados), len(erros)), (1, 0))

    def test_coluna_obrigatoria_faltando(self):
        with self.assertRaises(ColunaObrigatoriaError) as contexto:
            tratar([CABECALHO[:4], ["CTI001", "X", "Ana", "Ind"]])
        self.assertIn("Nível", contexto.exception.colunas)

    def test_planilha_vazia(self):
        with self.assertRaises(PlanilhaInvalidaError):
            tratar([])


if __name__ == "__main__":
    unittest.main()
