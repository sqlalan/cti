package br.com.cti.insights.repository;

import br.com.cti.insights.model.Consultor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultorRepository extends JpaRepository<Consultor, Long> {

	Optional<Consultor> findByNomeIgnoreCase(String nome);

	Optional<Consultor> findByMatricula(String matricula);

}
