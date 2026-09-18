package com.synthetix.cti.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "consultores")
public class Consultor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String nome;

	// Consultor 1 -- N Cliente
	@OneToMany(mappedBy = "consultor")
	private List<Cliente> clientes = new ArrayList<>();

	protected Consultor() {
	}

	public Consultor(String nome) {
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

	/**
	 * Somente leitura: o vinculo e feito por {@link Cliente#setConsultor(Consultor)}, que mantem os
	 * dois lados da relacao coerentes.
	 */
	public List<Cliente> getClientes() {
		return Collections.unmodifiableList(clientes);
	}

	void adicionarCliente(Cliente cliente) {
		clientes.add(cliente);
	}

	void removerCliente(Cliente cliente) {
		clientes.remove(cliente);
	}

}
