package com.synthetix.cti.repository;

import com.synthetix.cti.model.Consultor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultorRepository extends JpaRepository<Consultor, Long> {

	Optional<Consultor> findByNomeIgnoreCase(String nome);

}
