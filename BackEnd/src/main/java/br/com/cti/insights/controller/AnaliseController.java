package br.com.cti.insights.controller;

import br.com.cti.insights.dto.AnaliseDTO;
import br.com.cti.insights.dto.AnaliseResumoDTO;
import br.com.cti.insights.dto.InsightDTO;
import br.com.cti.insights.service.AnaliseService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resultados das planilhas processadas. Indicadores e problemas saem como o Python os gravou.
 */
@RestController
@RequestMapping("/api")
public class AnaliseController {

	private final AnaliseService service;

	public AnaliseController(AnaliseService service) {
		this.service = service;
	}

	@GetMapping("/analises")
	public List<AnaliseResumoDTO> listar() {
		return service.listar();
	}

	@GetMapping("/analises/ultima")
	public AnaliseDTO ultima() {
		return service.ultima();
	}

	@GetMapping("/analises/{id}")
	public AnaliseDTO buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@GetMapping("/analises/{id}/problemas")
	public ResponseEntity<String> problemas(@PathVariable Long id) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.problemas(id));
	}

	@GetMapping("/insights")
	public List<InsightDTO> insights() {
		return service.insightsDaUltima();
	}

	@GetMapping("/indicadores")
	public ResponseEntity<String> indicadores() {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.indicadoresDaUltima());
	}

}
