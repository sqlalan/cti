package br.com.cti.insights.service;

import br.com.cti.insights.dto.ClienteDTO;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Validacao complementar do lote de clientes: o que o DTO nao enxerga sozinho (codigo repetido entre
 * linhas) e a padronizacao dos textos. Nao grava no banco e nao chama Python; devolve a lista pronta.
 *
 * As regras de padronizacao sao as mesmas do front (esquemaPlanilha.js) e do Python (esquema.py).
 */
@Service
public class ClienteValidacaoService {

	// Sinonimos na forma "achatada" (sem acento, minuscula, sem pontuacao), como nos outros dois lados.
	private static final Map<String, List<String>> SEGMENTOS = new LinkedHashMap<>();

	private static final Map<String, List<String>> SERVICOS = new LinkedHashMap<>();

	static {
		SEGMENTOS.put("Indústria", List.of("industria", "ind", "industrial", "fabrica"));
		SEGMENTOS.put("Comércio", List.of("comercio", "com", "comercial", "varejo"));
		SEGMENTOS.put("Serviços", List.of("servicos", "servico", "serv"));
		SEGMENTOS.put("Saúde", List.of("saude", "hospitalar"));
		SEGMENTOS.put("Tecnologia", List.of("tecnologia", "tec", "ti"));
		SEGMENTOS.put("Educação", List.of("educacao", "ensino", "educacional"));

		SERVICOS.put("Internet Dedicada", List.of("internet dedicada", "internet"));
		SERVICOS.put("Link Empresarial", List.of("link empresarial", "link"));
		SERVICOS.put("MPLS", List.of("mpls"));
		SERVICOS.put("Firewall", List.of("firewall"));
		SERVICOS.put("Backup", List.of("backup"));
		SERVICOS.put("Cloud", List.of("cloud", "nuvem"));
	}

	private static final Set<String> PARTICULAS = Set.of("de", "da", "do", "das", "dos", "e", "em", "a", "o");

	private static final Pattern SEPARADOR_SERVICOS = Pattern.compile("[;,/]");

	public List<ClienteDTO> validarEPreparar(List<ClienteDTO> clientes) {
		// Regra de lote: so comparando as linhas entre si da para achar o codigo repetido.
		Set<String> codigos = new HashSet<>();
		for (ClienteDTO cliente : clientes) {
			String codigo = limpar(cliente.codigoCliente()).toUpperCase(Locale.ROOT);
			if (!codigos.add(codigo)) {
				throw new IllegalArgumentException("Código repetido na planilha: " + codigo);
			}
		}
		return clientes.stream().map(this::padronizar).toList();
	}

	private ClienteDTO padronizar(ClienteDTO c) {
		return new ClienteDTO(limpar(c.codigoCliente()).toUpperCase(Locale.ROOT), formatarNome(c.nomeCliente()),
				formatarNome(c.consultor()), padronizarSegmento(c.segmento()),
				limpar(c.nivelCliente()).toUpperCase(Locale.ROOT), c.faturamentoAnual(),
				padronizarServicos(c.servicosContratados()), c.dataContratacao(), formatarNome(c.cidade()),
				limpar(c.uf()).toUpperCase(Locale.ROOT));
	}

	/**
	 * "IND.", "industria" e "INDÚSTRIA" viram "Indústria". Segmento fora do dicionario passa so com o
	 * nome arrumado: pode ser um segmento novo.
	 */
	private String padronizarSegmento(String segmento) {
		String canonico = consultar(SEGMENTOS, segmento);
		return canonico != null ? canonico : formatarNome(segmento);
	}

	// "internet dedicada, FIREWALL / nuvem" vira "Internet Dedicada;Firewall;Cloud".
	private String padronizarServicos(String servicos) {
		List<String> itens = new ArrayList<>();
		for (String pedaco : SEPARADOR_SERVICOS.split(limpar(servicos))) {
			if (limpar(pedaco).isEmpty()) {
				continue;
			}
			String canonico = consultar(SERVICOS, pedaco);
			String item = canonico != null ? canonico : formatarNome(pedaco);
			if (!itens.contains(item)) {
				itens.add(item);
			}
		}
		return String.join(";", itens);
	}

	/**
	 * "ANA SOUZA" vira "Ana Souza" e "joão DA silva" vira "João da Silva". Sigla curta em texto de caixa
	 * mista fica como esta ("Consultoria TI").
	 */
	private String formatarNome(String nome) {
		String limpo = limpar(nome);
		if (limpo.isEmpty()) {
			return limpo;
		}
		boolean gritado = limpo.equals(limpo.toUpperCase(Locale.ROOT));
		String[] palavras = limpo.split(" ");
		for (int i = 0; i < palavras.length; i++) {
			String palavra = palavras[i];
			if (i > 0 && PARTICULAS.contains(achatar(palavra))) {
				palavras[i] = palavra.toLowerCase(Locale.ROOT);
			}
			else if (!gritado && palavra.length() <= 3 && palavra.matches("\\p{Lu}+")) {
				palavras[i] = palavra;
			}
			else {
				palavras[i] = palavra.substring(0, 1).toUpperCase(Locale.ROOT)
						+ palavra.substring(1).toLowerCase(Locale.ROOT);
			}
		}
		return String.join(" ", Arrays.asList(palavras));
	}

	private static String consultar(Map<String, List<String>> dicionario, String bruto) {
		String chave = achatar(bruto);
		return dicionario.entrySet()
			.stream()
			.filter(item -> item.getValue().contains(chave))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElse(null);
	}

	// Espaco sobrando fora, inclusive o nao separavel ( ) que vem de copia da web.
	private static String limpar(String texto) {
		return texto == null ? "" : texto.replaceAll("[\\s\\u00a0]+", " ").trim();
	}

	// So para comparar: sem acento, minuscula, sem ponto/traco ("IND." e "ind" batem).
	private static String achatar(String texto) {
		String semAcento = Normalizer.normalize(limpar(texto), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcento.toLowerCase(Locale.ROOT).replaceAll("[._-]+", " ").replaceAll("\\s+", " ").trim();
	}

}
