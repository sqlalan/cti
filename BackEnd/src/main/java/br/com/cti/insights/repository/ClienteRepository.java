package br.com.cti.insights.repository;

import br.com.cti.insights.model.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	// So o cadastro manual tem codigo unico: o mesmo codigo volta a cada lote enviado.
	Optional<Cliente> findByCodigoAndLoteIsNull(String codigo);

	boolean existsByCodigoAndLoteIsNull(String codigo);

}
