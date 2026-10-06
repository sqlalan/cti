package com.synthetix.cti.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Familia do servico no catalogo CTI. Deduzida do nome: quem cadastra o servico (inclusive a
 * importacao da planilha) nao precisa informar.
 */
public enum CategoriaServico {

	CONECTIVIDADE("Conectividade"), SEGURANCA("Segurança"), NUVEM("Nuvem"), OUTROS("Outros");

	// Chaves sem acento e em minuscula, como o Python padroniza os nomes.
	private static final Map<String, CategoriaServico> POR_NOME = Map.of("internet dedicada", CONECTIVIDADE,
			"link empresarial", CONECTIVIDADE, "mpls", CONECTIVIDADE, "firewall", SEGURANCA, "backup", SEGURANCA,
			"cloud", NUVEM);

	private final String descricao;

	CategoriaServico(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

	static CategoriaServico para(String nomeServico) {
		String chave = Normalizer.normalize(nomeServico.trim(), Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT);
		return POR_NOME.getOrDefault(chave, OUTROS);
	}

}
