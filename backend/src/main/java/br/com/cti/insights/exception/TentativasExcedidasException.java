package br.com.cti.insights.exception;

/**
 * Senhas erradas demais seguidas para o mesmo e-mail: o login fica bloqueado por alguns minutos.
 */
public class TentativasExcedidasException extends RuntimeException {

	public TentativasExcedidasException(long minutos) {
		super("Muitas tentativas de login. Tente de novo em " + minutos + (minutos == 1 ? " minuto" : " minutos"));
	}

}
