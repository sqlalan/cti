package com.synthetix.cti.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Registro de um evento de processamento: upload de planilha ou execucao do script Python.
 */
@Entity
@Table(name = "telemetria")
public class Telemetria {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_evento", nullable = false, length = 30)
	private TipoEventoTelemetria tipoEvento;

	@Column(nullable = false)
	private boolean sucesso;

	@Column(name = "registros_processados")
	private Integer registrosProcessados;

	@Column(name = "duracao_ms")
	private Long duracaoMs;

	@Column(length = 500)
	private String detalhe;

	@Column(name = "ocorrido_em", nullable = false, updatable = false)
	private Instant ocorridoEm;

	protected Telemetria() {
	}

	public Telemetria(TipoEventoTelemetria tipoEvento, boolean sucesso, Integer registrosProcessados,
			Long duracaoMs, String detalhe) {
		this.tipoEvento = tipoEvento;
		this.sucesso = sucesso;
		this.registrosProcessados = registrosProcessados;
		this.duracaoMs = duracaoMs;
		this.detalhe = detalhe;
	}

	@PrePersist
	void aoRegistrar() {
		if (ocorridoEm == null) {
			ocorridoEm = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public TipoEventoTelemetria getTipoEvento() {
		return tipoEvento;
	}

	public boolean isSucesso() {
		return sucesso;
	}

	public Integer getRegistrosProcessados() {
		return registrosProcessados;
	}

	public Long getDuracaoMs() {
		return duracaoMs;
	}

	public String getDetalhe() {
		return detalhe;
	}

	public Instant getOcorridoEm() {
		return ocorridoEm;
	}

}
