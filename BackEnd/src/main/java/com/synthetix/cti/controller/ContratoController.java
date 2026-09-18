package com.synthetix.cti.controller;

import com.synthetix.cti.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

	private final ClienteService service;

	public ContratoController(ClienteService service) {
		this.service = service;
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		service.removerContrato(id);
		return ResponseEntity.noContent().build();
	}

}
