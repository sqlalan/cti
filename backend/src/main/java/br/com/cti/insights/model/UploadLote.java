package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Um envio de planilha. O Spring grava as linhas validas e o Python atualiza o status e a mensagem
 * direto no banco (banco.py).
 */
@Entity
@Table(name = "upload_lote")
public class UploadLote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "arquivo_nome", nullable = false)
	private String arquivoNome;

	// E-mail de quem enviou (subject do token).
	@Column(name = "enviado_por", nullable = false, length = 180)
	private String enviadoPor;

	@Column(name = "enviado_em", nullable = false, updatable = false)
	private Instant enviadoEm;

	// Linhas da planilha, boas e ruins.
	@Column(name = "total_linhas", nullable = false)
	private int totalLinhas;

	@Column(name = "linhas_validas", nullable = false)
	private int linhasValidas;

	@Column(name = "linhas_invalidas", nullable = false)
	private int linhasInvalidas;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusLote status = StatusLote.AGUARDANDO_ETL;

	@Column(length = 500)
	private String mensagem;

	public UploadLote() {
	}

	public UploadLote(String arquivoNome, String enviadoPor, int totalLinhas, int linhasValidas) {
		this.arquivoNome = arquivoNome;
		this.enviadoPor = enviadoPor;
		this.totalLinhas = totalLinhas;
		this.linhasValidas = linhasValidas;
		this.linhasInvalidas = totalLinhas - linhasValidas;
		this.mensagem = "Aguardando o serviço Python.";
	}

	@PrePersist
	void aoEnviar() {
		if (enviadoEm == null) {
			enviadoEm = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getArquivoNome() {
		return arquivoNome;
	}

	public void setArquivoNome(String arquivoNome) {
		this.arquivoNome = arquivoNome;
	}

	public String getEnviadoPor() {
		return enviadoPor;
	}

	public void setEnviadoPor(String enviadoPor) {
		this.enviadoPor = enviadoPor;
	}

	public Instant getEnviadoEm() {
		return enviadoEm;
	}

	public void setEnviadoEm(Instant enviadoEm) {
		this.enviadoEm = enviadoEm;
	}

	public int getTotalLinhas() {
		return totalLinhas;
	}

	public void setTotalLinhas(int totalLinhas) {
		this.totalLinhas = totalLinhas;
	}

	public int getLinhasValidas() {
		return linhasValidas;
	}

	public void setLinhasValidas(int linhasValidas) {
		this.linhasValidas = linhasValidas;
	}

	public int getLinhasInvalidas() {
		return linhasInvalidas;
	}

	public void setLinhasInvalidas(int linhasInvalidas) {
		this.linhasInvalidas = linhasInvalidas;
	}

	public StatusLote getStatus() {
		return status;
	}

	public void setStatus(StatusLote status) {
		this.status = status;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}

}
