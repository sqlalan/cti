package com.synthetix.cti.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "contratos")
public class Contrato {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Cliente 1 -- N Contrato
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Cliente cliente;

	// Contrato N -- 1 Servico
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "servico_id", nullable = false)
	private Servico servico;

	@Column(name = "data_contratacao", nullable = false)
	private LocalDate dataContratacao;

	// Contrato 1 -- N Insight
	@OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Insight> insights = new ArrayList<>();

	protected Contrato() {
	}

	/**
	 * Criado por {@link Cliente#contratar(Servico, LocalDate)} para que o contrato sempre entre na
	 * lista do cliente.
	 */
	Contrato(Cliente cliente, Servico servico, LocalDate dataContratacao) {
		this.cliente = cliente;
		this.servico = servico;
		this.dataContratacao = dataContratacao;
	}

	public Long getId() {
		return id;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public Servico getServico() {
		return servico;
	}

	public LocalDate getDataContratacao() {
		return dataContratacao;
	}

	public void setDataContratacao(LocalDate dataContratacao) {
		this.dataContratacao = dataContratacao;
	}

	public List<Insight> getInsights() {
		return Collections.unmodifiableList(insights);
	}

}
