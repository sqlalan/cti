package br.com.cti.insights.repository;

import br.com.cti.insights.model.Segmento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SegmentoRepository extends JpaRepository<Segmento, Long> {

	Optional<Segmento> findByNomeIgnoreCase(String nome);

}
