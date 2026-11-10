package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Uma linha do lote como chegou do Pinia, ja validada e padronizada pelo Spring (aula 10). E o que o
 * PythonEtlService envia ao Python.
 */
@Entity
@Table(name = "upload_linha")
public class UploadLinha {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Apagar o lote apaga as linhas dele.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lote_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private UploadLote lote;

	// Posicao na lista enviada (1 = a primeira).
	@Column(name = "numero_linha", nullable = false)
	private int numeroLinha;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private StatusLinha status = StatusLinha.VALIDA;

	@Column(name = "codigo_cliente", nullable = false, length = 30)
	private String codigoCliente;

	@Column(name = "nome_cliente", nullable = false, length = 150)
	private String nomeCliente;

	@Column(nullable = false, length = 120)
	private String consultor;

	@Column(nullable = false, length = 100)
	private String segmento;

	@Column(name = "nivel_cliente", nullable = false, length = 1)
	private String nivelCliente;

	@Column(name = "faturamento_anual", nullable = false, precision = 15, scale = 2)
	private BigDecimal faturamentoAnual;

	// Separados por ";" (Internet Dedicada;Firewall).
	@Column(name = "servicos_contratados", nullable = false, length = 500)
	private String servicosContratados;

	@Column(name = "data_contratacao", nullable = false)
	private LocalDate dataContratacao;

	@Column(length = 100)
	private String cidade;

	@Column(length = 2)
	private String uf;

	public UploadLinha() {
	}

	public UploadLinha(UploadLote lote, int numeroLinha) {
		this.lote = lote;
		this.numeroLinha = numeroLinha;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UploadLote getLote() {
		return lote;
	}

	public void setLote(UploadLote lote) {
		this.lote = lote;
	}

	public int getNumeroLinha() {
		return numeroLinha;
	}

	public void setNumeroLinha(int numeroLinha) {
		this.numeroLinha = numeroLinha;
	}

	public StatusLinha getStatus() {
		return status;
	}

	public void setStatus(StatusLinha status) {
		this.status = status;
	}

	public String getCodigoCliente() {
		return codigoCliente;
	}

	public void setCodigoCliente(String codigoCliente) {
		this.codigoCliente = codigoCliente;
	}

	public String getNomeCliente() {
		return nomeCliente;
	}

	public void setNomeCliente(String nomeCliente) {
		this.nomeCliente = nomeCliente;
	}

	public String getConsultor() {
		return consultor;
	}

	public void setConsultor(String consultor) {
		this.consultor = consultor;
	}

	public String getSegmento() {
		return segmento;
	}

	public void setSegmento(String segmento) {
		this.segmento = segmento;
	}

	public String getNivelCliente() {
		return nivelCliente;
	}

	public void setNivelCliente(String nivelCliente) {
		this.nivelCliente = nivelCliente;
	}

	public BigDecimal getFaturamentoAnual() {
		return faturamentoAnual;
	}

	public void setFaturamentoAnual(BigDecimal faturamentoAnual) {
		this.faturamentoAnual = faturamentoAnual;
	}

	public String getServicosContratados() {
		return servicosContratados;
	}

	public void setServicosContratados(String servicosContratados) {
		this.servicosContratados = servicosContratados;
	}

	public LocalDate getDataContratacao() {
		return dataContratacao;
	}

	public void setDataContratacao(LocalDate dataContratacao) {
		this.dataContratacao = dataContratacao;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

}
