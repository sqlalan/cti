package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.hibernate.annotations.ColumnDefault;

/**
 * Servico do catalogo CTI (Internet Dedicada, MPLS, Firewall...).
 */
@Entity
@Table(name = "servicos")
public class Servico {

	// Chaves sem acento e em minuscula, como o Python padroniza os nomes.
	private static final Map<String, String> CATEGORIA_POR_NOME = Map.of("internet dedicada", "Conectividade",
			"link empresarial", "Conectividade", "mpls", "Conectividade", "firewall", "Segurança", "backup",
			"Segurança", "cloud", "Nuvem");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String nome;

	@Column(nullable = false, length = 30)
	@ColumnDefault("'Outros'")
	private String categoria = "Outros";

	// 1 Servico -- N Contratos
	@OneToMany(mappedBy = "servico")
	private List<Contrato> contratos = new ArrayList<>();

	public Servico() {
	}

	/**
	 * A categoria sai do nome: quem cadastra o servico (inclusive a planilha) nao precisa informar.
	 */
	public Servico(String nome) {
		setNome(nome);
		this.categoria = categoriaPara(nome);
	}

	static String categoriaPara(String nomeServico) {
		String chave = Normalizer.normalize(nomeServico.trim(), Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT);
		return CATEGORIA_POR_NOME.getOrDefault(chave, "Outros");
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome.trim();
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public List<Contrato> getContratos() {
		return Collections.unmodifiableList(contratos);
	}

}
