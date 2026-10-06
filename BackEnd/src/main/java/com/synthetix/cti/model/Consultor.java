package com.synthetix.cti.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "consultores")
public class Consultor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String nome;

	// Opcional: consultor criado pela planilha so tem nome; a matricula chega no cadastro do usuario.
	@Column(unique = true, length = 20)
	private String matricula;

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

	public String getMatricula() {
		return matricula;
	}

	/**
	 * Em branco vira nulo: a coluna e unica, e dois "" colidiriam.
	 */
	public void setMatricula(String matricula) {
		this.matricula = matricula == null || matricula.isBlank() ? null : matricula.trim().toUpperCase(Locale.ROOT);
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
