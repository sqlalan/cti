package com.synthetix.cti.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Base dos insights gerados a partir dos dados de clientes. Cada tipo concreto guarda os proprios
 * numeros; todos ficam na mesma tabela, diferenciados pela coluna "tipo".
 */
@Entity
@Table(name = "insights")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", length = 30)
public abstract class Insight {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "gerado_em", nullable = false, updatable = false)
	private Instant geradoEm;

	// Cliente 1 -- N Insight (opcional: insights agregados nao pertencem a um cliente)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;

	// Contrato 1 -- N Insight (opcional)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contrato_id")
	private Contrato contrato;

	@PrePersist
	void aoGerar() {
		if (geradoEm == null) {
			geradoEm = Instant.now();
		}
	}

	/**
	 * Texto do insight pronto para exibicao. Cada subclasse formata do seu jeito.
	 */
	public abstract String gerarResumo();

	public Long getId() {
		return id;
	}

	public Instant getGeradoEm() {
		return geradoEm;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public Contrato getContrato() {
		return contrato;
	}

	public void setContrato(Contrato contrato) {
		this.contrato = contrato;
	}

}
