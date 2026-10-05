package com.synthetix.cti.analise;

import java.nio.file.Path;

/**
 * Trata e analisa uma planilha salva em disco. A implementacao real chama o script Python; nos
 * testes pode ser trocada por uma falsa.
 */
public interface AnalisadorPlanilha {

	/**
	 * @throws com.synthetix.cti.exception.ExcelInvalidoException arquivo ilegivel ou vazio
	 * @throws com.synthetix.cti.exception.ColunaObrigatoriaException falta coluna no cabecalho
	 * @throws com.synthetix.cti.exception.ProcessamentoPlanilhaException falha do proprio script
	 */
	ResultadoAnalise analisar(Path planilha, Path pastaSaida);

}
