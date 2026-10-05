package com.synthetix.cti.analise;

import java.util.List;

/**
 * Tudo o que o Python devolveu para uma planilha. Indicadores e problemas seguem como JSON cru:
 * o Java so os guarda e repassa ao dashboard.
 */
public record ResultadoAnalise(int linhasLidas, int linhasValidas, int linhasRejeitadas, int avisos,
		List<ClienteTratado> clientes, List<InsightTratado> insights, String indicadores, String problemas) {

}
