package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Um envio de planilha. Quem cria e atualiza o lote é o Python (banco.py); o Java só lê,
 * para a tela de envio e o dashboard.
 */
@Entity
@Table(name = "upload_lote")
public class UploadLote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "arquivo_nome", nullable = false)
	private String arquivoNome;

	@Column(name = "enviado_em", nullable = false, updatable = false)
	private Instant enviadoEm;

	// Linhas da planilha, boas e ruins.
	@Column(name = "total_linhas", nullable = false)
	private int totalLinhas;

	@Column(name = "linhas_validas", nullable = false)
	private int linhasValidas;

	@Column(name = "linhas_invalidas", nullable = false)
	private int linhasInvalidas;

	// Linhas boas que tinham algum aviso (ex.: segmento fora do dicionário).
	@Column(name = "linhas_com_aviso", nullable = false)
	private int linhasComAviso;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusLote status = StatusLote.PROCESSANDO;

	@Column(length = 500)
	private String mensagem;

	public UploadLote() {
	}

	public UploadLote(String arquivoNome, int totalLinhas, int linhasValidas) {
		this.arquivoNome = arquivoNome;
		this.totalLinhas = totalLinhas;
		this.linhasValidas = linhasValidas;
		this.linhasInvalidas = totalLinhas - linhasValidas;
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

	public int getLinhasComAviso() {
		return linhasComAviso;
	}

	public void setLinhasComAviso(int linhasComAviso) {
		this.linhasComAviso = linhasComAviso;
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
