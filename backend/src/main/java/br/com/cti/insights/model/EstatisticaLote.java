package br.com.cti.insights.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * O JSON do dashboard (media, mediana, agrupamentos e insights) que o Python grava para cada lote. O
 * Spring so le.
 */
@Entity
@Table(name = "estatistica_lote")
public class EstatisticaLote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lote_id", nullable = false, unique = true)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private UploadLote lote;

	// Hora UTC sem fuso, como o banco.py grava.
	@Column(name = "gerado_em", nullable = false)
	private LocalDateTime geradoEm;

	// Texto com JSON: no SQL, resultado::json le os campos.
	@Column(nullable = false, columnDefinition = "text")
	private String resultado;

	public EstatisticaLote() {
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

	public LocalDateTime getGeradoEm() {
		return geradoEm;
	}

	public void setGeradoEm(LocalDateTime geradoEm) {
		this.geradoEm = geradoEm;
	}

	public String getResultado() {
		return resultado;
	}

	public void setResultado(String resultado) {
		this.resultado = resultado;
	}

}
