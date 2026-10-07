package br.com.cti.insights.repository;

import br.com.cti.insights.model.Servico;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

	Optional<Servico> findByNomeIgnoreCase(String nome);

}
