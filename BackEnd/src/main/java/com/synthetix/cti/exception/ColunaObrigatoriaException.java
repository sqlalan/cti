package com.synthetix.cti.exception;

/**
 * A planilha nao traz uma coluna sem a qual o cliente nao pode ser cadastrado (ex.: "segmento").
 */
public class ColunaObrigatoriaException extends RuntimeException {

	private final String coluna;

	public ColunaObrigatoriaException(String coluna) {
		super("Coluna obrigatoria ausente na planilha: " + coluna);
		this.coluna = coluna;
	}

	public String getColuna() {
		return coluna;
	}

}
