package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "clientes")
public class Cliente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Codigo CTI do cliente; e a chave de negocio usada na planilha. */
	@Column(nullable = false, unique = true, length = 30)
	private String codigo;

	@Column(nullable = false, length = 160)
	private String nome;

	@Column(nullable = false, length = 60)
	private String segmento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 1)
	private NivelCliente nivel;

	@Column(name = "faturamento_anual", nullable = false, precision = 15, scale = 2)
	private BigDecimal faturamentoAnual;

	@Column(nullable = false, length = 100)
	private String cidade;

	@Column(nullable = false, length = 2)
	private String uf;

	// Consultor 1 -- N Cliente
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "consultor_id", nullable = false)
	private Consultor consultor;

	// Cliente 1 -- N Contrato
	@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Contrato> contratos = new ArrayList<>();

	// Cliente 1 -- N Insight
	@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Insight> insights = new ArrayList<>();

	public Cliente() {
	}

	public Cliente(String codigo, String nome, Consultor consultor) {
		setCodigo(codigo);
		setNome(nome);
		setConsultor(consultor);
	}

	/**
	 * Classifica o porte do cliente pelo faturamento anual. Quem chama nao precisa conhecer os
	 * limites de cada faixa.
	 */
	public FaixaFaturamento calcularFaixaFaturamento() {
		return FaixaFaturamento.para(faturamentoAnual);
	}

	/**
	 * Cria o contrato ja vinculado a este cliente, mantendo a lista de contratos coerente.
	 */
	public Contrato contratar(Servico servico, LocalDate dataInicio) {
		Contrato contrato = new Contrato(this, servico, dataInicio);
		contratos.add(contrato);
		return contrato;
	}

	public void removerContrato(Contrato contrato) {
		contratos.remove(contrato);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo.trim().toUpperCase(Locale.ROOT);
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome.trim();
	}

	public String getSegmento() {
		return segmento;
	}

	public void setSegmento(String segmento) {
		this.segmento = segmento.trim();
	}

	public NivelCliente getNivel() {
		return nivel;
	}

	public void setNivel(NivelCliente nivel) {
		this.nivel = nivel;
	}

	public BigDecimal getFaturamentoAnual() {
		return faturamentoAnual;
	}

	public void setFaturamentoAnual(BigDecimal faturamentoAnual) {
		if (faturamentoAnual == null || faturamentoAnual.signum() < 0) {
			throw new IllegalArgumentException("Faturamento anual deve ser zero ou positivo");
		}
		this.faturamentoAnual = faturamentoAnual;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade.trim();
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf.trim().toUpperCase(Locale.ROOT);
	}

	public Consultor getConsultor() {
		return consultor;
	}

	/**
	 * Troca o consultor responsavel e atualiza a lista de clientes dos dois consultores.
	 */
	public void setConsultor(Consultor consultor) {
		if (this.consultor != null) {
			this.consultor.removerCliente(this);
		}
		this.consultor = consultor;
		consultor.adicionarCliente(this);
	}

	public List<Contrato> getContratos() {
		return Collections.unmodifiableList(contratos);
	}

	// Copia para a lista existente: com orphanRemoval o Hibernate recusa trocar a lista por outra.
	public void setContratos(List<Contrato> contratos) {
		this.contratos.clear();
		if (contratos != null) {
			this.contratos.addAll(contratos);
		}
	}

	public List<Insight> getInsights() {
		return Collections.unmodifiableList(insights);
	}

	public void setInsights(List<Insight> insights) {
		this.insights.clear();
		if (insights != null) {
			this.insights.addAll(insights);
		}
	}

}
