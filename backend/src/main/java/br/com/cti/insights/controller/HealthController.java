package br.com.cti.insights.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Confere se a API esta no ar (teste do deploy). Publica, sem token: nao expoe dado nenhum.
 */
@RestController
public class HealthController {

	// GET /api/health -> {"status":"ok"}
	@GetMapping("/api/health")
	public Map<String, String> health() {
		return Map.of("status", "ok");
	}

}
