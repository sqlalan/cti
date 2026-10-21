package br.com.cti.insights.dto;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Resposta do GET /api/dashboard. estatisticas e o JSON que o Python gravou em estatistica_lote, sem
 * mudar nada; fica nulo enquanto o ETL nao terminou.
 */
public record DashboardDTO(UploadLoteDTO lote, Instant geradoEm, JsonNode estatisticas) {

}
