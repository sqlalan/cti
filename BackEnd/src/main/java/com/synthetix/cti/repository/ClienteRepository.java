package com.synthetix.cti.repository;

import com.synthetix.cti.model.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	Optional<Cliente> findByCodigo(String codigo);

	boolean existsByCodigo(String codigo);

}
