package br.com.cti.insights.controller;

import br.com.cti.insights.dto.DashboardDTO;
import br.com.cti.insights.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService service;

	public DashboardController(DashboardService service) {
		this.service = service;
	}

	// GET /api/dashboard?loteId=1 -> { lote, geradoEm, estatisticas }
	@GetMapping
	public DashboardDTO obter(@RequestParam(required = false) Long loteId) {
		return service.obter(loteId);
	}

}
