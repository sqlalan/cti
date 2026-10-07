package br.com.cti.insights.analise;

import java.nio.file.Path;

/**
 * Trata e analisa uma planilha salva em disco. A implementacao real chama o script Python; nos
 * testes pode ser trocada por uma falsa.
 */
public interface AnalisadorPlanilha {

	/**
	 * @throws br.com.cti.insights.exception.ExcelInvalidoException arquivo ilegivel ou vazio
	 * @throws br.com.cti.insights.exception.ColunaObrigatoriaException falta coluna no cabecalho
	 * @throws br.com.cti.insights.exception.ProcessamentoPlanilhaException falha do proprio script
	 */
	ResultadoAnalise analisar(Path planilha, Path pastaSaida);

}
