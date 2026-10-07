package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "contratos")
public class Contrato {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "data_inicio", nullable = false)
	private LocalDate dataInicio;

	// Cancelado vira false e continua no historico.
	@Column(nullable = false)
	@ColumnDefault("true")
	private Boolean ativo = true;

	// N Contratos -- 1 Cliente
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Cliente cliente;

	// N Contratos -- 1 Servico
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "servico_id", nullable = false)
	private Servico servico;

	// Contrato 1 -- N Insight
	@OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Insight> insights = new ArrayList<>();

	public Contrato() {
	}

	/**
	 * Usado por {@link Cliente#contratar(Servico, LocalDate)}, que poe o contrato na lista do cliente.
	 */
	Contrato(Cliente cliente, Servico servico, LocalDate dataInicio) {
		this.cliente = cliente;
		this.servico = servico;
		this.dataInicio = dataInicio;
	}

	public void cancelar() {
		this.ativo = false;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(LocalDate dataInicio) {
		this.dataInicio = dataInicio;
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public Servico getServico() {
		return servico;
	}

	public void setServico(Servico servico) {
		this.servico = servico;
	}

	public List<Insight> getInsights() {
		return Collections.unmodifiableList(insights);
	}

	// Copia para a lista existente: com orphanRemoval o Hibernate recusa trocar a lista por outra.
	public void setInsights(List<Insight> insights) {
		this.insights.clear();
		if (insights != null) {
			this.insights.addAll(insights);
		}
	}

}
