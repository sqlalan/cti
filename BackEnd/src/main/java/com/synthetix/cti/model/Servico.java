package com.synthetix.cti.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

/**
 * Servico do catalogo CTI (Internet Dedicada, MPLS, Firewall...).
 */
@Entity
@Table(name = "servicos")
public class Servico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String nome;

	// Default no banco: linhas criadas antes desta coluna existir nao quebram o ddl-auto.
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@ColumnDefault("'OUTROS'")
	private CategoriaServico categoria = CategoriaServico.OUTROS;

	protected Servico() {
	}

	public Servico(String nome) {
		setNome(nome);
		this.categoria = CategoriaServico.para(nome);
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome.trim();
	}

	public CategoriaServico getCategoria() {
		return categoria;
	}

	public void setCategoria(CategoriaServico categoria) {
		this.categoria = categoria;
	}

}
