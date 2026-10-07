package br.com.cti.insights.repository;

import br.com.cti.insights.model.Telemetria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetriaRepository extends JpaRepository<Telemetria, Long> {

}
