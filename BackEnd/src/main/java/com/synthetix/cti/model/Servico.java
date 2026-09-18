package com.synthetix.cti.model;

import jakarta.persistence.*;

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

	protected Servico() {
	}

	public Servico(String nome) {
		setNome(nome);
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

}
