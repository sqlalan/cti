import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

import pandas as pd

import analise
import insights
from limpeza import carregar

RAIZ = Path(__file__).resolve().parents[1]
PLANILHA = RAIZ.parent / "FrontEnd" / "planilhas-exemplo" / "dados.xlsx"


class Medidas(unittest.TestCase):

    def test_medidas_conhecidas(self):
        m = analise.medidas([1, 2, 3, 4, 100])
        self.assertEqual((m["media"], m["mediana"], m["amplitude"]), (22, 3, 99))
        self.assertAlmostEqual(m["desvio_padrao"], pd.Series([1, 2, 3, 4, 100]).std())

    def test_um_valor_so_nao_tem_desvio(self):
        self.assertIsNone(analise.medidas([5])["desvio_padrao"])

    def test_frequencia_respeita_ordem_e_inclui_zeros(self):
        freq = analise.frequencia(pd.Series(["B", "A", "B"]), ordem=["A", "B", "C"])
        self.assertEqual([(f["categoria"], f["quantidade"]) for f in freq], [("A", 1), ("B", 2), ("C", 0)])

    def test_p_valor_formatado(self):
        self.assertEqual(analise.formatar_p(0.00001), "p < 0,0001")
        self.assertEqual(analise.formatar_p(0.1034), "p = 0,103")


@unittest.skipUnless(PLANILHA.exists(), "planilha modelo não encontrada")
class PlanilhaModelo(unittest.TestCase):
    """Números conferidos à mão na aba upload_clientes da planilha modelo."""

    @classmethod
    def setUpClass(cls):
        cls.dados, cls.erros, cls.avisos, _ = carregar(PLANILHA)
        cls.ind = analise.analisar(cls.dados, cls.erros, cls.avisos)

    def test_linhas_aproveitadas_e_recusadas(self):
        q = self.ind["qualidade"]
        self.assertEqual((q["linhas_lidas"], q["linhas_validas"], q["linhas_rejeitadas"]), (39, 31, 8))
        recusados = {e["codigo"] for e in self.erros}
        self.assertEqual(recusados, {"CTI001", "CTI031", "CTI032", "CTI033", "CTI035", "CTI036", "CTI037", "CTI038"})

    def test_segmentos_e_consultores_padronizados(self):
        self.assertEqual(set(self.dados["segmento"]),
                         {"Indústria", "Comércio", "Serviços", "Saúde", "Tecnologia", "Educação"})
        self.assertEqual(set(self.dados["consultor"]), {"Ana Souza", "Carlos Lima", "Marcos Reis", "Carla Mendes"})

    def test_nivel_a_concentra_o_faturamento(self):
        part = {p["nivel"]: p["percentual"] for p in self.ind["faturamento"]["participacao_por_nivel"]}
        self.assertAlmostEqual(part["A"], 51.7 / 70.51 * 100, places=3)

    def test_testes_estatisticos(self):
        t = self.ind["testes"]
        self.assertTrue(t["normalidade_faturamento"]["rejeita_h0"])
        self.assertTrue(t["correlacao_faturamento_servicos"]["rejeita_h0"])
        self.assertFalse(t["associacao_segmento_nivel"]["pressuposto_atendido"])

    def test_insights_gerados(self):
        lista = insights.gerar(self.ind)
        self.assertGreaterEqual(len(lista), 3)
        self.assertTrue(all(i["tipo"] and i["titulo"] and i["texto"] for i in lista))


@unittest.skipUnless(PLANILHA.exists(), "planilha modelo não encontrada")
class LinhaDeComando(unittest.TestCase):
    """O contrato com o Java: arquivos em --saida, resumo JSON na última linha e código de saída."""

    def executar(self, *args):
        processo = subprocess.run([sys.executable, str(RAIZ / "main.py"), *args],
                                  capture_output=True, text=True, encoding="utf-8")
        return processo.returncode, json.loads(processo.stdout.strip().splitlines()[-1])

    def test_sucesso_gera_os_arquivos(self):
        with tempfile.TemporaryDirectory() as pasta:
            codigo, resumo = self.executar(str(PLANILHA), "--saida", pasta)
            self.assertEqual(codigo, 0)
            self.assertTrue(resumo["sucesso"])
            for nome in ["clientes_tratados.json", "problemas.json", "indicadores.json", "insights.json"]:
                self.assertTrue((Path(pasta) / nome).exists(), nome)
            self.assertEqual(len(list((Path(pasta) / "graficos").glob("*.png"))), 6)

    def test_coluna_faltando_devolve_codigo_2(self):
        with tempfile.TemporaryDirectory() as pasta:
            csv = Path(pasta) / "sem_nivel.csv"
            csv.write_text("codigo;nome;consultor\nCTI001;Cliente;Ana\n", encoding="utf-8")
            codigo, resumo = self.executar(str(csv), "--saida", pasta, "--sem-graficos")
            self.assertEqual(codigo, 2)
            self.assertEqual(resumo["tipo"], "COLUNA_OBRIGATORIA")
            self.assertIn("Segmento", resumo["colunas"])

    def test_arquivo_ilegivel_devolve_excel_invalido(self):
        with tempfile.TemporaryDirectory() as pasta:
            falso = Path(pasta) / "falso.xlsx"
            falso.write_text("isto não é um excel", encoding="utf-8")
            codigo, resumo = self.executar(str(falso), "--saida", pasta)
            self.assertEqual((codigo, resumo["tipo"]), (2, "EXCEL_INVALIDO"))


if __name__ == "__main__":
    unittest.main()
