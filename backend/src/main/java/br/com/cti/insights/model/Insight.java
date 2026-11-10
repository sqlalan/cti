package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Base dos insights gerados a partir dos dados de clientes. As subclasses ficam na mesma tabela,
 * separadas pela coluna "classe"; "tipo" e o assunto do insight (SEGMENTO, FATURAMENTO, SERVICO...).
 */
@Entity
@Table(name = "insights")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "classe", length = 30)
public abstract class Insight {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String tipo;

	@Column(length = 1000)
	private String descricao;

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

	public Insight() {
	}

	protected Insight(String tipo) {
		this.tipo = tipo;
	}

	// A descricao gravada e o resumo de cada subclasse no momento em que o insight nasce.
	@PrePersist
	void aoGerar() {
		if (geradoEm == null) {
			geradoEm = Instant.now();
		}
		if (descricao == null) {
			descricao = gerarResumo();
		}
	}

	/**
	 * Texto do insight pronto para exibicao. Cada subclasse formata do seu jeito.
	 */
	public abstract String gerarResumo();

	public abstract String getTitulo();

	/**
	 * RESUMO: numeros agregados pelo Java (por segmento, por faixa). A subclasse com texto da analise
	 * sobrescreve.
	 */
	public String getOrigem() {
		return "RESUMO";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Instant getGeradoEm() {
		return geradoEm;
	}

	public void setGeradoEm(Instant geradoEm) {
		this.geradoEm = geradoEm;
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
