package br.com.cti.insights.repository;

import br.com.cti.insights.model.Analise;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnaliseRepository extends JpaRepository<Analise, Long> {

	Optional<Analise> findFirstByOrderByGeradoEmDescIdDesc();

	List<Analise> findAllByOrderByGeradoEmDescIdDesc();

}
