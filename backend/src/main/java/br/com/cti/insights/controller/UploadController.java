package br.com.cti.insights.controller;

import br.com.cti.insights.dto.ClienteDTO;
import br.com.cti.insights.dto.UploadLoteDTO;
import br.com.cti.insights.service.UploadService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

	private final UploadService service;

	public UploadController(UploadService service) {
		this.service = service;
	}

	// POST /api/uploads/validadas?arquivo=dados.xlsx&recebidas=38&comAviso=2 com as linhas do Pinia no
	// corpo, no formato do ClienteDTO. O Java revalida e chama o Python, que cria o lote e grava
	// tudo. Responde com o lote ja processado (ou erro, se o Python nao respondeu).
	// Sem @Valid aqui: o UploadService revalida na ordem da planilha (arquivo, colunas, linhas).
	@PostMapping("/validadas")
	public ResponseEntity<UploadLoteDTO> enviarValidadas(@RequestParam(required = false) String arquivo,
			@RequestParam(required = false) Integer recebidas, @RequestParam(required = false) Integer comAviso,
			@RequestBody List<ClienteDTO> linhas) {
		UploadLoteDTO lote = service.registrar(arquivo, recebidas, comAviso, linhas);
		return ResponseEntity.created(URI.create("/api/dashboard?loteId=" + lote.id())).body(lote);
	}

	@GetMapping
	public List<UploadLoteDTO> listar() {
		return service.listar();
	}

}
