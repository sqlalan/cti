package com.synthetix.cti.controller;

import com.synthetix.cti.dto.AnaliseDTO;
import com.synthetix.cti.model.Analise;
import com.synthetix.cti.service.PlanilhaService;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/planilhas")
public class PlanilhaController {

	private final PlanilhaService service;

	public PlanilhaController(PlanilhaService service) {
		this.service = service;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AnaliseDTO> enviar(@RequestParam("arquivo") MultipartFile arquivo) {
		Analise analise = service.processar(arquivo);
		return ResponseEntity.created(URI.create("/api/analises/" + analise.getId())).body(AnaliseDTO.de(analise));
	}

}
