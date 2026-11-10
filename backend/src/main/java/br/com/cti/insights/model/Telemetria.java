package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro de um evento de processamento: upload de planilha ou execucao do script Python.
 */
@Entity
@Table(name = "telemetria")
public class Telemetria {

	public static final String UPLOAD_PLANILHA = "UPLOAD_PLANILHA";

	public static final String EXECUCAO_SCRIPT = "EXECUCAO_SCRIPT";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String evento;

	@Column(name = "data_hora", nullable = false, updatable = false)
	private LocalDateTime dataHora;

	@Column(length = 500)
	private String mensagem;

	@Column(nullable = false)
	private boolean sucesso;

	@Column(name = "registros_processados")
	private Integer registrosProcessados;

	@Column(name = "duracao_ms")
	private Long duracaoMs;

	public Telemetria() {
	}

	public Telemetria(String evento, boolean sucesso, Integer registrosProcessados, Long duracaoMs,
			String mensagem) {
		this.evento = evento;
		this.sucesso = sucesso;
		this.registrosProcessados = registrosProcessados;
		this.duracaoMs = duracaoMs;
		this.mensagem = mensagem;
	}

	@PrePersist
	void aoRegistrar() {
		if (dataHora == null) {
			dataHora = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEvento() {
		return evento;
	}

	public void setEvento(String evento) {
		this.evento = evento;
	}

	public LocalDateTime getDataHora() {
		return dataHora;
	}

	public void setDataHora(LocalDateTime dataHora) {
		this.dataHora = dataHora;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}

	public boolean isSucesso() {
		return sucesso;
	}

	public void setSucesso(boolean sucesso) {
		this.sucesso = sucesso;
	}

	public Integer getRegistrosProcessados() {
		return registrosProcessados;
	}

	public void setRegistrosProcessados(Integer registrosProcessados) {
		this.registrosProcessados = registrosProcessados;
	}

	public Long getDuracaoMs() {
		return duracaoMs;
	}

	public void setDuracaoMs(Long duracaoMs) {
		this.duracaoMs = duracaoMs;
	}

}
