package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "consultor")
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

	public Consultor() {
	}

	public Consultor(String nome) {
		setNome(nome);
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

	public String getMatricula() {
		return matricula;
	}

	/**
	 * Em branco vira nulo: a coluna e unica, e dois "" colidiriam.
	 */
	public void setMatricula(String matricula) {
		this.matricula = matricula == null || matricula.isBlank() ? null : matricula.trim().toUpperCase(Locale.ROOT);
	}

	public List<Cliente> getClientes() {
		return Collections.unmodifiableList(clientes);
	}

	/**
	 * Lado mappedBy: muda so a lista em memoria. No banco o vinculo e o consultor_id do cliente,
	 * gravado por {@link Cliente#setConsultor(Consultor)}.
	 */
	public void setClientes(List<Cliente> clientes) {
		this.clientes.clear();
		if (clientes != null) {
			this.clientes.addAll(clientes);
		}
	}

	void adicionarCliente(Cliente cliente) {
		clientes.add(cliente);
	}

	void removerCliente(Cliente cliente) {
		clientes.remove(cliente);
	}

}
