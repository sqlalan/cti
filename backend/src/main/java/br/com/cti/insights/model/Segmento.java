package br.com.cti.insights.model;

import jakarta.persistence.*;

/**
 * Segmento do cliente (Indústria, Comércio...), sem repetir. Quem grava na importação é o Python
 * (banco.py); o cadastro manual de cliente usa o CatalogoService.
 */
@Entity
@Table(name = "segmento")
public class Segmento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 60)
	private String nome;

	public Segmento() {
	}

	public Segmento(String nome) {
		setNome(nome);
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome.trim();
	}

}
