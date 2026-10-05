package com.synthetix.cti.repository;

import com.synthetix.cti.model.Analise;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnaliseRepository extends JpaRepository<Analise, Long> {

	Optional<Analise> findFirstByOrderByGeradoEmDescIdDesc();

	List<Analise> findAllByOrderByGeradoEmDescIdDesc();

}
