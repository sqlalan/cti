package com.synthetix.cti.repository;

import com.synthetix.cti.model.Servico;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

	Optional<Servico> findByNomeIgnoreCase(String nome);

}
